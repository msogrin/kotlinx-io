/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.io.json

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*

/**
 * External [Serializer] object providing [SerializationStrategy] and [DeserializationStrategy] for [JsonElement].
 * It can only be used by with [Json] format an its input ([JsonInput] and [JsonOutput]).
 * Currently, this hierarchy has no guarantees on descriptor content.
 *
 * Example usage:
 * ```
 *   val string = Json.stringify(JsonElementSerializer, json { "key" to 1.0 })
 *   val literal = Json.parse(JsonElementSerializer, string)
 *   assertEquals(JsonObject(mapOf("key" to JsonLiteral(1.0))), literal)
 * ```
 */
@Serializer(forClass = JsonElement::class)
public object ioJsonElementSerializer : KSerializer<JsonElement> {
    override val descriptor: SerialDescriptor =
        SerialDescriptor("kotlinx.serialization.json.JsonElement", PolymorphicKind.SEALED) {
            // Resolve cyclic dependency in descriptors by late binding
            element("JsonPrimitive", defer { JsonPrimitiveSerializer.descriptor })
            element("JsonNull", defer { JsonNullSerializer.descriptor })
            element("JsonLiteral", defer { JsonLiteralSerializer.descriptor })
            element("JsonObject", defer { JsonObjectSerializer.descriptor })
            element("JsonArray", defer { JsonArraySerializer.descriptor })
        }

    override fun serialize(encoder: Encoder, obj: JsonElement) {
        verify(encoder)
        when (obj) {
            is JsonPrimitive -> encoder.encodeSerializableValue(
                ioJsonPrimitiveSerializer, obj
            )
            is JsonObject -> encoder.encodeSerializableValue(ioJsonObjectSerializer, obj)
            is JsonArray -> encoder.encodeSerializableValue(ioJsonArraySerializer, obj)
        }
    }

    override fun deserialize(decoder: Decoder): JsonElement {
        verify(decoder)
        val input = decoder as ioJsonInput
        return input.decodeJson()
    }
}

/**
 * External [Serializer] object providing [SerializationStrategy] and [DeserializationStrategy] for [JsonPrimitive].
 * It can only be used by with [Json] format an its input ([JsonInput] and [JsonOutput]).
 */
@Serializer(forClass = JsonPrimitive::class)
public object ioJsonPrimitiveSerializer : KSerializer<JsonPrimitive> {
    override val descriptor: SerialDescriptor =
        SerialDescriptor("kotlinx.serialization.json.JsonPrimitive", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, obj: JsonPrimitive) {
        verify(encoder)
        return if (obj is JsonNull) {
            encoder.encodeSerializableValue(ioJsonNullSerializer, JsonNull)
        } else {
            encoder.encodeSerializableValue(ioJsonLiteralSerializer, obj as JsonLiteral)
        }
    }

    override fun deserialize(decoder: Decoder): JsonPrimitive {
        verify(decoder)
        return if (decoder.decodeNotNullMark()) JsonPrimitive(decoder.decodeString())
        else decoder.decodeSerializableValue(ioJsonNullSerializer)
    }
}

/**
 * External [Serializer] object providing [SerializationStrategy] and [DeserializationStrategy] for [JsonNull].
 * It can only be used by with [Json] format an its input ([JsonInput] and [JsonOutput]).
 */
@Serializer(forClass = JsonNull::class)
public object ioJsonNullSerializer : KSerializer<JsonNull> {
    override fun serialize(encoder: Encoder, obj: JsonNull) {
        verify(encoder)
        encoder.encodeNull()
    }

    override fun deserialize(decoder: Decoder): JsonNull {
        verify(decoder)
        decoder.decodeNull()
        return JsonNull
    }

    override val descriptor: SerialDescriptor =
        SerialDescriptor("kotlinx.serialization.json.JsonNull", UnionKind.ENUM_KIND)
}

/**
 * External [Serializer] object providing [SerializationStrategy] and [DeserializationStrategy] for [JsonLiteral].
 * It can only be used by with [Json] format an its input ([JsonInput] and [JsonOutput]).
 */
@Serializer(forClass = JsonLiteral::class)
public object ioJsonLiteralSerializer : KSerializer<JsonLiteral> {

    override val descriptor: SerialDescriptor =
        PrimitiveDescriptor("kotlinx.serialization.json.JsonLiteral", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, obj: JsonLiteral) {
        verify(encoder)
        if (obj.isString) {
            return encoder.encodeString(obj.content)
        }

        val long = obj.longOrNull
        if (long != null) {
            return encoder.encodeLong(long)
        }

        val double = obj.doubleOrNull
        if (double != null) {
            return encoder.encodeDouble(double)
        }

        val boolean = obj.booleanOrNull
        if (boolean != null) {
            return encoder.encodeBoolean(boolean)
        }

        encoder.encodeString(obj.content)
    }

    override fun deserialize(decoder: Decoder): JsonLiteral {
        verify(decoder)
        return JsonLiteral(decoder.decodeString())
    }
}

/**
 * External [Serializer] object providing [SerializationStrategy] and [DeserializationStrategy] for [JsonObject].
 * It can only be used by with [Json] format an its input ([JsonInput] and [JsonOutput]).
 */
@Serializer(forClass = JsonObject::class)
public object ioJsonObjectSerializer : KSerializer<JsonObject> {
    override val descriptor: SerialDescriptor =
        NamedMapClassDescriptor(
            "JsonObject", StringSerializer.descriptor,
            ioJsonElementSerializer.descriptor
        )

    override fun serialize(encoder: Encoder, obj: JsonObject) {
        verify(encoder)
        LinkedHashMapSerializer(StringSerializer, ioJsonElementSerializer).serialize(encoder, obj.content)
    }

    override fun deserialize(decoder: Decoder): JsonObject {
        verify(decoder)
        return JsonObject(
            LinkedHashMapSerializer(
                StringSerializer,
                ioJsonElementSerializer
            ).deserialize(decoder)
        )
    }
}

/**
 * External [Serializer] object providing [SerializationStrategy] and [DeserializationStrategy] for [JsonArray].
 * It can only be used by with [Json] format an its input ([JsonInput] and [JsonOutput]).
 */
@Serializer(forClass = JsonArray::class)
public object ioJsonArraySerializer : KSerializer<JsonArray> {

    override val descriptor: SerialDescriptor = NamedListClassDescriptor(
        "JsonArray",
        ioJsonElementSerializer.descriptor
    )

    override fun serialize(encoder: Encoder, obj: JsonArray) {
        verify(encoder)
        ArrayListSerializer(ioJsonElementSerializer).serialize(encoder, obj)
    }

    override fun deserialize(decoder: Decoder): JsonArray {
        verify(decoder)
        return JsonArray(ArrayListSerializer(ioJsonElementSerializer).deserialize(decoder))
    }
}

private fun verify(encoder: Encoder) {
    if (encoder !is ioJsonOutput) error("Json element serializer can be used only by Json format")
}

private fun verify(decoder: Decoder) {
    if (decoder !is ioJsonInput) error("Json element serializer can be used only by Json format")
}
