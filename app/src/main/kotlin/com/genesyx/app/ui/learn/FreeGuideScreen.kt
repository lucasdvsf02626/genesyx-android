package com.genesyx.app.ui.learn

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.genesyx.app.domain.content.FreeGuideBlock
import com.genesyx.app.domain.content.FreeGuideContent
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.GenesyxPage
import com.genesyx.app.ui.components.GxBackButton
import com.genesyx.app.ui.theme.ElectricLavender

@Composable
fun FreeGuideScreen(navController: NavController) {
    val colors = MaterialTheme.colorScheme
    GenesyxPage {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GxBackButton(onClick = { navController.popBackStack() })
                Text(
                    FreeGuideContent.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onBackground,
                    modifier = Modifier.padding(start = 4.dp, end = 16.dp),
                )
            }
            Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp, top = 8.dp)) {
                FreeGuideContent.pages.forEach { page ->
                    Eyebrow("Page ${page.number}", color = ElectricLavender)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        page.heading,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onBackground,
                    )
                    Spacer(Modifier.height(10.dp))
                    page.blocks.forEach { block ->
                        when (block) {
                            is FreeGuideBlock.Paragraph -> Text(
                                block.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.onSurfaceVariant,
                            )
                            is FreeGuideBlock.Subheading -> Text(
                                block.text,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onBackground,
                            )
                            is FreeGuideBlock.Bullets -> block.items.forEach { item ->
                                Text(
                                    "• $item",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
