package com.videoeditor.core.designsys

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.videoeditor.core.theme.glass

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val base = modifier.glass(cornerRadius = cornerRadius)
    val withClick = if (onClick != null) base.clickable(onClick = onClick) else base
    Box(modifier = withClick.padding(contentPadding)) {
        content()
    }
}
