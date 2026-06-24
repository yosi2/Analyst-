package com.example.data

object CsvParser {
    fun parse(content: String, fileName: String, sizeBytes: Long): CsvData {
        val rows = mutableListOf<List<String>>()
        val currentField = StringBuilder()
        var inQuotes = false
        var currentRow = mutableListOf<String>()
        
        var i = 0
        val len = content.length
        while (i < len) {
            val char = content[i]
            when {
                char == '"' -> {
                    if (inQuotes && i + 1 < len && content[i + 1] == '"') {
                        // Escaped double quote
                        currentField.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                char == ',' && !inQuotes -> {
                    currentRow.add(currentField.toString().trim())
                    currentField.clear()
                }
                (char == '\r' || char == '\n') && !inQuotes -> {
                    if (char == '\r' && i + 1 < len && content[i + 1] == '\n') {
                        i++
                    }
                    currentRow.add(currentField.toString().trim())
                    currentField.clear()
                    if (currentRow.isNotEmpty() && (currentRow.size > 1 || currentRow[0].isNotEmpty())) {
                        rows.add(currentRow)
                    }
                    currentRow = mutableListOf()
                }
                else -> {
                    currentField.append(char)
                }
            }
            i++
        }
        
        // Add last field and row if remaining
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString().trim())
            if (currentRow.isNotEmpty() && (currentRow.size > 1 || currentRow[0].isNotEmpty())) {
                rows.add(currentRow)
            }
        }
        
        if (rows.isEmpty()) {
            return CsvData(emptyList(), emptyList(), fileName, sizeBytes)
        }
        
        val headers = rows[0].map { it.ifEmpty { "Column_${rows[0].indexOf(it)}" } }
        val rawDataRows = rows.drop(1)
        
        // Normalize rows to have the same length as headers
        val normalizedRows = rawDataRows.map { row ->
            if (row.size < headers.size) {
                row + List(headers.size - row.size) { "" }
            } else if (row.size > headers.size) {
                row.take(headers.size)
            } else {
                row
            }
        }
        
        return CsvData(headers, normalizedRows, fileName, sizeBytes)
    }
}
