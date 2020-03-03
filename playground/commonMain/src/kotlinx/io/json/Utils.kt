package kotlinx.io.json

import kotlinx.io.json.EscapeCharMappings.ESCAPE_2_CHAR
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.jvm.*
import kotlin.native.concurrent.*


// special strings
internal const val NULL = "null"

// special chars
internal const val COMMA = ','
internal const val COLON = ':'
internal const val BEGIN_OBJ = '{'
internal const val END_OBJ = '}'
internal const val BEGIN_LIST = '['
internal const val END_LIST = ']'
internal const val STRING = '"'
internal const val STRING_ESC = '\\'

internal const val STRING_BYTE = '"'.toByte()
internal const val STRING_ESC_BYTE = '\\'.toByte()

internal const val INVALID = 0.toChar()
internal const val UNICODE_ESC = 'u'

// token classes
internal const val TC_OTHER: Byte = 0
internal const val TC_STRING: Byte = 1
internal const val TC_STRING_ESC: Byte = 2
internal const val TC_WS: Byte = 3
internal const val TC_COMMA: Byte = 4
internal const val TC_COLON: Byte = 5
internal const val TC_BEGIN_OBJ: Byte = 6
internal const val TC_END_OBJ: Byte = 7
internal const val TC_BEGIN_LIST: Byte = 8
internal const val TC_END_LIST: Byte = 9
internal const val TC_NULL: Byte = 10
internal const val TC_INVALID: Byte = 11
internal const val TOKEN_EOF: Byte = 12

internal const val PRIMITIVE_TAG = "primitive" // also used in JsonPrimitiveInput

// mapping from chars to token classes
internal const val CTC_MAX = 0x7e

// mapping from escape chars real chars
internal const val ESC2C_MAX = 0x75

@SharedImmutable
internal val C2TC = ByteArray(CTC_MAX).apply {
    for (i in 0..0x20) {
        initC2TC(i, TC_INVALID)
    }

    initC2TC(0x09, TC_WS)
    initC2TC(0x0a, TC_WS)
    initC2TC(0x0d, TC_WS)
    initC2TC(0x20, TC_WS)
    initC2TC(COMMA, TC_COMMA)
    initC2TC(COLON, TC_COLON)
    initC2TC(BEGIN_OBJ, TC_BEGIN_OBJ)
    initC2TC(END_OBJ, TC_END_OBJ)
    initC2TC(BEGIN_LIST, TC_BEGIN_LIST)
    initC2TC(END_LIST, TC_END_LIST)
    initC2TC(STRING, TC_STRING)
    initC2TC(STRING_ESC, TC_STRING_ESC)
}

// object instead of @SharedImmutable because there is mutual initialization in [initC2ESC]
internal object EscapeCharMappings {
    @JvmField
    public val ESCAPE_2_CHAR = CharArray(ESC2C_MAX)

    init {
        for (i in 0x00..0x1f) {
            initC2ESC(i, UNICODE_ESC)
        }

        initC2ESC(0x08, 'b')
        initC2ESC(0x09, 't')
        initC2ESC(0x0a, 'n')
        initC2ESC(0x0c, 'f')
        initC2ESC(0x0d, 'r')
        initC2ESC('/', '/')
        initC2ESC(STRING, STRING)
        initC2ESC(STRING_ESC, STRING_ESC)
    }

    internal fun initC2ESC(c: Int, esc: Char) {
        if (esc != UNICODE_ESC) ESCAPE_2_CHAR[esc.toInt()] = c.toChar()
    }

    internal fun initC2ESC(c: Char, esc: Char) =
        initC2ESC(c.toInt(), esc)
}

internal fun ByteArray.initC2TC(c: Int, cl: Byte) {
    this[c] = cl
}

internal fun ByteArray.initC2TC(c: Char, cl: Byte) {
    initC2TC(c.toInt(), cl)
}

internal fun charToTokenClass(char: Byte): Byte = if (char.toInt() < CTC_MAX
) C2TC[char.toInt()] else TC_OTHER

internal fun charToTokenClass(char: Char): Byte = if (char.toInt() < CTC_MAX
) C2TC[char.toInt()] else TC_OTHER

internal fun escapeToChar(c: Int): Char = if (c < ESC2C_MAX
) ESCAPE_2_CHAR[c] else INVALID

internal fun rangeEquals(source: String, start: Int, length: Int, str: String): Boolean {
    val n = str.length
    if (length != n) return false
    for (i in 0 until n) if (source[start + i] != str[i]) return false
    return true
}

@Suppress("USELESS_CAST") // Contracts does not work in K/N
internal inline fun <reified T : JsonElement> cast(obj: JsonElement): T {
    check(obj is T) { "Expected ${T::class} but found ${obj::class}" }
    return obj as T
}

internal fun defer(deferred: () -> SerialDescriptor): SerialDescriptor = object : SerialDescriptor {
    private val original: SerialDescriptor by lazy(deferred)

    override val serialName: String
        get() = original.serialName
    override val kind: SerialKind
        get() = original.kind
    override val elementsCount: Int
        get() = original.elementsCount

    override fun getElementName(index: Int): String = original.getElementName(index)
    override fun getElementIndex(name: String): Int = original.getElementIndex(name)
    override fun getElementAnnotations(index: Int): List<Annotation> = original.getElementAnnotations(index)
    override fun getElementDescriptor(index: Int): SerialDescriptor = original.getElementDescriptor(index)
    override fun isElementOptional(index: Int): Boolean = original.isElementOptional(index)
}
