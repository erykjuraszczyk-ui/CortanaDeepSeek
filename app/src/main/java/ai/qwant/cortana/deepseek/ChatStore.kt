package ai.qwant.cortana.deepseek

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Local-only conversation storage. The database stays inside the app's private
 * storage and is not synchronized with a server.
 */
class ChatStore(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_MESSAGES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                role TEXT NOT NULL,
                body TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_messages_created_at ON $TABLE_MESSAGES(created_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future schema migrations belong here. Do not drop user history.
    }

    fun addMessage(message: ChatLine) {
        val values = ContentValues().apply {
            put("role", message.role)
            put("body", message.text)
            put("created_at", System.currentTimeMillis())
        }
        writableDatabase.insertOrThrow(TABLE_MESSAGES, null, values)
    }

    fun loadMessages(limit: Int = MAX_HISTORY): List<ChatLine> {
        val result = ArrayList<ChatLine>()
        readableDatabase.query(
            TABLE_MESSAGES,
            arrayOf("role", "body"),
            null,
            null,
            null,
            null,
            "id DESC",
            limit.toString()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    ChatLine(
                        role = cursor.getString(0),
                        text = cursor.getString(1)
                    )
                )
            }
        }
        result.reverse()
        return result
    }

    fun clearMessages() {
        writableDatabase.delete(TABLE_MESSAGES, null, null)
    }

    companion object {
        private const val DATABASE_NAME = "cortana_local_memory.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_MESSAGES = "messages"
        private const val MAX_HISTORY = 500
    }
}
