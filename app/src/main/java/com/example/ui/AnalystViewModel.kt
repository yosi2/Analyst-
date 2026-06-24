package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AnalystRepository
import com.example.data.CsvAnalyzer
import com.example.data.CsvColumnInsight
import com.example.data.CsvData
import com.example.data.CsvParser
import com.example.data.SampleDatasets
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.DatasetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnalystViewModel(
    application: Application,
    private val repository: AnalystRepository
) : AndroidViewModel(application) {

    // All available datasets
    val datasets: StateFlow<List<DatasetEntity>> = repository.allDatasets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Active selected dataset ID
    private val _selectedDatasetId = MutableStateFlow<Long?>(null)
    val selectedDatasetId: StateFlow<Long?> = _selectedDatasetId.asStateFlow()

    // Loaded active CSV Data
    private val _activeCsvData = MutableStateFlow<CsvData?>(null)
    val activeCsvData: StateFlow<CsvData?> = _activeCsvData.asStateFlow()

    // Auto-calculated column insights
    private val _columnInsights = MutableStateFlow<List<CsvColumnInsight>>(emptyList())
    val columnInsights: StateFlow<List<CsvColumnInsight>> = _columnInsights.asStateFlow()

    // Chat history for active dataset
    private val _messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val messages: StateFlow<List<ChatMessageEntity>> = _messages.asStateFlow()

    // Generation states
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Automatically preload samples on first launch if empty
        viewModelScope.launch {
            datasets.collectLatest { datasetList ->
                if (datasetList.isEmpty()) {
                    preloadSampleDatasets()
                } else if (_selectedDatasetId.value == null) {
                    // Select first dataset by default
                    selectDataset(datasetList.first().id)
                }
            }
        }

        // Listen for dataset selection changes to update messages
        viewModelScope.launch {
            _selectedDatasetId.collectLatest { datasetId ->
                if (datasetId != null) {
                    repository.getMessagesForDataset(datasetId).collectLatest { messageList ->
                        _messages.value = messageList
                    }
                } else {
                    _messages.value = emptyList()
                }
            }
        }
    }

    private suspend fun preloadSampleDatasets() {
        withContext(Dispatchers.IO) {
            SampleDatasets.samples.forEach { sample ->
                repository.insertDataset(
                    DatasetEntity(
                        name = sample.name,
                        content = sample.csvContent,
                        sizeBytes = sample.csvContent.toByteArray().size.toLong()
                    )
                )
            }
        }
    }

    fun selectDataset(id: Long) {
        viewModelScope.launch {
            _selectedDatasetId.value = id
            val entity = withContext(Dispatchers.IO) {
                repository.getDatasetById(id)
            }
            if (entity != null) {
                val csvData = withContext(Dispatchers.Default) {
                    CsvParser.parse(entity.content, entity.name, entity.sizeBytes)
                }
                _activeCsvData.value = csvData
                
                // Perform statistical calculations
                val insights = withContext(Dispatchers.Default) {
                    CsvAnalyzer.analyze(csvData)
                }
                _columnInsights.value = insights
            } else {
                _activeCsvData.value = null
                _columnInsights.value = emptyList()
            }
        }
    }

    fun loadCustomCsv(fileName: String, content: String, sizeBytes: Long) {
        viewModelScope.launch {
            val newId = withContext(Dispatchers.IO) {
                repository.insertDataset(
                    DatasetEntity(
                        name = fileName,
                        content = content,
                        sizeBytes = sizeBytes
                    )
                )
            }
            selectDataset(newId)
        }
    }

    fun deleteDataset(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteDataset(id)
            }
            if (_selectedDatasetId.value == id) {
                _selectedDatasetId.value = null
                _activeCsvData.value = null
                _columnInsights.value = emptyList()
                _messages.value = emptyList()
                
                // Select another dataset if available
                val remaining = datasets.value
                if (remaining.isNotEmpty()) {
                    val nextToSelect = remaining.firstOrNull { it.id != id }
                    if (nextToSelect != null) {
                        selectDataset(nextToSelect.id)
                    }
                }
            }
        }
    }

    fun clearChat() {
        val datasetId = _selectedDatasetId.value ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // Delete messages by deleting dataset session and re-inserting dataset
                val currentDataset = repository.getDatasetById(datasetId)
                if (currentDataset != null) {
                    repository.deleteDataset(datasetId)
                    val newId = repository.insertDataset(
                        DatasetEntity(
                            name = currentDataset.name,
                            content = currentDataset.content,
                            sizeBytes = currentDataset.sizeBytes,
                            timestamp = currentDataset.timestamp
                        )
                    )
                    selectDataset(newId)
                }
            }
        }
    }

    fun askQuestion(questionText: String) {
        val datasetId = _selectedDatasetId.value ?: return
        val csvData = _activeCsvData.value ?: return
        val insights = _columnInsights.value
        
        if (questionText.isBlank()) return

        viewModelScope.launch {
            _errorMessage.value = null
            
            // 1. Save user message locally
            val userMsg = ChatMessageEntity(
                datasetId = datasetId,
                sender = "user",
                content = questionText
            )
            withContext(Dispatchers.IO) {
                repository.insertMessage(userMsg)
            }

            // 2. Set generating state
            _isGenerating.value = true

            try {
                // Check API key
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    throw IllegalStateException("API key is not configured. Please use the Secrets panel in the Google AI Studio UI to configure your GEMINI_API_KEY.")
                }

                // 3. Build comprehensive CSV summary context
                val columnStats = insights.joinToString(separator = "\n") { insight ->
                    val base = "- **${insight.name}** (${insight.type}): nulls=${insight.nullCount}"
                    val detail = when (insight.type) {
                        "Numeric" -> ", min=${insight.min}, max=${insight.max}, avg=${insight.average}, sum=${insight.sum}"
                        "Categorical" -> ", categories=${insight.valueDistribution?.keys?.joinToString()}"
                        else -> ", samples=${insight.sampleValues.joinToString()}"
                    }
                    base + detail
                }

                // Limit rows if CSV is exceptionally large, but send complete data for typical mobile files
                val csvContentString = if (csvData.rowCount > 500) {
                    val previewRows = csvData.rows.take(150)
                    val headerStr = csvData.headers.joinToString(",")
                    val dataStr = previewRows.joinToString("\n") { it.joinToString(",") }
                    "$headerStr\n$dataStr\n... [Truncated for prompt limits. Showing first 150 of ${csvData.rowCount} rows]"
                } else {
                    val headerStr = csvData.headers.joinToString(",")
                    val dataStr = csvData.rows.joinToString("\n") { it.joinToString(",") }
                    "$headerStr\n$dataStr"
                }

                val systemPrompt = """
                    You are "Eyo AI Data Analyst", a highly sophisticated, professional, and friendly AI data assistant.
                    Your purpose is to help the user analyze, summarize, and understand their CSV datasets.
                    
                    Here is the metadata of the currently uploaded dataset:
                    - File Name: ${csvData.fileName}
                    - Rows Count: ${csvData.rowCount}
                    - Columns Count: ${csvData.columnCount}
                    
                    Column Metadata & Statistical Insights:
                    $columnStats
                    
                    Here is the dataset itself in standard CSV format:
                    ```csv
                    $csvContentString
                    ```
                    
                    Guidelines:
                    - Answer the user's questions clearly, accurately, and professionally.
                    - Always calculate values correctly based on the provided data.
                    - Use beautiful Markdown formatting: lists, bold text, and especially clean Markdown tables when presenting or comparing tabular records.
                    - Point out interesting anomalies, correlations, or key trends that add massive analytical value.
                    - Keep the tone helpful, direct, and elite. If you cannot answer based on the data, state what is missing instead of hallucinating.
                """.trimIndent()

                // 4. Construct previous turns context
                val previousTurns = _messages.value.map { msg ->
                    Content(
                        parts = listOf(Part(text = msg.content))
                    )
                }

                // Append the current prompt as the last content block
                val finalPromptContents = previousTurns + Content(
                    parts = listOf(Part(text = questionText))
                )

                // 5. Call Gemini API
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent(
                        apiKey = apiKey,
                        request = GenerateContentRequest(
                            contents = finalPromptContents,
                            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
                        )
                    )
                }

                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Eyo AI Analyst was unable to generate a response. Please try rephrasing your question."

                // 6. Save model response
                val assistantMsg = ChatMessageEntity(
                    datasetId = datasetId,
                    sender = "analyst",
                    content = responseText
                )
                withContext(Dispatchers.IO) {
                    repository.insertMessage(assistantMsg)
                }

            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "An unexpected network error occurred."
                
                // Add a failure message to the chat so the user can see what went wrong
                val errorChatMsg = ChatMessageEntity(
                    datasetId = datasetId,
                    sender = "analyst",
                    content = "🚨 **Error calling Eyo AI Analyst:**\n${e.localizedMessage ?: "Network or API failure."}\n\n*Double-check that you have added your `GEMINI_API_KEY` in the Secrets panel in the AI Studio sidebar and have internet access.*"
                )
                withContext(Dispatchers.IO) {
                    repository.insertMessage(errorChatMsg)
                }
            } finally {
                _isGenerating.value = false
            }
        }
    }
}

class AnalystViewModelFactory(
    private val application: Application,
    private val repository: AnalystRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalystViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnalystViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
