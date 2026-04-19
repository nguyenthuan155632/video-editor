package com.videoeditor.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.videoeditor.core.designsys.AuroraBackground
import com.videoeditor.core.designsys.AuroraChip
import com.videoeditor.core.designsys.AuroraFab
import com.videoeditor.core.designsys.GlassCard
import com.videoeditor.core.designsys.GradientIconBadge
import com.videoeditor.core.navigation.FeatureCard
import com.videoeditor.core.navigation.FeatureRegistry
import com.videoeditor.core.navigation.Routes
import com.videoeditor.core.theme.AuroraGradients
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.core.theme.AuroraTextSecondary

@Composable
fun HomeScreen(onOpenFeature: (String) -> Unit) {
    AuroraBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                HomeHero()
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(FeatureRegistry.cards, key = { it.id }) { card ->
                        FeatureCardView(card) { route -> route?.let(onOpenFeature) }
                    }
                }
            }
            AuroraFab(
                text = "Quick compress",
                icon = Icons.Outlined.Bolt,
                onClick = { onOpenFeature(Routes.COMPRESS) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun HomeHero() {
    val gradient = remember { AuroraGradients.horizontal() }
    Column(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp),
    ) {
        Text(
            text = "Video Editor",
            style = MaterialTheme.typography.displayLarge.copy(brush = gradient),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Compress · Trim · Convert · More",
            style = MaterialTheme.typography.bodyMedium,
            color = AuroraTextSecondary,
        )
    }
}

@Composable
private fun FeatureCardView(card: FeatureCard, onClick: (String?) -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press-scale")
    val enabled = card.route != null
    val scope = rememberCoroutineScope()

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .scale(scale),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(18.dp),
        onClick = if (enabled) {
            {
                scope.launch {
                    pressed = true
                    delay(80)
                    pressed = false
                    delay(20)
                    onClick(card.route)
                }
            }
        } else null,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                GradientIconBadge(icon = card.icon, size = 44.dp)
                Column {
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = AuroraTextPrimary,
                    )
                    Text(
                        text = card.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = AuroraTextSecondary,
                    )
                }
            }
            if (!enabled) {
                AuroraChip(
                    label = "Soon",
                    selected = true,
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
    }
}
