package kotlinx.io.text.utf8

import kotlinx.io.*
import kotlinx.io.text.*

/**
 * Feed [consumer] with chars from [Input]. [consumer] returns should we continue to decode.
 *
 * @throws MalformedInputException if decoder fail to recognize charset.
 */
inline fun Input.decodeUtf8Chars(
    crossinline consumer: (Char) -> Boolean
): Int {
    var byteCount = 0
    var value = 0
    var state = STATE_UTF_8
    var count = 0

    while (state != STATE_FINISH && !eof()) {
        count += readBufferRange { buffer, startOffset, endOffset ->
            for (offset in startOffset until endOffset) {
                val byte = buffer.loadByteAt(offset).toInt() and 0xff
                when {
                    byte and 0x80 == 0 -> { // ASCII
                        if (byteCount != 0)
                            malformedInput(value)
                        if (!consumer(byte.toChar())) {
                            state = STATE_FINISH
                            return@readBufferRange offset + 1 - startOffset
                        }
                    }
                    byteCount == 0 -> {
                        // first unicode byte
                        when {
                            byte < 0x80 -> {
                                if (!consumer(byte.toChar())) {
                                    state = STATE_FINISH
                                    return@readBufferRange offset + 1 - startOffset
                                }
                            }
                            byte < 0xC0 -> {
                                byteCount = 0
                                value = byte and 0x7F
                            }
                            byte < 0xE0 -> {
                                byteCount = 1
                                value = byte and 0x3F
                            }
                            byte < 0xF0 -> {
                                byteCount = 2
                                value = byte and 0x1F
                            }
                            byte < 0xF8 -> {
                                byteCount = 3
                                value = byte and 0xF
                            }
                            byte < 0xFC -> {
                                byteCount = 4
                                value = byte and 0x7
                            }
                            byte < 0xFE -> {
                                byteCount = 5
                                value = byte and 0x3
                            }
                        }

                    }
                    else -> {
                        // trailing unicode byte
                        value = (value shl 6) or (byte and 0x7f)
                        byteCount--

                        if (byteCount == 0) {
                            val more = when {
                                value ushr 16 == 0 -> {
                                    if (consumer(value.toChar())) {
                                        count++
                                        true
                                    } else false
                                }
                                else -> {
                                    if (value > MaxCodePoint)
                                        malformedInput(value)

                                    val high = highSurrogate(value).toChar()
                                    val low = lowSurrogate(value).toChar()
                                    if (consumer(high)) {
                                        count++
                                        if (consumer(low)) {
                                            count++
                                            true
                                        } else false
                                    } else false
                                }
                            }
                            if (!more) {
                                state = STATE_FINISH
                                return@readBufferRange offset + 1 - startOffset
                            }

                            value = 0
                        }
                    }
                }
            }
            endOffset - startOffset
        }
    }
    return count
}
