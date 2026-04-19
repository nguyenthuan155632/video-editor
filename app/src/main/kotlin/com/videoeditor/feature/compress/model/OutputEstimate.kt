package com.videoeditor.feature.compress.model

data class OutputEstimate(
    val sizeBytes: Long,
    val ratio: Double,
    val effectiveBitrateKbps: Int,
    val notes: List<String>,
)