package com.genesyx.app.ui.profile

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
import com.genesyx.app.domain.content.MEDICAL_DISCLAIMER
import com.genesyx.app.domain.content.MedicalSources
import com.genesyx.app.ui.components.CitationList
import com.genesyx.app.ui.components.GenesyxPage
import com.genesyx.app.ui.components.GxBackButton

@Composable
fun MedicalSourcesScreen(navController: NavController) {
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
                    "Medical sources",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onBackground,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            Text(
                MEDICAL_DISCLAIMER,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            CitationList(
                title = "Sources",
                citations = MedicalSources.all,
                modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp, top = 8.dp),
            )
        }
    }
}
