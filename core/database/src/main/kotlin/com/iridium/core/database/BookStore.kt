package com.iridium.core.database

import com.iridium.core.model.Book
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** In-memory stand-in. Phase 2 replaces with Room BookEntity/BookDao. */
@Singleton
class BookStore @Inject constructor() {
    private val books = MutableStateFlow<List<Book>>(emptyList())

    fun observeBooks(): Flow<List<Book>> = books

    fun observeBook(id: String): Flow<Book?> = books.map { list -> list.firstOrNull { it.id == id } }
}
