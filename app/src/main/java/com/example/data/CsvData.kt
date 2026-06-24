package com.example.data

data class CsvData(
    val headers: List<String>,
    val rows: List<List<String>>,
    val fileName: String,
    val sizeBytes: Long
) {
    val rowCount: Int get() = rows.size
    val columnCount: Int get() = headers.size
}

data class CsvColumnInsight(
    val name: String,
    val type: String, // "Numeric", "Date", "Categorical", "Text"
    val sampleValues: List<String>,
    val nullCount: Int,
    val min: Double? = null,
    val max: Double? = null,
    val average: Double? = null,
    val sum: Double? = null,
    val valueDistribution: Map<String, Int>? = null // Value -> Count
)
