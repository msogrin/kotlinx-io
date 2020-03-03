package kotlinx.io.json.internal

import kotlinx.io.*
import kotlinx.io.json.*
import kotlinx.io.text.*
import kotlin.jvm.*

@UseExperimental(ExperimentalStdlibApi::class)
private val BINARY_TRUE: ByteArray = "true".encodeToByteArray()

@UseExperimental(ExperimentalStdlibApi::class)
private val BINARY_FALSE: ByteArray = "false".encodeToByteArray()

internal class Composer(@JvmField internal val output: Output, private val json: ioJson) {
    private var level = 0
    var writingFirst = true
        private set

    fun indent() {
        writingFirst = true; level++
    }

    fun unIndent() {
        level--
    }

    fun nextItem() {
        writingFirst = false
        if (json.configuration.prettyPrint) {
            print("\n")
            repeat(level) { print(json.configuration.indent) }
        }
    }

    fun space() {
        if (json.configuration.prettyPrint)
            print(' ')
    }

    fun print(v: Char) {
        output.writeUtf8Char(v)
    }

    fun print(v: String) {
        output.writeASCIIString(v)
    }

    fun print(v: Float) {
        output.writeASCIIString(v.toString())
    }

    fun print(v: Double) {
        output.writeASCIIString(v.toString())
    }

    fun print(v: Byte) {
        output.writeASCIIString(v.toString())
    }

    fun print(v: Short) {
        output.writeASCIIString(v.toString())
    }

    fun print(v: Int) {
        output.writeASCIIString(v.toString())
    }

    fun print(v: Long) {
        output.writeASCIIString(v.toString())
    }

    fun print(v: Boolean) {
        val value = if (v) {
            BINARY_TRUE
        } else {
            BINARY_FALSE
        }

        output.writeByteArray(value)
    }

    fun printQuoted(value: String): Unit = output.printQuoted(value)
}

internal fun Output.printQuoted(value: String) {
    writeByte(STRING_BYTE)
    var lastPos = 0
    val length = value.length
    for (index in 0 until length) {
        val code = value[index].toInt()

        if (code >= ESCAPE_CHARS.size) {
            // No need to escape
            continue
        }

        val escapeSequence = ESCAPE_CHARS[code] ?: continue
        writeUtf8String(value, lastPos, index - lastPos) // Flush without escape
        writeUtf8String(escapeSequence)

        lastPos = index + 1
    }

    writeUtf8String(value, lastPos, length - lastPos)
    writeByte(STRING_BYTE)
}
