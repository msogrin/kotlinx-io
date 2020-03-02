import kotlinx.io.*
import kotlinx.io.text.*
import java.lang.StringBuilder

class HttpParser {
}

class Request

private const val HTTP_LINE_LIMIT = 8192

suspend fun parseRequest(input: Input): Request {
    val builder = StringBuilder()
    var start = 0
    var end = 0

    while (true) {
        input.readUtf8LineTo(builder, HTTP_LINE_LIMIT)
        end = builder.length

        if (start == end) continue

//        start = skipSpaces(builder, start, end)
//        val method = parseHttpMethod(builder, start, end)
//        val uri = parseUri(builder, start, end)
//        val version = parseVersion(builder, range)
//        start = skipSpaces(builder, start, end)
//
//        if (start != end) error("Extra characters in request line: ${builder.substring(start, end)}")
//        if (uri.isEmpty()) error("URI is not specified")
//        if (version.isEmpty()) error("HTTP version is not specified")
//
//        val headers = parseHeaders(input, builder, range)
//
//        return Request(method, uri, version, headers, builder)
        TODO()
    }
}

private fun parseHttpMethod(text: CharSequence, start: Int, end: Int): String {
//    val exact = DefaultHttpMethods.search(text, start, end) { ch, _ -> ch == ' ' }.singleOrNull()
//    if (exact != null) {
//        range.start += exact.value.length
//        return exact
//    }
//
//    return parseHttpMethodFull(text, range)
    TODO()
}

internal fun skipSpaces(text: StringBuilder, start: Int, end: Int): Int {
    if (start >= end || text[start] != ' ')  {
        return start
    }

    var index = start
    index++

    while (index < end) {
        if (text[index] != ' ') break
        index++
    }

    return index
}