package ai.qwant.cortana.deepseek

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class MemoryNote(val id: Long, val content: String, val createdAt: Long)

class ChatStore(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    "cortana_local_memory.db",
    null,
    2
) {
    override fun onCreate(db: SQLiteDatabase) {
        createMessagesTable(db)
        createNotesTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createNotesTable(db)
    }

    fun addMessage(message: ChatLine) {
        val values = ContentValues().apply {
            put("role", message.role)
            put("body", message.text)
            put("created_at", System.currentTimeMillis())
        }
        writableDatabase.insertOrThrow("messages", null, values)
    }

    fun loadMessages(limit: Int = 500): List<ChatLine> {
        val result = ArrayList<ChatLine>()
        readableDatabase.query(
            "messages", arrayOf("role", "body"), null, null, null, null,
            "id DESC", limit.coerceIn(1, 500).toString()
        ).use { cursor ->
            while (cursor.moveToNext()) result.add(ChatLine(cursor.getString(0), cursor.getString(1)))
        }
        result.reverse()
        return result
    }

    fun clearMessages() {
        writableDatabase.delete("messages", null, null)
    }

    fun addNote(content: String): Long {
        val clean = content.trim().take(3000)
        require(clean.isNotEmpty()) { "Notatka nie może być pusta." }
        val values = ContentValues().apply {
            put("content", clean)
            put("created_at", System.currentTimeMillis())
        }
        return writableDatabase.insertOrThrow("memory_notes", null, values)
    }

    fun loadNotes(): List<MemoryNote> {
        val result = ArrayList<MemoryNote>()
        readableDatabase.query(
            "memory_notes", arrayOf("id", "content", "created_at"),
            null, null, null, null, "id DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(MemoryNote(cursor.getLong(0), cursor.getString(1), cursor.getLong(2)))
            }
        }
        return result
    }

    fun deleteNote(id: Long) {
        writableDatabase.delete("memory_notes", "id = ?", arrayOf(id.toString()))
    }

    fun clearNotes() {
        writableDatabase.delete("memory_notes", null, null)
    }

    private fun createMessagesTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                role TEXT NOT NULL,
                body TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_messages_created_at ON messages(created_at)")
    }

    private fun createNotesTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS memory_notes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                content TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}
