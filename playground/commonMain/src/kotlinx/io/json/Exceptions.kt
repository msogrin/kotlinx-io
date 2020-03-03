package kotlinx.io.json

import kotlinx.serialization.*
import kotlinx.serialization.json.*


fun ioInvalidFloatingPoint(value: Number, type: String) = JsonEncodingException(
    "$value is not a valid $type as per JSON specification. " +
            "You can disable strict mode to serialize such values"
)


fun ioInvalidFloatingPoint(value: Number, key: String, type: String) = JsonEncodingException(
    "$value with key $key is not a valid $type as per JSON specification. " +
            "You can disable strict mode to serialize such values"
)

fun ioJsonUnknownKeyException(position: Int, key: String) = JsonDecodingException(
    position,
    "Strict JSON encountered unknown key: $key\n" +
            "You can disable strict mode to skip unknown keys"
)


fun ioJsonMapInvalidKeyKind(keyDescriptor: SerialDescriptor) = kotlinx.serialization.json.JsonException(
    "Value of type ${keyDescriptor.serialName} can't be used in json as map key. " +
            "It should have either primitive or enum kind, but its kind is ${keyDescriptor.kind}.\n" +
            "You can convert such maps to arrays [key1, value1, key2, value2,...] with 'allowStructuredMapKeys' flag in JsonConfiguration."
)
