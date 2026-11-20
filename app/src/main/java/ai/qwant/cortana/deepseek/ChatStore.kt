package ai.qwant.cortana.deepseek

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class MemoryNote(
    val id: Long,
    val content: String,
    val createdAt: Long
)

/** Local-only storage in the application's private SQLite database. */
class ChatStore(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        createMessagesTable(db)
        createNotesTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createNotesTable(db)
    }

    fun addMessage(message: ChatLine) {
        require(message.role == "user" || message.role == "assistant") {
            "Nieobsługiwana rola wiadomości."
        }
        val values = ContentValues().apply {
            put("role", message.role)
            put("body", message.text.take(MAX_MESSAGE_LENGTH))
            put("created_at", System.currentTimeMillis())
        }
        writableDatabase.insertOrThrow(TABLE_MESSAGES, null, values)
    }

    fun loadMessages(limit: Int = MAX_HISTORY): List<ChatLine> {
        val result = ArrayList<ChatLine>()
        readableDatabase.query(
            TABLE_MESSAGES,
            arrayOf("role", "body"),
            null, null, null, null,
            "id DESC",
            limit.coerceIn(1, MAX_HISTORY).toString()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(ChatLine(cursor.getString(0), cursor.getString(1)))
            }
        }
        result.reverse()
        return result
    }

    fun clearMessages() {
        writableDatabase.delete(TABLE_MESSAGES, null, null)
    }

    fun addNote(content: String): Long {
        val clean = content.trim().take(MAX_NOTE_LENGTH)
        require(clean.isNotEmpty()) { "Notatka nie może być pusta." }
        val values = ContentValues().apply {
            put("content", clean)
            put("created_at", System.currentTimeMillis())
        }
        return writableDatabase.insertOrThrow(TABLE_NOTES, null, values)
    }

    fun loadNotes(): List<MemoryNote> {
        val result = ArrayList<MemoryNote>()
        readableDatabase.query(
            TABLE_NOTES,
            arrayOf("id", "content", "created_at"),
            null, null, null, null,
            "id DESC",
            MAX_NOTES.toString()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(MemoryNote(cursor.getLong(0), cursor.getString(1), cursor.getLong(2)))
            }
        }
        return result
    }

    fun deleteNote(id: Long) {
        writableDatabase.delete(TABLE_NOTES, "id = ?", arrayOf(id.toString()))
    }

    fun clearNotes() {
        writableDatabase.delete(TABLE_NOTES, null, null)
    }

    private fun createMessagesTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_MESSAGES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                role TEXT NOT NULL,
                body TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_created_at ON $TABLE_MESSAGES(created_at)")
    }

    private fun createNotesTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_NOTES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                content TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    companion object {
        private const val DATABASE_NAME = "cortana_local_memory.db"
        private const val DATABASE_VERSION = 2
        private const val TABLE_MESSAGES = "messages"
        private const val TABLE_NOTES = "memory_notes"
        private const val MAX_HISTORY = 500
        private const val MAX_NOTES = 500
        private const val MAX_MESSAGE_LENGTH = 20000
        private const val MAX_NOTE_LENGTH = 3000
    }
}
