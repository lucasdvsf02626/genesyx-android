package com.genesyx.app.ui.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.genesyx.app.domain.content.searchArticles
import com.genesyx.app.ui.components.GxBackButton
import com.genesyx.app.ui.navigation.Screen
import com.genesyx.app.ui.theme.ElectricLavender

/** Client-side search over ten bundled articles. No debounce, no index — it's ten string compares. */
@Composable
fun LearnSearchScreen(navController: NavController) {
    val colors = MaterialTheme.colorScheme
    var query by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val results = searchArticles(query)

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GxBackButton(onClick = { navController.popBackStack() })
            Spacer(Modifier.size(4.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                placeholder = { Text("Search articles") },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = colors.onSurfaceVariant) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.Close, "Clear search", tint = colors.onSurfaceVariant)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricLavender,
                    unfocusedBorderColor = colors.outline,
                ),
            )
        }

        Spacer(Modifier.height(16.dp))

        when {
            query.isBlank() -> EmptyState(
                title = "Search the Learn section",
                body = "Try \"hydration\", \"habits\", or \"insights\".",
            )

            results.isEmpty() -> EmptyState(
                title = "No articles match “$query”",
                body = "Try a shorter word, or browse everything from the Learn tab.",
            )

            else -> LazyColumn {
                items(results, key = { it.id }) { article ->
                    ArticleRow(
                        article = article,
                        onClick = { navController.navigate(Screen.ArticleDetail.create(article.slug)) },
                        modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp),
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.Search,
            null,
            tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    }
}
