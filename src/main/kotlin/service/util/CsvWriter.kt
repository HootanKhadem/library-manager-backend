package com.dw.service.util

object CsvWriter {

    fun write(headers: List<String>, rows: List<List<String?>>): String {
        val builder = StringBuilder()
        builder.append(headers.joinToString(",") { escape(it) }).append("\r\n")
        rows.forEach { row ->
            builder.append(row.joinToString(",") { escape(it ?: "") }).append("\r\n")
        }
        return builder.toString()
    }

    private fun escape(field: String): String {
        val needsQuoting = field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")
        if (!needsQuoting) return field
        return "\"" + field.replace("\"", "\"\"") + "\""
    }
}
