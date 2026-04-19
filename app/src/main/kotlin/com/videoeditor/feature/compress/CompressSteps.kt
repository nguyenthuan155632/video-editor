package com.videoeditor.feature.compress

import com.videoeditor.core.designsys.Step

object CompressSteps {
    val ALL: List<Step> = listOf(
        Step("pick", "Pick"),
        Step("configure", "Configure"),
        Step("run", "Run"),
        Step("done", "Done"),
    )
}
