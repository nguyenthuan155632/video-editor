package com.videoeditor.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.videoeditor.core.navigation.FeatureCard
import com.videoeditor.core.navigation.FeatureRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenFeature: (String) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Video Editor") }) },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding),
        ) {
            items(FeatureRegistry.cards, key = { it.id }) { card ->
                FeatureCardView(card) { route -> route?.let(onOpenFeature) }
            }
        }
    }
}

@Composable
private fun FeatureCardView(card: FeatureCard, onClick: (String?) -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press-scale")
    val enabled = card.route != null
    Surface(
        shape = RoundedCornerShape(24.dp),
        tonalElevation = if (enabled) 2.dp else 0.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .scale(scale)
            .alpha(if (enabled) 1f else 0.55f)
            .clickable(enabled = enabled) {
                pressed = true
                onClick(card.route)
                pressed = false
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start,
        ) {
            Icon(card.icon, contentDescription = null)
            Column {
                Text(card.title, style = MaterialTheme.typography.headlineSmall)
                Text(card.subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}