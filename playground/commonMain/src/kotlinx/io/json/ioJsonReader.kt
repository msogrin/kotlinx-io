/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package kotlinx.io.json

import kotlinx.io.*
import kotlinx.io.text.utf8.*
import kotlinx.serialization.json.*

internal class ioJsonReader(private val source: Input) {
    internal var position = 0

    val isDone: Boolean get() = (nextToken() == TOKEN_EOF)

    public fun nextToken(): Byte {
        var resultToken: Byte = TOKEN_EOF

        position += source.readUntilExclusive {
            when (val token = charToTokenClass(it)) {
                TC_WS -> return@readUntilExclusive true
                else -> {
                    resultToken = token
                    return@readUntilExclusive false
                }
            }
        }

        if (resultToken == TC_INVALID) {
            fail("Unexpected token")
        }

        return resultToken
    }

    public fun nextLiteral(): String = buildString {
        nextToken()

        source.preview {
            readUntilExclusive {
                if (charToTokenClass(it) == TC_OTHER
                ) {
                    append(it.toChar())
                    return@readUntilExclusive true
                } else {
                    return@readUntilExclusive false
                }
            }
        }
    }

    public fun takeToken(): Byte {
        var resultToken: Byte = TC_INVALID

        position += source.readUntil {
            when (val token = charToTokenClass(it)) {
                TC_WS -> return@readUntil true
                else -> {
                    resultToken = token
                    return@readUntil false
                }
            }
        }

        return if (resultToken != TC_INVALID) {
            resultToken
        } else {
            TOKEN_EOF
        }
    }

    public fun takeStringOrLiteral(): String = if (nextToken() == TC_STRING) {
        takeString()
    } else {
        takeLiteral()
    }

    public fun takeString(): String = buildString {
        var escape = false
        var unicodeEscape = false
        var unicodeChar = 0
        var unicodeSize = 4

        var last: Char = INVALID
        consume(TC_STRING) { "Expected '\"'" }

        val count = source.decodeUtf8Chars {
            last = it
            if (escape) {
                escape = false
                if (it == UNICODE_ESC) {
                    unicodeEscape = true
                    return@decodeUtf8Chars true
                }

                val unescaped = escapeToChar(it.toInt())
                require(unescaped != INVALID, position) { "Invalid escaped char '$it'" }
                append(unescaped)
                return@decodeUtf8Chars true
            }

            if (unicodeEscape) {
                unicodeChar = fromHexChar(it) or (unicodeChar shl 4)
                unicodeSize -= 1

                if (unicodeSize == 0) {
                    append(unicodeChar.toChar())
                    unicodeEscape = false
                    unicodeChar = 0
                    unicodeSize = 4
                }

                return@decodeUtf8Chars true
            }

            if (it == STRING_ESC) {
                escape = true
                return@decodeUtf8Chars true
            }

            if (it != STRING) {
                append(it)
            }

            it != STRING
        }

        position += count
        if (count == 0 || last != STRING) {
            fail("Expected '$STRING'")
        }
    }

    public fun takeLiteral(): String {
        if (nextToken() != TC_OTHER) {
            fail("Expected literal, but got '${nextToken()}'")
        }

        return buildString {
            position += source.readUntilExclusive {
                if (charToTokenClass(it) == TC_OTHER) {
                    append(it.toChar())
                    return@readUntilExclusive true
                } else {
                    return@readUntilExclusive false
                }
            }
        }
    }

    public fun skipElement() {
        val tokenStack = mutableListOf<Byte>()
        do {
            when (val token = nextToken()) {
                TC_BEGIN_LIST, TC_BEGIN_OBJ -> {
                    tokenStack.add(token)
                    takeToken()
                }
                TC_END_LIST -> {
                    if (tokenStack.last() != TC_BEGIN_LIST) {
                        fail("found ] instead of }")
                    }
                    tokenStack.removeAt(tokenStack.size - 1)
                    takeToken()
                }
                TC_STRING -> skipString()
                TC_OTHER -> skipLiteral()
                TC_END_OBJ -> {
                    if (tokenStack.last() != TC_BEGIN_OBJ) {
                        fail("found } instead of ]")
                    }
                    tokenStack.removeAt(tokenStack.size - 1)
                    takeToken()
                }
                else -> takeToken()
            }
        } while (tokenStack.isNotEmpty())
    }

    private fun skipLiteral() {
        position += source.readUntilExclusive {
            charToTokenClass(it) == TC_OTHER
        }
    }

    private fun skipString() {
        var escape = false
        var unicodeEscape = false
        var unicodeSize = 4

        consume(TC_STRING) { "Expected '\"'" }

        position += source.decodeUtf8Chars {
            if (escape) {
                escape = false
                if (it == UNICODE_ESC) {
                    unicodeEscape = true
                    return@decodeUtf8Chars true
                }

                return@decodeUtf8Chars true
            }

            if (unicodeEscape) {
                unicodeSize -= 1

                if (unicodeSize == 0) {
                    unicodeEscape = false
                }

                return@decodeUtf8Chars true
            }

            if (it == STRING_ESC) {
                escape = true
                return@decodeUtf8Chars true
            }

            it != STRING
        }
    }

    public override fun toString(): String {
        return "JsonReader(source='$source', position='$position')"
    }

    public fun fail(message: String, position: Int = this.position): Nothing {
        throw JsonDecodingException(position, message)
    }

    private fun fromHexChar(char: Char): Int = when (char) {
        in '0'..'9' -> char.toInt() - '0'.toInt()
        in 'a'..'f' -> char.toInt() - 'a'.toInt() + 10
        in 'A'..'F' -> char.toInt() - 'A'.toInt() + 10
        else -> fail("Invalid toHexChar char '$char' in unicode escape")
    }

    private fun require(condition: Boolean, position: Int = this.position, message: () -> String) {
        if (!condition) {
            fail(message(), position)
        }
    }
}
