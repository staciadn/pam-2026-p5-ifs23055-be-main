package org.delcom.repositories

import org.delcom.entities.Todo

interface ITodoRepository {
    // [MODIFIED] Menambahkan parameter page, perPage, filter, dan urgency
    suspend fun getAll(
        userId: String,
        search: String,
        page: Int,
        perPage: Int,
        filter: String,
        urgency: String
    ): List<Todo>

    // [NEW] Fungsi untuk mengambil statistik/summary todos untuk halaman Home
    suspend fun getSummary(userId: String): Map<String, Long>

    suspend fun getById(todoId: String): Todo?
    suspend fun create(todo: Todo): String
    suspend fun update(userId: String, todoId: String, newTodo: Todo): Boolean
    suspend fun delete(userId: String, todoId: String): Boolean
}