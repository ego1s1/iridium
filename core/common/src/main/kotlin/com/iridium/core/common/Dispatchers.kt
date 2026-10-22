package com.iridium.core.common

import javax.inject.Qualifier

/** I/O-bound work (filesystem, database, network). Backed by `Dispatchers.IO` in prod. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** CPU-bound work (mapping, filtering, parsing). Backed by `Dispatchers.Default` in prod. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
