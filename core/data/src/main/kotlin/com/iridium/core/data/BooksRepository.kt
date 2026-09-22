package com.iridium.core.data

import com.iridium.core.database.BookStore
import com.iridium.core.model.Book
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface BooksRepository {
    fun observeBooks(): Flow<List<Book>>
    fun observeBook(id: String): Flow<Book?>
}

@Singleton
class OfflineFirstBooksRepository @Inject constructor(
    private val store: BookStore,
) : BooksRepository {
    override fun observeBooks(): Flow<List<Book>> = store.observeBooks()
    override fun observeBook(id: String): Flow<Book?> = store.observeBook(id)
}
