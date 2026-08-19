package com.genesyx.app

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/** Debug-only empty host for instrumented Compose + Hilt tests. Not in release. */
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity()
