/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.io.json.internal

import kotlinx.io.json.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.collections.set
import kotlin.jvm.*

internal fun <T> ioJson.writeJson(value: T, serializer: SerializationStrategy<T>): JsonElement {
    lateinit var result: JsonElement
    val encoder = ioJsonTreeOutput(this) { result = it }
    encoder.encode(serializer, value)
    return result
}

private sealed class ioAbstractJsonTreeOutput(
    final override val json: ioJson,
    val nodeConsumer: (JsonElement) -> Unit
) : NamedValueEncoder(), ioJsonOutput {

    final override val context: SerialModule
        get() = json.context

    @JvmField
    protected val configuration = json.configuration

    private var writePolymorphic = false

    override fun encodeJson(element: JsonElement) {
        encodeSerializableValue(ioJsonElementSerializer, element)
    }

    override fun shouldEncodeElementDefault(desc: SerialDescriptor, index: Int): Boolean = configuration.encodeDefaults
    override fun composeName(parentName: String, childName: String): String = childName
    abstract fun putElement(key: String, element: JsonElement)
    abstract fun getCurrent(): JsonElement

    override fun encodeTaggedNull(tag: String) = putElement(tag, JsonNull)

    override fun encodeTaggedInt(tag: String, value: Int) = putElement(tag, JsonLiteral(value))
    override fun encodeTaggedByte(tag: String, value: Byte) = putElement(tag, JsonLiteral(value))
    override fun encodeTaggedShort(tag: String, value: Short) = putElement(tag, JsonLiteral(value))
    override fun encodeTaggedLong(tag: String, value: Long) = putElement(tag, JsonLiteral(value))

    override fun encodeTaggedFloat(tag: String, value: Float) {
        if (configuration.strictMode && !value.isFinite()) {
            throw ioInvalidFloatingPoint(value, tag, "float")
        }

        putElement(tag, JsonLiteral(value))
    }

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        // Writing non-structured data (i.e. primitives) on top-level (e.g. without any tag) requires special output
        if (currentTagOrNull != null || serializer.descriptor.kind !is PrimitiveKind && serializer.descriptor
                .kind !== UnionKind.ENUM_KIND
        ) {
            encodePolymorphically(serializer, value) { writePolymorphic = true }
        } else ioJsonPrimitiveOutput(json, nodeConsumer).apply {
            encodeSerializableValue(serializer, value)
            endEncode(serializer.descriptor)
        }
    }

    override fun encodeTaggedDouble(tag: String, value: Double) {
        if (configuration.strictMode && !value.isFinite()) {
            throw ioInvalidFloatingPoint(value, tag, "double")
        }

        putElement(tag, JsonLiteral(value))
    }

    override fun encodeTaggedBoolean(tag: String, value: Boolean) = putElement(tag, JsonLiteral(value))
    override fun encodeTaggedChar(tag: String, value: Char) = putElement(tag, JsonLiteral(value.toString()))
    override fun encodeTaggedString(tag: String, value: String) = putElement(tag, JsonLiteral(value))
    override fun encodeTaggedEnum(
        tag: String,
        enumDescription: SerialDescriptor,
        ordinal: Int
    ) = putElement(tag, JsonLiteral(enumDescription.getElementName(ordinal)))

    override fun encodeTaggedValue(tag: String, value: Any) {
        putElement(tag, JsonLiteral(value.toString()))
    }

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
        val consumer =
            if (currentTagOrNull == null) nodeConsumer
            else { node -> putElement(currentTag, node) }

        val encoder = when (desc.kind) {
            StructureKind.LIST, is PolymorphicKind -> ioJsonTreeListOutput(json, consumer)
            StructureKind.MAP -> json.selectMapMode(
                desc,
                { ioJsonTreeMapOutput(json, consumer) },
                { ioJsonTreeListOutput(json, consumer) }
            )
            else -> ioJsonTreeOutput(json, consumer)
        }

        if (writePolymorphic) {
            writePolymorphic = false
            encoder.putElement(configuration.classDiscriminator, JsonPrimitive(desc.serialName))
        }

        return encoder
    }

    override fun endEncode(desc: SerialDescriptor) {
        nodeConsumer(getCurrent())
    }
}

internal const val PRIMITIVE_TAG = "primitive" // also used in JsonPrimitiveInput

private class ioJsonPrimitiveOutput(json: ioJson, nodeConsumer: (JsonElement) -> Unit) :
    ioAbstractJsonTreeOutput(json, nodeConsumer) {
    private var content: JsonElement? = null

    init {
        pushTag(PRIMITIVE_TAG)
    }

    override fun putElement(key: String, element: JsonElement) {
        require(
            key === PRIMITIVE_TAG
        ) { "This output can only consume primitives with '$PRIMITIVE_TAG' tag" }
        require(content == null) { "Primitive element was already recorded. Does call to .encodeXxx happen more than once?" }
        content = element
    }

    override fun getCurrent(): JsonElement =
        requireNotNull(content) { "Primitive element has not been recorded. Is call to .encodeXxx is missing in serializer?" }
}

private open class ioJsonTreeOutput(json: ioJson, nodeConsumer: (JsonElement) -> Unit) :
    ioAbstractJsonTreeOutput(json, nodeConsumer) {

    protected val content: MutableMap<String, JsonElement> = linkedMapOf()

    override fun putElement(key: String, element: JsonElement) {
        content[key] = element
    }

    override fun getCurrent(): JsonElement = JsonObject(content)
}

private class ioJsonTreeMapOutput(json: ioJson, nodeConsumer: (JsonElement) -> Unit) :
    ioJsonTreeOutput(json, nodeConsumer) {
    private lateinit var tag: String

    override fun putElement(key: String, element: JsonElement) {
        val idx = key.toInt()
        if (idx % 2 == 0) { // writing key
            tag = when (element) {
                is JsonPrimitive -> element.content
                is JsonObject -> throw ioJsonMapInvalidKeyKind(ioJsonObjectSerializer.descriptor)
                is JsonArray -> throw ioJsonMapInvalidKeyKind(ioJsonArraySerializer.descriptor)
            }
        } else {
            content[tag] = element
        }
    }

    override fun getCurrent(): JsonElement {
        return JsonObject(content)
    }

    override fun shouldWriteElement(desc: SerialDescriptor, tag: String, index: Int): Boolean = true
}

private class ioJsonTreeListOutput(json: ioJson, nodeConsumer: (JsonElement) -> Unit) :
    ioAbstractJsonTreeOutput(json, nodeConsumer) {
    private val array: ArrayList<JsonElement> = arrayListOf()
    override fun elementName(desc: SerialDescriptor, index: Int): String = index.toString()

    override fun shouldWriteElement(desc: SerialDescriptor, tag: String, index: Int): Boolean = true

    override fun putElement(key: String, element: JsonElement) {
        val idx = key.toInt()
        array.add(idx, element)
    }

    override fun getCurrent(): JsonElement = JsonArray(array)
}

@Suppress("USELESS_CAST") // Contracts does not work in K/N
internal inline fun <reified T : JsonElement> cast(obj: JsonElement): T {
    check(obj is T) { "Expected ${T::class} but found ${obj::class}" }
    return obj as T
}
