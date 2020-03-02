import kotlinx.io.*
import kotlinx.io.bytes.*
import kotlin.experimental.*


fun Input.readTLSVersion() = (readShort().toInt() and 0xffff).toShort()

fun BytesInput.readTLSServerHello() {
    readTLSVersion()

    val random = readByteArray(32)
    val sessionIdLength = readByte().toInt() and 0xff

    val sessionId = readByteArray(sessionIdLength)
    val suite = readShort()

    val compressionMethod = readByte().toShort() and 0xff

    if (remaining.toInt() == 0) return

    // handle extensions
    val extensionSize = readShort().toInt() and 0xffff

    while (remaining > 0) {
        val type = readShort().toInt() and 0xffff
        val length = readShort().toInt() and 0xffff
        val extension = readByteArray(length)
    }
}

internal fun Input.readTLSRecord() {
    val type = readByte().toInt() and 0xff
    val version = readTLSVersion()

    val length = (readShort().toInt() and 0xffff).let { (it shl 8) or ((it shr 8) and 0xffff) }
    val packet = readBytesInput(length)
}

internal fun BytesInput.readTLSHandshake() {
    val typeAndVersion = readInt()
    val length = typeAndVersion and 0xffffff
    val packet = readBytesInput(length)
}

internal fun Output.writeRecord() {
    writeByte(0)
    writeByte(0)
    writeByte(0)
    writeShort(0)

    val packet: BytesInput = buildInput {  }
    packet.copyTo(this)
    flush()
}
