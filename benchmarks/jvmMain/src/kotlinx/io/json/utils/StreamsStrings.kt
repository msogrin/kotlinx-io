package kotlinx.io.json.utils

import kotlinx.io.*
import java.io.*
import java.io.ByteArrayInputStream
import java.io.InputStream

fun String.toInputStream(): InputStream {
    val bytes = toByteArray()
    return ByteArrayInputStream(bytes)
}

fun String.toInput(): Input {
    val bytes = toByteArray()
    return ByteArrayInput(bytes)
}