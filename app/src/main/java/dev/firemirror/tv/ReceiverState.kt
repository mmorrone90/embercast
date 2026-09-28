package dev.firemirror.tv

/** Actual protocol callbacks will drive these states in Phase 3. */
enum class ReceiverState(val label: Int) {
    UNAVAILABLE(R.string.receiver_unavailable),
    WAITING(R.string.waiting),
    CONNECTING(R.string.connecting),
    CONNECTED(R.string.connected),
    DISCONNECTED(R.string.disconnected),
    ERROR(R.string.error)
}

data class ReceiverSnapshot(
    val state: ReceiverState = ReceiverState.UNAVAILABLE,
    val deviceName: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val framesPerSecond: Double? = null
)
