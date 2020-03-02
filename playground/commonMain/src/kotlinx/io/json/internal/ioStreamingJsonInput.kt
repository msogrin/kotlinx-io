/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.io.json.internal

import kotlinx.io.json.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.jvm.*

/**
 * [JsonInput] which reads given JSON from [JsonReader] field by field.
 */
internal class ioStreamingJsonInput internal constructor(
    public override val json: ioJson,
    private val mode: ioWriteMode,
    @JvmField internal val reader: ioJsonReader
) : ioJsonInput, ElementValueDecoder() {

    public override val context: SerialModule = json.context
    private var lastIndex = -1
    private val configuration = json.configuration

    public override fun decodeJson(): JsonElement = ioJsonParser(reader).read()

    @Suppress("DEPRECATION")
    override val updateMode: UpdateMode
        get() = configuration.updateMode

    @InternalSerializationApi
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        return decodeSerializableValuePolymorphic(deserializer)
    }

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        val newMode = json.switchMode(desc)
        if (newMode.begin != INVALID) {
            reader.consume(newMode.beginTc) { "Expected '${newMode.begin}, kind: ${desc.kind}'" }
            reader.nextToken()
        }
        return when (newMode) {
            ioWriteMode.LIST, ioWriteMode.MAP, ioWriteMode
                .POLY_OBJ -> ioStreamingJsonInput(
                json,
                newMode,
                reader
            ) // need fresh cur index
            else -> if (mode == newMode) this else
                ioStreamingJsonInput(json, newMode, reader) // todo: reuse instance per mode
        }
    }

    override fun endStructure(desc: SerialDescriptor) {
        if (mode.end != INVALID) {
            reader.consume(mode.endTc) { "Expected '${mode.end}'" }
            reader.nextToken()
        }
    }

    override fun decodeNotNullMark(): Boolean =
        (reader.nextLiteral() != "null")

    override fun decodeNull(): Nothing? {
        val nullLiteral = reader.takeLiteral()
        if (nullLiteral != "null") {
            reader.fail("Expected null literal")
        }
        return null
    }

    override fun decodeElementIndex(desc: SerialDescriptor): Int {
        return when (mode) {
            ioWriteMode.LIST -> decodeListIndex()
            ioWriteMode.MAP -> decodeMapIndex()
            ioWriteMode.POLY_OBJ -> {
                when (++lastIndex) {
                    0 -> 0
                    1 -> 1
                    else -> {
                        CompositeDecoder.READ_DONE
                    }
                }
            }
            else -> decodeObjectIndex(desc)
        }
    }

    private fun decodeMapIndex(): Int {
        if (lastIndex % 2 == 0) {
            reader.consume(TC_COLON) { "Expected ':'" }
        }

        val token = reader.nextToken()
        if (lastIndex % 2 != 0) {
            // Read key or }
            if (token == TC_END_OBJ) {
                return CompositeDecoder.READ_DONE
            }

            if (lastIndex >= 0) {
                reader.consume(TC_COMMA) { "Expected ','" }
            }
        } else {
            // Read value
            if (token == TC_END_OBJ) {
                reader.fail("Object value expected, but got '}'")
            }
        }

        return ++lastIndex
    }

    private fun decodeObjectIndex(descriptor: SerialDescriptor): Int {
        while (true) {
            when (reader.nextToken()) {
                TC_END_OBJ -> return CompositeDecoder.READ_DONE
                TC_COMMA -> {
                    if (lastIndex == -1) {
                        reader.fail("Unexpected ','")
                    }
                    reader.takeToken()
                }
            }

            ++lastIndex

            val key = reader.takeStringOrLiteral()
            reader.consume(TC_COLON) { "Expected ':', actual '${it.toInt()}'" }

            val index = descriptor.getElementIndex(key)
            if (index != CompositeDecoder.UNKNOWN_NAME) {
                return index
            }

            if (configuration.strictMode) {
                reader.fail("Encountered an unknown key '$key'")
            } else {
                reader.skipElement()
            }
        }
    }

    private fun decodeListIndex(): Int {
        when (reader.nextToken()) {
            TC_COMMA -> {
                if (lastIndex < 0) {
                    reader.fail("Unexpected leading ','")
                }

                reader.takeToken()
            }
            TC_END_LIST -> {
                return CompositeDecoder.READ_DONE
            }
            else -> {
                if (lastIndex >= 0) {
                    reader.fail("Expected ',' or '}'")
                }
            }
        }

        lastIndex++
        return lastIndex
    }

    override fun decodeBoolean(): Boolean =
        reader.takeLiteral().run { if (configuration.strictMode) toBooleanStrict() else toBoolean() }

    override fun decodeByte(): Byte = reader.takeLiteral().toByte()
    override fun decodeShort(): Short = reader.takeLiteral().toShort()
    override fun decodeInt(): Int = reader.takeLiteral().toInt()
    override fun decodeLong(): Long = reader.takeStringOrLiteral().toLong()
    override fun decodeFloat(): Float = reader.takeLiteral().toFloat()
    override fun decodeDouble(): Double = reader.takeLiteral().toDouble()
    override fun decodeChar(): Char = reader.takeLiteral().single()
    override fun decodeString(): String = reader.takeStringOrLiteral()

    override fun decodeEnum(enumDescription: SerialDescriptor): Int =
        enumDescription.getElementIndexOrThrow(reader.takeString())
}
