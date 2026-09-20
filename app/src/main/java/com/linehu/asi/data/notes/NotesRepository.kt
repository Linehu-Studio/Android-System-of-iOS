package com.linehu.asi.data.notes

import kotlinx.coroutines.flow.Flow
import java.util.UUID

class NotesRepository(private val dao: NoteDao) {

    val allNotes: Flow<List<NoteEntity>> = dao.all()

    suspend fun save(title: String, body: String, existingId: String? = null) {
        val id = existingId ?: UUID.randomUUID().toString()
        dao.upsert(
            NoteEntity(
                id = id,
                title = title.ifBlank { body.lineSequence().firstOrNull().orEmpty() },
                body = body,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun byId(id: String): NoteEntity? = dao.byId(id)

    suspend fun delete(id: String) = dao.delete(id)
}
