package kotlinx.io.json

import java.nio.*

fun decodeAsciiChars(
    input: ByteArray,
    output: StringBuilder
) {
    val buffer = CharArray(input.size)
    for (index in input.indices) {
        buffer[index] = (input[index].toChar())
    }
    output.append(buffer)
}

fun decodeAsciiCharsBB(
    input: ByteBuffer,
    output: StringBuilder
) {
    val buffer = CharArray(input.limit())

    for (index in 0 until input.limit()) {
        buffer[index] = (input[index].toChar())
    }

    output.append(buffer)
}
