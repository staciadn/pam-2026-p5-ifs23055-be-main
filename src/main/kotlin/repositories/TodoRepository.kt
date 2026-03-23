package org.delcom.repositories

import org.delcom.dao.TodoDAO
import org.delcom.entities.Todo
import org.delcom.helpers.suspendTransaction
import org.delcom.helpers.todoDAOToModel
import org.delcom.tables.TodoTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import java.util.*

class TodoRepository : ITodoRepository {
    // [MODIFIED] Menggunakan filter dinamis dan pagination (limit, offset)
    override suspend fun getAll(userId: String, search: String, page: Int, perPage: Int, filter: String, urgency: String): List<Todo> = suspendTransaction {
        TodoDAO.find {
            var op: org.jetbrains.exposed.sql.Op<Boolean> = org.jetbrains.exposed.sql.Op.build { TodoTable.userId eq UUID.fromString(userId) }

            if (search.isNotBlank()) {
                op = op and org.jetbrains.exposed.sql.Op.build { TodoTable.title.lowerCase() like "%${search.lowercase()}%" }
            }

            if (filter.equals("selesai", ignoreCase = true)) {
                op = op and org.jetbrains.exposed.sql.Op.build { TodoTable.isDone eq true }
            } else if (filter.equals("belum selesai", ignoreCase = true) || filter.equals("belum", ignoreCase = true)) {
                op = op and org.jetbrains.exposed.sql.Op.build { TodoTable.isDone eq false }
            }

            if (urgency.isNotBlank() && !urgency.equals("semua", ignoreCase = true)) {
                op = op and org.jetbrains.exposed.sql.Op.build { TodoTable.urgency eq urgency.lowercase() }
            }
            op
        }
            .orderBy(TodoTable.createdAt to SortOrder.DESC)
            .limit(perPage)
            .offset(((page - 1) * perPage).toLong())
            .map(::todoDAOToModel)
    }

    // [NEW] Implementasi fungsi untuk mengambil statistik summary todo
    override suspend fun getSummary(userId: String): Map<String, Long> = suspendTransaction {
        val total = TodoTable.selectAll().where { TodoTable.userId eq UUID.fromString(userId) }.count()
        val selesai = TodoTable.selectAll().where { (TodoTable.userId eq UUID.fromString(userId)) and (TodoTable.isDone eq true) }.count()
        val belumSelesai = TodoTable.selectAll().where { (TodoTable.userId eq UUID.fromString(userId)) and (TodoTable.isDone eq false) }.count()

        mapOf(
            "total" to total,
            "selesai" to selesai,
            "belumSelesai" to belumSelesai
        )
    }

    override suspend fun getById(todoId: String): Todo? = suspendTransaction {
        TodoDAO
            .find {
                (TodoTable.id eq UUID.fromString(todoId))
            }
            .limit(1)
            .map(::todoDAOToModel)
            .firstOrNull()
    }

    override suspend fun create(todo: Todo): String = suspendTransaction {
        val todoDAO = TodoDAO.new {
            userId = UUID.fromString(todo.userId)
            title = todo.title
            description = todo.description
            cover = todo.cover
            isDone = todo.isDone
            urgency = todo.urgency // [NEW] Simpan urgency
            createdAt = todo.createdAt
            updatedAt = todo.updatedAt
        }

        todoDAO.id.value.toString()
    }

    override suspend fun update(userId: String, todoId: String, newTodo: Todo): Boolean = suspendTransaction {
        val todoDAO = TodoDAO
            .find {
                (TodoTable.id eq UUID.fromString(todoId)) and
                        (TodoTable.userId eq UUID.fromString(userId))
            }
            .limit(1)
            .firstOrNull()

        if (todoDAO != null) {
            todoDAO.title = newTodo.title
            todoDAO.description = newTodo.description
            todoDAO.cover = newTodo.cover
            todoDAO.isDone = newTodo.isDone
            todoDAO.urgency = newTodo.urgency // [NEW] Update urgency
            todoDAO.updatedAt = newTodo.updatedAt
            true
        } else {
            false
        }
    }

    override suspend fun delete(userId: String, todoId: String): Boolean = suspendTransaction {
        val rowsDeleted = TodoTable.deleteWhere {
            (TodoTable.id eq UUID.fromString(todoId)) and
                    (TodoTable.userId eq UUID.fromString(userId))
        }
        rowsDeleted >= 1
    }

}