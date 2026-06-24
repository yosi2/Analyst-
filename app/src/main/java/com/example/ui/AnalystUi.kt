package com.example.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CsvColumnInsight
import com.example.data.CsvData
import com.example.data.local.ChatMessageEntity
import com.example.data.local.DatasetEntity
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@Composable
fun AnalystApp(viewModel: AnalystViewModel) {
    val context = LocalContext.current
    val datasets by viewModel.datasets.collectAsState()
    val selectedId by viewModel.selectedDatasetId.collectAsState()
    val activeCsvData by viewModel.activeCsvData.collectAsState()
    val columnInsights by viewModel.columnInsights.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0 = Chat, 1 = Table, 2 = Insights, 3 = Datasets

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val contentResolver = context.contentResolver
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                val fileName = getFileName(context, uri) ?: "custom_dataset.csv"
                val sizeBytes = content.toByteArray().size.toLong()

                if (content.isNotEmpty()) {
                    viewModel.loadCustomCsv(fileName, content, sizeBytes)
                    Toast.makeText(context, "Successfully loaded $fileName", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "File is empty!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading CSV: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("scaffold_layout"),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            // Clean Minimalism Custom Bottom Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(0.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val navItems = listOf(
                    Triple(0, "Chat", Icons.Default.Send),
                    Triple(1, "Explorer", Icons.Default.List),
                    Triple(2, "Insights", Icons.Default.Star),
                    Triple(3, "Datasets", Icons.Default.Info)
                )

                navItems.forEach { (tabIndex, label, icon) ->
                    val isSelected = activeTab == tabIndex
                    val tintColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { activeTab = tabIndex }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = tintColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 10.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(0.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Analytics logo",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = "Eyo AI",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 15.sp,
                                lineHeight = 16.sp
                            )
                        )
                        Text(
                            text = "PROFESSIONAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 9.sp,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { 
                            Toast.makeText(context, "Eyo AI Notifications are active", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (activeCsvData != null) {
                // Current Dataset Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clickable { filePickerLauncher.launch("text/comma-separated-values") }
                        .testTag("current_file_banner"),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = "CSV document",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "CURRENT FILE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        letterSpacing = 1.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = activeCsvData!!.fileName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 200.dp)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Replace or select dataset",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (activeCsvData != null) {
                    when (activeTab) {
                        0 -> ChatView(
                            messages = messages,
                            isGenerating = isGenerating,
                            errorMessage = errorMessage,
                            activeCsvData = activeCsvData!!,
                            onSendMessage = { viewModel.askQuestion(it) }
                        )
                        1 -> DataTableView(activeCsvData!!)
                        2 -> InsightsView(columnInsights)
                        3 -> DatasetsManageView(
                            datasets = datasets,
                            selectedId = selectedId,
                            onSelectDataset = { viewModel.selectDataset(it) },
                            onDeleteDataset = { viewModel.deleteDataset(it) },
                            onUploadClick = { filePickerLauncher.launch("text/comma-separated-values") }
                        )
                    }
                } else {
                    EmptyLandingView(
                        datasets = datasets,
                        selectedId = selectedId,
                        onSelectDataset = { viewModel.selectDataset(it) },
                        onUploadClick = { filePickerLauncher.launch("text/comma-separated-values") }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyLandingView(
    datasets: List<DatasetEntity>,
    selectedId: Long?,
    onSelectDataset: (Long) -> Unit,
    onUploadClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Eyo AI Data Scientist",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Instantly load any CSV spreadsheet file to parse columns, perform automatic statistics, and query with AI natural language.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onUploadClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("landing_upload_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Upload CSV Spreadsheet")
        }

        if (datasets.isNotEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "OR CHOOSE A PRELOADED DATASET:",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(datasets) { dataset ->
                    val isSelected = dataset.id == selectedId
                    Card(
                        modifier = Modifier
                            .clickable { onSelectDataset(dataset.id) }
                            .testTag("landing_dataset_${dataset.id}"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column {
                                Text(
                                    text = dataset.name.take(20) + if (dataset.name.length > 20) "..." else "",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = formatSize(dataset.sizeBytes),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DatasetsManageView(
    datasets: List<DatasetEntity>,
    selectedId: Long?,
    onSelectDataset: (Long) -> Unit,
    onDeleteDataset: (Long) -> Unit,
    onUploadClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DATASETS & REPOSITORY",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            )
            
            OutlinedButton(
                onClick = onUploadClick,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Upload", fontSize = 12.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(datasets) { dataset ->
                val isSelected = dataset.id == selectedId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectDataset(dataset.id) }
                        .testTag("manage_dataset_card_${dataset.id}"),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column {
                                Text(
                                    text = dataset.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Size: ${formatSize(dataset.sizeBytes)}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (isSelected) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Active",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            
                            if (datasets.size > 1) {
                                IconButton(
                                    onClick = { onDeleteDataset(dataset.id) },
                                    modifier = Modifier.testTag("delete_dataset_btn_${dataset.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. CHAT TAB VIEW
// ==========================================
@Composable
fun ChatView(
    messages: List<ChatMessageEntity>,
    isGenerating: Boolean,
    errorMessage: String?,
    activeCsvData: CsvData,
    onSendMessage: (String) -> Unit
) {
    val scrollState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    // Quick prompts suggestions based on current dataset content
    val suggestions = remember(activeCsvData.fileName) {
        listOf(
            "📝 Summarize this dataset",
            "📈 Find trends and key metrics",
            "🔍 Find anomalies and outliers",
            "📊 List out interesting insights"
        )
    }

    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        // Chat list area
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // First Analyst Welcome Message
            item {
                AnalystBubble(
                    message = "Hello! I am your **Eyo AI Data Analyst**. I have fully parsed and analyzed **${activeCsvData.fileName}** (${activeCsvData.rowCount} rows, ${activeCsvData.columnCount} columns).\n\n" +
                            "What would you like to know? You can ask me to run aggregations, find correlations, summarize metrics, or discover patterns!",
                    timestamp = System.currentTimeMillis()
                )
            }

            items(messages) { message ->
                if (message.sender == "user") {
                    UserBubble(message = message.content, timestamp = message.timestamp)
                } else {
                    AnalystBubble(message = message.content, timestamp = message.timestamp)
                }
            }

            if (isGenerating) {
                item {
                    GeneratingBubble()
                }
            }
        }

        // Suggestions bar
        if (messages.isEmpty() && !isGenerating) {
            Text(
                text = "SUGGESTED ANALYTICS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestions) { suggestion ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .clickable { onSendMessage(suggestion.substring(2)) }
                            .padding(4.dp)
                            .testTag("suggestion_chip_${suggestion.substring(2).replace(" ", "_")}")
                    ) {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Input row
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(18.dp)
                )

                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { 
                        Text(
                            text = "Ask a question about your data...",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        ) 
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("question_input"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank() && !isGenerating) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                FloatingActionButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isGenerating) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("send_question_button"),
                    containerColor = if (inputText.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Send Message",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun UserBubble(message: String, timestamp: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Column(
            modifier = Modifier.widthIn(max = 290.dp),
            horizontalAlignment = Alignment.End
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onPrimary,
                        lineHeight = 20.sp
                    )
                )
            }
        }
    }
}

@Composable
fun AnalystBubble(message: String, timestamp: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = "AI ANALYSIS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                )
            }

            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                MarkdownText(text = message)
            }
        }
    }
}

@Composable
fun GeneratingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = "AI THINKING",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                )
            }

            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
                    )
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// A beautiful lightweight custom Markdown parser that renders lists, tables, code blocks, and bold text properly!
@Composable
fun MarkdownText(text: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val lines = text.split("\n")
        var inTable = false
        var tableHeaders = emptyList<String>()
        val tableRows = remember { mutableStateListOf<List<String>>() }

        lines.forEach { line ->
            val trimmedLine = line.trim()
            
            // Check for Markdown table formatting: | Col 1 | Col 2 |
            if (trimmedLine.startsWith("|") && trimmedLine.endsWith("|")) {
                inTable = true
                val parts = trimmedLine.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                
                // Exclude table dividers like |---|---|
                if (parts.all { it.all { c -> c == '-' } }) {
                    // It's a divider row, skip it
                } else if (tableHeaders.isEmpty()) {
                    tableHeaders = parts
                } else {
                    tableRows.add(parts)
                }
                return@forEach
            } else {
                // If we were in a table and it ended, render the accumulated table
                if (inTable && tableHeaders.isNotEmpty()) {
                    RenderMarkdownTable(headers = tableHeaders, rows = tableRows.toList())
                    inTable = false
                    tableHeaders = emptyList()
                    tableRows.clear()
                }
            }

            when {
                trimmedLine.startsWith("###") -> {
                    Text(
                        text = removeBoldAsterisks(trimmedLine.removePrefix("###").trim()),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                trimmedLine.startsWith("##") -> {
                    Text(
                        text = removeBoldAsterisks(trimmedLine.removePrefix("##").trim()),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                trimmedLine.startsWith("#") -> {
                    Text(
                        text = removeBoldAsterisks(trimmedLine.removePrefix("#").trim()),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                trimmedLine.startsWith("-") || trimmedLine.startsWith("*") -> {
                    val bulletText = trimmedLine.removePrefix("-").removePrefix("*").trim()
                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("•", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = parseBoldText(bulletText),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                trimmedLine.startsWith("```") -> {
                    // Simply ignore code block identifiers or write them in monospace
                    if (trimmedLine.length > 3) {
                        Text(
                            text = "[Code block: " + trimmedLine.substring(3) + "]",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    }
                }
                else -> {
                    if (trimmedLine.isNotEmpty()) {
                        Text(
                            text = parseBoldText(trimmedLine),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Handle case where table reaches the end of response text
        if (inTable && tableHeaders.isNotEmpty()) {
            RenderMarkdownTable(headers = tableHeaders, rows = tableRows.toList())
        }
    }
}

// Basic Bold text bolding using annotated string
@Composable
fun parseBoldText(text: String): androidx.compose.ui.text.AnnotatedString {
    val parts = text.split("**")
    return androidx.compose.ui.text.buildAnnotatedString {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
                append(part)
                pop()
            } else {
                append(part)
            }
        }
    }
}

@Composable
fun RenderMarkdownTable(headers: List<String>, rows: List<List<String>>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(vertical = 6.dp, horizontal = 4.dp)
            ) {
                headers.forEach { header ->
                    Text(
                        text = header,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            // Rows
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                ) {
                    row.forEach { cell ->
                        Text(
                            text = cell,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

// ==========================================
// 2. DATA TABLE TAB VIEW
// ==========================================
@Composable
fun DataTableView(csvData: CsvData) {
    var searchQuery by remember { mutableStateOf("") }
    var pageIndex by remember { mutableStateOf(0) }
    val pageSize = 15

    // Sorting State: Pair(ColumnIndex, Ascending)
    var sortState by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }

    // Filtered data based on search
    val filteredRows = remember(csvData, searchQuery) {
        if (searchQuery.isBlank()) {
            csvData.rows
        } else {
            csvData.rows.filter { row ->
                row.any { cell -> cell.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    // Sorted data
    val sortedRows = remember(filteredRows, sortState) {
        val currentSort = sortState
        if (currentSort == null) {
            filteredRows
        } else {
            val (colIndex, ascending) = currentSort
            filteredRows.sortedWith { r1, r2 ->
                val val1 = r1.getOrNull(colIndex) ?: ""
                val val2 = r2.getOrNull(colIndex) ?: ""
                
                // Try numeric sort
                val d1 = val1.replace("$", "").replace(",", "").toDoubleOrNull()
                val d2 = val2.replace("$", "").replace(",", "").toDoubleOrNull()
                
                val result = if (d1 != null && d2 != null) {
                    d1.compareTo(d2)
                } else {
                    val1.compareTo(val2, ignoreCase = true)
                }
                if (ascending) result else -result
            }
        }
    }

    // Page boundaries
    val totalPages = kotlin.math.max(1, (sortedRows.size + pageSize - 1) / pageSize)
    val pageRows = remember(sortedRows, pageIndex, totalPages) {
        val safePage = kotlin.math.min(pageIndex, totalPages - 1)
        val start = safePage * pageSize
        val end = kotlin.math.min(start + pageSize, sortedRows.size)
        if (start < sortedRows.size) sortedRows.subList(start, end) else emptyList()
    }

    // Reset page index on filter or sort
    LaunchedEffect(searchQuery, sortState) {
        pageIndex = 0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Input
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { 
                Text(
                    text = "Search data records...",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                ) 
            },
            leadingIcon = { 
                Icon(
                    imageVector = Icons.Default.Search, 
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                ) 
            },
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                .testTag("table_search_input"),
            shape = RoundedCornerShape(24.dp),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Table headers and grid
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Horizontal scroll wrapper for columns
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        // Headers
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                    .padding(vertical = 12.dp, horizontal = 12.dp)
                            ) {
                                csvData.headers.forEachIndexed { index, header ->
                                    val isSorted = sortState?.first == index
                                    val isAscending = sortState?.second == true
                                    
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                sortState = if (isSorted) {
                                                    if (isAscending) Pair(index, false) else null
                                                } else {
                                                    Pair(index, true)
                                                }
                                            }
                                            .padding(2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = header,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (isSorted) {
                                            Text(
                                                text = if (isAscending) "▲" else "▼",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Grid rows
                        if (pageRows.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No matching records found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            items(pageRows) { row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        .padding(vertical = 12.dp, horizontal = 12.dp)
                                ) {
                                    row.forEach { cell ->
                                        Text(
                                            text = cell,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pagination buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Showing ${if (sortedRows.isEmpty()) 0 else (pageIndex * pageSize) + 1} to ${kotlin.math.min((pageIndex + 1) * pageSize, sortedRows.size)} of ${sortedRows.size} rows",
                style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { if (pageIndex > 0) pageIndex-- },
                    enabled = pageIndex > 0,
                    modifier = Modifier.testTag("prev_page_button")
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Previous page")
                }
                IconButton(
                    onClick = { if (pageIndex < totalPages - 1) pageIndex++ },
                    enabled = pageIndex < totalPages - 1,
                    modifier = Modifier.testTag("next_page_button")
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Next page")
                }
            }
        }
    }
}

// ==========================================
// 3. INSIGHTS TAB VIEW (AUTO CALCULATIONS)
// ==========================================
@Composable
fun InsightsView(insights: List<CsvColumnInsight>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "AUTOMATIC METRICS & STATS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        items(insights) { col ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("insight_card_${col.name}"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = col.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Badge type chip
                        val typeColor = when (col.type) {
                            "Numeric" -> MaterialTheme.colorScheme.primaryContainer
                            "Date" -> MaterialTheme.colorScheme.tertiaryContainer
                            "Categorical" -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        
                        val typeText = col.type
                        Card(
                            colors = CardDefaults.cardColors(containerColor = typeColor),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = typeText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Detail stats
                    if (col.type == "Numeric") {
                        val format = DecimalFormat("#.##")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NumericStatBox(title = "Average", value = format.format(col.average ?: 0.0), type = "avg", modifier = Modifier.weight(1f))
                            NumericStatBox(title = "Sum", value = format.format(col.sum ?: 0.0), type = "sum", modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NumericStatBox(title = "Minimum", value = format.format(col.min ?: 0.0), type = "min", modifier = Modifier.weight(1f))
                            NumericStatBox(title = "Maximum", value = format.format(col.max ?: 0.0), type = "max", modifier = Modifier.weight(1f))
                        }
                    } else if (col.type == "Categorical" && col.valueDistribution != null) {
                        Text(
                            text = "Top Frequencies:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        
                        val totalSum = col.valueDistribution.values.sum().toFloat()
                        col.valueDistribution.forEach { (catValue, count) ->
                            val pct = if (totalSum > 0) count / totalSum else 0f
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = catValue, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(text = "$count (${DecimalFormat("#.#").format(pct * 100)}%)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                )
                            }
                        }
                    } else {
                        // Text or general column values
                        Text(
                            text = "Sample Values:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Text(
                            text = col.sampleValues.joinToString(separator = ", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Null count indicator
                    if (col.nullCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Found ${col.nullCount} empty (null) values in this column.",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.error)
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                            Text(
                                text = "Complete dataset. 100% data filled.",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF4CAF50))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NumericStatBox(title: String, value: String, type: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val emoji = when (type) {
                "avg" -> "📊"
                "sum" -> "➕"
                "min" -> "👇"
                else -> "👆"
            }
            Text(text = emoji, fontSize = 16.sp)
            Column {
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = value, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val df = DecimalFormat("#.##")
    val kb = bytes / 1024.0
    if (kb < 1024) return "${df.format(kb)} KB"
    val mb = kb / 1024.0
    return "${df.format(mb)} MB"
}

fun getFileName(context: android.content.Context, uri: android.net.Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

fun removeBoldAsterisks(text: String): String {
    return text.replace("**", "")
}
