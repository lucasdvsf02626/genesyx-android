package com.genesyx.app.ui.learn

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.genesyx.app.domain.content.Article
import com.genesyx.app.domain.content.ArticleCategory
import com.genesyx.app.domain.content.learnArticles
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.navigation.Screen
import com.genesyx.app.ui.theme.ElectricBlue
import com.genesyx.app.ui.theme.ElectricLavender
import com.genesyx.app.ui.theme.ElectricPink

/**
 * Learn landing. Content is a compile-time constant ([learnArticles]) so there is no loading state
 * and no error state — see docs/V1_1_NOTIFICATIONS_AND_LEARN.md §10.1. The ViewModel exists only for
 * the persisted first-time hint.
 */
@Composable
fun LearnScreen(
    navController: NavController,
    viewModel: LearnViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val introSeen by viewModel.introSeen.collectAsState()
    var selectedCategory by rememberSaveable { mutableStateOf<ArticleCategory?>(null) }

    val visible = learnArticles.filter { selectedCategory == null || it.category == selectedCategory }
    // The featured hero only leads the unfiltered list; inside a filter it's just another article.
    val featured = if (selectedCategory == null) visible.firstOrNull { it.featured } else null
    val rest = visible.filter { it != featured }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 12.dp, top = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Eyebrow("Learn", color = ElectricLavender)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Short reads",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onBackground,
                    )
                }
                IconButton(
                    onClick = { navController.navigate(Screen.LearnSearch.route) },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.Outlined.Search, "Search articles", tint = colors.onBackground)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Tracking, nutrition, and what your patterns mean.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(16.dp))
        }

        if (!introSeen) {
            item {
                IntroCard(
                    onDismiss = viewModel::dismissIntro,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        item {
            CategoryChips(
                selected = selectedCategory,
                onSelect = { selectedCategory = it },
            )
            Spacer(Modifier.height(16.dp))
        }

        if (featured != null) {
            item {
                FeaturedCard(
                    article = featured,
                    onClick = { navController.navigate(Screen.ArticleDetail.create(featured.slug)) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(20.dp))
            }
        }

        items(rest, key = { it.id }) { article ->
            ArticleRow(
                article = article,
                onClick = { navController.navigate(Screen.ArticleDetail.create(article.slug)) },
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp),
            )
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun CategoryChips(selected: ArticleCategory?, onSelect: (ArticleCategory?) -> Unit) {
    val colors = MaterialTheme.colorScheme
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricLavender.copy(alpha = 0.12f),
                    selectedLabelColor = ElectricLavender,
                    containerColor = colors.surface,
                ),
            )
        }
        items(ArticleCategory.entries.toList(), key = { it.name }) { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(if (selected == category) null else category) },
                label = { Text(category.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = category.accent().copy(alpha = 0.12f),
                    selectedLabelColor = category.accent(),
                    containerColor = colors.surface,
                ),
            )
        }
    }
}

/** One dismissible card on first visit. Not a coach-mark tour. */
@Composable
private fun IntroCard(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ElectricLavender.copy(alpha = 0.08f))
            .padding(start = 18.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "New here? Start with “Your first week with Genesyx”. Everything else can wait.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Filled.Close, "Dismiss", tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

/** Placeholder hero until real artwork exists — a brand gradient keyed to the category. */
internal fun ArticleCategory.accent(): Color = when (this) {
    ArticleCategory.GETTING_STARTED -> ElectricBlue
    ArticleCategory.NUTRITION -> ElectricLavender
    ArticleCategory.TRACKING -> ElectricBlue
    ArticleCategory.INSIGHTS -> ElectricPink
    ArticleCategory.WELLNESS -> ElectricLavender
}

/**
 * Article hero. Renders [Article.heroImage] when the art exists; otherwise a brand gradient keyed to
 * the category, so the slot is reserved and every article lays out identically either way.
 */
@Composable
internal fun ArticleHero(article: Article, modifier: Modifier = Modifier) {
    val hero = article.heroImage
    if (hero != null) {
        Image(
            painter = painterResource(hero),
            contentDescription = null, // Decorative — the title carries the meaning.
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        val accent = article.category.accent()
        Box(
            modifier = modifier.background(
                Brush.linearGradient(listOf(accent.copy(alpha = 0.28f), accent.copy(alpha = 0.06f))),
            ),
        )
    }
}

@Composable
private fun FeaturedCard(article: Article, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.surface)
            .clickable(onClick = onClick),
    ) {
        ArticleHero(article, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
        Column(Modifier.padding(20.dp)) {
            Eyebrow(article.category.label, color = article.category.accent())
            Spacer(Modifier.height(8.dp))
            Text(
                article.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(article.excerpt, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Text(article.readingTime, fontSize = 11.5.sp, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ArticleRow(article: Article, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArticleHero(article, Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)))
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(article.title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(
                "${article.category.label} · ${article.readingTime}",
                fontSize = 11.5.sp,
                color = colors.onSurfaceVariant,
            )
        }
        Icon(Icons.Filled.ChevronRight, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}
