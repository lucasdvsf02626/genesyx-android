package com.genesyx.app.core.di

import javax.inject.Qualifier

/** Application-lifetime [kotlinx.coroutines.CoroutineScope] for repository-owned background work. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
