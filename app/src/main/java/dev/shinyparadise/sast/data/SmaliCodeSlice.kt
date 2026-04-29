package dev.shinyparadise.sast.data

data class SmaliCodeSlice(
    val file: String,
    val className: String?,
    val methodName: String?,
    val startLine: Int,
    val endLine: Int,
    val signals: List<String>,
    val code: String,
)
