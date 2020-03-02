package kotlinx.io.json

import kotlinx.serialization.json.*


internal class ioJsonParser(private val reader: ioJsonReader) {

    fun read(): JsonElement = when (reader.nextToken()) {
        TC_OTHER -> readValue(isString = false)
        TC_STRING -> readValue(isString = true)
        TC_BEGIN_OBJ -> readObject()
        TC_BEGIN_LIST -> readArray()
        else -> reader.fail("Can't begin reading element, unexpected token")
    }

    private fun readObject(): JsonElement {
        val result = linkedMapOf<String, JsonElement>()
        reader.consume(TC_BEGIN_OBJ) { "Expected '{', but found '$it' " }

        var first = true
        while (true) {
            val token = reader.nextToken()
            if (token == TC_END_OBJ) {
                reader.takeToken()
                break
            }

            if (!first) {
                reader.consume(TC_COMMA) { "Expected ',', Actual '$it'" }
            } else {
                first = false
            }

            val key = reader.takeStringOrLiteral()
            if (key == "null") {
                reader.fail("Expected string or non-null literal", reader.position - 4)
            }

            reader.consume(TC_COLON) { "Expected ':', but found '$it' " }

            val value = read()
            result[key] = value
        }

        return JsonObject(result)
    }

    private fun readValue(isString: Boolean): JsonElement {
        val value = if (isString) {
            reader.takeString()
        } else {
            val literal = reader.takeLiteral()
            if (literal == NULL) {
                return JsonNull
            } else literal
        }

        @Suppress("INVISIBLE_MEMBER")
        return JsonLiteral(value, isString)
    }

    private fun readArray(): JsonElement {
        val result = arrayListOf<JsonElement>()

        reader.consume(TC_BEGIN_LIST) { "List expected" }
        if (reader.nextToken() == TC_END_LIST) {
            reader.takeToken()
            return JsonArray(result)
        }

        while (true) {
            result += read()

            val token = reader.takeToken()
            if (token == TC_END_LIST) {
                break
            }

            if (token != TC_COMMA) {
                reader.fail("\"Expected ','")
            }
        }

        return JsonArray(result)
    }
}

internal inline fun ioJsonReader.consume(expected: Byte, errorMessage: (Char) -> String) {
    val type = takeToken()
    if (type != expected) {
        fail(errorMessage(type.toChar()), position - 1)
    }
}
