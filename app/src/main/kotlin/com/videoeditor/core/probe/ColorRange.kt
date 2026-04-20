package com.videoeditor.core.probe

enum class ColorRange {
    FULL,    // pc / full / jpeg — phone camera videos are typically this
    LIMITED, // tv / limited / mpeg — broadcast standard
    UNKNOWN, // could not detect — don't touch
}
