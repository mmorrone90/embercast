package dev.firemirror.tv

import android.content.Context

class ReceiverPreferences(context: Context) {
    private val store = context.getSharedPreferences("receiver", Context.MODE_PRIVATE)
    val receiverName: String get() = store.getString("name", DEFAULT_NAME) ?: DEFAULT_NAME
    val diagnosticLogging: Boolean get() = store.getBoolean("diagnostic_logging", false)

    fun save(name: String, logging: Boolean) {
        require(isValidName(name))
        store.edit().putString("name", name.trim()).putBoolean("diagnostic_logging", logging).apply()
    }

    companion object {
        const val DEFAULT_NAME = "Living Room Fire TV"

        // Keep the complete name inside the DNS service label budget for Phase 2.
        fun isValidName(name: String): Boolean {
            val trimmed = name.trim()
            return trimmed.isNotEmpty() && trimmed.toByteArray(Charsets.UTF_8).size <= 63 &&
                trimmed.none { Character.isISOControl(it) }
        }
    }
}
