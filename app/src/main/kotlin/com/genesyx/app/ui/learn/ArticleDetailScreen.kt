package com.genesyx.app.ui.learn

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.genesyx.app.core.AppLinks
import com.genesyx.app.domain.content.Article
import com.genesyx.app.domain.content.ArticleBlock
import com.genesyx.app.domain.content.ArticleCta
import com.genesyx.app.domain.content.CtaType
import com.genesyx.app.domain.content.MEDICAL_DISCLAIMER
import com.genesyx.app.domain.content.articleBySlug
import com.genesyx.app.domain.content.relatedArticles
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.GxBackButton
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.navigation.Screen

@Composable
fun ArticleDetailScreen(slug: String, navController: NavController) {
    val article = articleBySlug(slug)
    if (article == null) {
        ArticleNotFound(onBack = { navController.popBackStack() })
        return
    }

    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState()),
    ) {
        val context = LocalContext.current
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GxBackButton(onClick = { navController.popBackStack() })
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = { context.shareArticle(article) },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Filled.Share, "Share article", tint = colors.onBackground)
            }
        }

        ArticleHero(
            article,
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(24.dp)),
        )

        Column(Modifier.padding(horizontal = 24.dp).padding(top = 20.dp)) {
            Eyebrow(
                "${article.category.label} · ${article.readingTime}",
                color = article.category.accent(),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                article.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onBackground,
            )
            Spacer(Modifier.height(20.dp))

            article.body.forEach { block ->
                ArticleBlockView(block)
                Spacer(Modifier.height(16.dp))
            }

            article.cta?.let { cta ->
                Spacer(Modifier.height(8.dp))
                CtaCard(cta) {
                    val route = cta.route()
                    navController.navigate(route) {
                        // A CTA into a tab must reuse that tab, not stack a second copy behind us.
                        if (Screen.bottomTabs.any { it.route == route }) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            restoreState = true
                        }
                        launchSingleTop = true
                    }
                }
            }

            if (article.disclaimerRequired) {
                Spacer(Modifier.height(24.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outline.copy(alpha = 0.6f)))
                Spacer(Modifier.height(16.dp))
                Text(
                    MEDICAL_DISCLAIMER,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }

            val related = relatedArticles(article)
            if (related.isNotEmpty()) {
                Spacer(Modifier.height(32.dp))
                Text(
                    "Related",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onBackground,
                )
                Spacer(Modifier.height(12.dp))
                related.forEach { other ->
                    ArticleRow(
                        article = other,
                        // Replace, don't stack: three taps through Related shouldn't need three backs.
                        onClick = {
                            navController.navigate(Screen.ArticleDetail.create(other.slug)) {
                                popUpTo(Screen.ArticleDetail.route) { inclusive = true }
                            }
                        },
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

/** In-app destination for a CTA. Deep links (`genesyx://…`) arrive with the notification work. */
private fun ArticleCta.route(): String = when (type) {
    CtaType.OPEN_LOG -> Screen.Log.route
    CtaType.OPEN_TRACK -> Screen.Track.route
    CtaType.OPEN_NUTRITION -> Screen.Nutrition.route
    CtaType.OPEN_INSIGHTS -> Screen.Insights.route
    // Non-null by construction — ArticleCta's init block rejects OPEN_ARTICLE without a targetSlug.
    CtaType.OPEN_ARTICLE -> Screen.ArticleDetail.create(checkNotNull(targetSlug))
}

@Composable
private fun ArticleBlockView(block: ArticleBlock) {
    val colors = MaterialTheme.colorScheme
    when (block) {
        is ArticleBlock.Heading -> Text(
            block.text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.onBackground,
            modifier = Modifier.padding(top = 8.dp),
        )

        is ArticleBlock.Paragraph -> Text(
            block.text,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 26.sp,
            color = colors.onSurfaceVariant,
        )

        is ArticleBlock.BulletList -> Column {
            block.items.forEach { item ->
                Row(Modifier.padding(bottom = 10.dp)) {
                    Box(
                        Modifier
                            .padding(top = 9.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(colors.onSurfaceVariant),
                    )
                    Spacer(Modifier.size(14.dp))
                    Text(
                        item,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }

        is ArticleBlock.Callout -> Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surface)
                .padding(18.dp),
        ) {
            Text(
                block.text,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
                color = colors.onSurface,
            )
        }
    }
}

@Composable
private fun CtaCard(cta: ArticleCta, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surfaceVariant)
            .padding(20.dp),
    ) {
        GxPrimaryButton(text = cta.label, onClick = onClick)
    }
}

/** Plain-text share. Links to the site root, not a per-article URL — see [AppLinks.SITE_URL]. */
private fun Context.shareArticle(article: Article) {
    val text = "${article.title}\n\n${article.excerpt}\n\nFrom the Genesyx app — ${AppLinks.SITE_URL}"
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, article.title)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching { startActivity(Intent.createChooser(send, null)) }
}

@Composable
private fun ArticleNotFound(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize().background(colors.background),
    ) {
        Row(Modifier.padding(start = 8.dp, top = 8.dp)) { GxBackButton(onClick = onBack) }
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "That article isn't available.",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onBackground,
            )
            Spacer(Modifier.height(24.dp))
            GxPrimaryButton(text = "Back to Learn", onClick = onBack)
        }
    }
}
