package com.example.data

object CsvAnalyzer {
    fun analyze(csvData: CsvData): List<CsvColumnInsight> {
        val headers = csvData.headers
        val rows = csvData.rows
        
        if (headers.isEmpty() || rows.isEmpty()) return emptyList()
        
        return headers.mapIndexed { colIndex, name ->
            val colValues = rows.map { row -> row.getOrNull(colIndex) ?: "" }
            val nonNullValues = colValues.filter { it.isNotEmpty() }
            val nullCount = colValues.size - nonNullValues.size
            
            // Identify if the column is primarily numeric
            val numericValues = nonNullValues.mapNotNull { valString ->
                // Strip currency symbols and commas before parsing
                val cleaned = valString.replace("$", "")
                    .replace(",", "")
                    .replace("%", "")
                    .trim()
                cleaned.toDoubleOrNull()
            }
            
            val isNumeric = numericValues.size >= (nonNullValues.size * 0.7) && numericValues.isNotEmpty()
            val sampleValues = nonNullValues.take(5)
            
            if (isNumeric) {
                val min = numericValues.minOrNull()
                val max = numericValues.maxOrNull()
                val sum = numericValues.sum()
                val average = if (numericValues.isNotEmpty()) sum / numericValues.size else 0.0
                
                CsvColumnInsight(
                    name = name,
                    type = "Numeric",
                    sampleValues = sampleValues,
                    nullCount = nullCount,
                    min = min,
                    max = max,
                    sum = sum,
                    average = average
                )
            } else {
                // Check if it's a date or category
                val isDate = nonNullValues.any { it.contains("-") || it.contains("/") } && 
                        nonNullValues.all { it.length >= 6 }
                
                val distribution = nonNullValues.groupBy { it }.mapValues { it.value.size }
                val uniqueCount = distribution.size
                
                val type = when {
                    isDate -> "Date"
                    uniqueCount <= 15 -> "Categorical"
                    else -> "Text"
                }
                
                val valueDistribution = if (uniqueCount <= 15 || type == "Categorical") {
                    distribution.entries
                        .sortedByDescending { it.value }
                        .take(5)
                        .associate { it.key to it.value }
                } else null
                
                CsvColumnInsight(
                    name = name,
                    type = type,
                    sampleValues = sampleValues,
                    nullCount = nullCount,
                    valueDistribution = valueDistribution
                )
            }
        }
    }
}
