package dev.fridgechef.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.fridgechef.data.IngredientLine
import dev.fridgechef.data.Quantities
import dev.fridgechef.data.RecipeDetail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeScreen(
    state: UiState,
    onBack: () -> Unit,
    onServings: (Int) -> Unit,
) {
    val detail = state.detail
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.title ?: "Recipe", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.detailLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.detailError != null -> Text(state.detailError, Modifier.align(Alignment.Center).padding(32.dp))
                detail != null -> RecipeContent(detail, state.servings, onServings)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeContent(detail: RecipeDetail, servings: Int?, onServings: (Int) -> Unit) {
    val uri = LocalUriHandler.current
    val scale = if (detail.servings != null && servings != null) servings.toDouble() / detail.servings else 1.0
    val toBuy = detail.ingredients.count { !it.have && !it.isStaple }

    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            AsyncImage(
                model = detail.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f),
            )
        }

        item {
            Column(Modifier.padding(16.dp)) {
                Text(detail.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    detail.readyInMinutes?.let { AssistChip(onClick = {}, label = { Text("$it min") }) }
                    detail.tags.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
                    AssistChip(onClick = {}, label = { Text("via ${detail.source.label}") })
                }
            }
        }

        item {
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Ingredients", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (detail.servings != null && servings != null) {
                    FilledTonalIconButton(onClick = { onServings(servings - 1) }, modifier = Modifier.size(36.dp)) {
                        Text("−", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "$servings serving${if (servings == 1) "" else "s"}",
                        Modifier.padding(horizontal = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    FilledTonalIconButton(onClick = { onServings(servings + 1) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Add, "More servings", Modifier.size(18.dp))
                    }
                }
            }
            Text(
                if (toBuy == 0) "You have everything you need."
                else "You have ${detail.ingredients.count { it.have }} of ${detail.ingredients.size}. $toBuy to buy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        itemsIndexed(detail.ingredients) { index, ingredient ->
            IngredientRow(ingredient, scale)
            if (index < detail.ingredients.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        }

        if (detail.servings == null) {
            item {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "This source doesn't list servings; quantities are as published (usually 2–4 people).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Text(
                "Instructions",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
            )
        }

        if (detail.steps.isEmpty()) {
            item { Text("No instructions provided. Open the original recipe below.", Modifier.padding(horizontal = 16.dp)) }
        }

        itemsIndexed(detail.steps) { index, step ->
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${index + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(12.dp))
                Text(step, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            }
        }

        item {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                detail.sourceUrl?.let { url ->
                    OutlinedButton(onClick = { uri.openUri(url) }, modifier = Modifier.fillMaxWidth()) { Text("Open original recipe") }
                }
                detail.videoUrl?.let { url ->
                    OutlinedButton(onClick = { uri.openUri(url) }, modifier = Modifier.fillMaxWidth()) { Text("Watch video") }
                }
            }
        }
    }
}

@Composable
private fun IngredientRow(ingredient: IngredientLine, scale: Double) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            ingredient.have -> Icon(Icons.Filled.CheckCircle, "You have this", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            ingredient.isStaple -> Box(
                Modifier.size(20.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            )
            else -> Icon(Icons.Filled.ShoppingCart, "To buy", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            ingredient.name.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            Quantities.line(ingredient, scale),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
