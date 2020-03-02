/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.io.json.internal

import kotlinx.io.*
import kotlinx.io.json.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

internal class ioStreamingJsonOutput(
    private val composer: Composer,
    override val json: ioJson,
    private val mode: ioWriteMode,
    private val modeReuseCache: Array<ioJsonOutput?>
) : ioJsonOutput, ElementValueEncoder() {

    internal constructor(
        output: Output, json: ioJson, mode: ioWriteMode,
        modeReuseCache: Array<ioJsonOutput?>
    ) : this(Composer(output, json), json, mode, modeReuseCache)

    public override val context: SerialModule = json.context
    private val configuration = json.configuration

    // Forces serializer to wrap all values into quotes
    private var forceQuoting: Boolean = false
    private var writePolymorphic = false

    init {
        val i = mode.ordinal
        if (modeReuseCache[i] !== null || modeReuseCache[i] !== this)
            modeReuseCache[i] = this
    }

    @InternalSerializationApi
    override fun encodeJson(element: JsonElement) {
        encodeSerializableValue(ioJsonElementSerializer, element)
    }

    override fun shouldEncodeElementDefault(desc: SerialDescriptor, index: Int): Boolean {
        return configuration.encodeDefaults
    }

    @InternalSerializationApi
    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        encodePolymorphically(serializer, value) {
            writePolymorphic = true
        }
    }

    private fun encodeTypeInfo(descriptor: SerialDescriptor) {
        composer.nextItem()
        encodeString(configuration.classDiscriminator)
        composer.print(COLON)
        composer.space()
        encodeString(descriptor.name)
    }

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
        val newMode = json.switchMode(desc)
        if (newMode.begin != INVALID) { // entry
            composer.print(newMode.begin)
            composer.indent()
        }

        if (writePolymorphic) {
            writePolymorphic = false
            encodeTypeInfo(desc)
        }

        if (mode == newMode) {
            return this
        }

        return modeReuseCache[newMode.ordinal] ?: ioStreamingJsonOutput(composer, json, newMode, modeReuseCache)
    }

    override fun endStructure(desc: SerialDescriptor) {
        if (mode.end != INVALID) {
            composer.unIndent()
            composer.nextItem()
            composer.print(mode.end)
        }
    }

    override fun encodeElement(desc: SerialDescriptor, index: Int): Boolean {
        when (mode) {
            ioWriteMode.LIST -> {
                if (!composer.writingFirst)
                    composer.print(COMMA)
                composer.nextItem()
            }
            ioWriteMode.MAP -> {
                if (!composer.writingFirst) {
                    forceQuoting = if (index % 2 == 0) {
                        composer.print(COMMA)
                        composer.nextItem() // indent should only be put after commas in map
                        true
                    } else {
                        composer.print(COLON)
                        composer.space()
                        false
                    }
                } else {
                    forceQuoting = true
                    composer.nextItem()
                }
            }
            ioWriteMode.POLY_OBJ -> {
                if (index == 0)
                    forceQuoting = true
                if (index == 1) {
                    composer.print(COMMA)
                    composer.space()
                    forceQuoting = false
                }
            }
            else -> {
                if (!composer.writingFirst)
                    composer.print(COMMA)
                composer.nextItem()
                encodeString(desc.getElementName(index))
                composer.print(COLON)
                composer.space()
            }
        }
        return true
    }

    override fun encodeNull() {
        composer.print(NULL)
    }

    override fun encodeBoolean(value: Boolean) {
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeByte(value: Byte) {
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeShort(value: Short) {
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeInt(value: Int) {
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeLong(value: Long) {
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeFloat(value: Float) {
        if (configuration.strictMode && !value.isFinite()) {
            throw ioInvalidFloatingPoint(value, "float")
        }

        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeDouble(value: Double) {
        if (configuration.strictMode && !value.isFinite()) {
            throw ioInvalidFloatingPoint(value, "double")
        }

        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeChar(value: Char) {
        encodeString(value.toString())
    }

    override fun encodeString(value: String) {
        if (configuration.unquoted && !shouldBeQuoted(value)) {
            composer.print(value)
        } else {
            composer.printQuoted(value)
        }
    }

    override fun encodeEnum(enumDescription: SerialDescriptor, ordinal: Int) {
        encodeString(enumDescription.getElementName(ordinal))
    }

    override fun encodeValue(value: Any) {
        if (configuration.strictMode) super.encodeValue(value) else
            encodeString(value.toString())
    }
}
