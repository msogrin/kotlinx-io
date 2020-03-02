package kotlinx.io.json.utils

import com.google.gson.*
import kotlinx.io.*
import kotlinx.io.json.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.*
import java.io.InputStream
import java.io.OutputStream
import kotlin.reflect.*

interface JsonAdapter {
    fun <T> parse(content: String, type: KType, clazz: Class<T>): T
    fun <T> parse(content: InputStream, type: KType, clazz: Class<T>): T
    fun <T> parse(content: Input, type: KType, clazz: Class<T>): T
    fun <T> encode(data: T, type: KType): String
    fun <T> encode(data: T, type: KType, outputStream: OutputStream)
    fun <T> encode(data: T, type: KType, output: Output)
}

@UseExperimental(ImplicitReflectionSerializer::class)
object KxJson : JsonAdapter {
    override fun <T> parse(content: String, type: KType, clazz: Class<T>): T =
        ioJson.parse(serializer(type), content) as T

    override fun <T> parse(content: InputStream, type: KType, clazz: Class<T>): T = error("unsupported")
    override fun <T> parse(content: Input, type: KType, clazz: Class<T>): T =
        ioJson.parse(serializer(type), content) as T

    override fun <T> encode(data: T, type: KType): String =
        ioJson.stringify(serializer(type), data)

    override fun <T> encode(data: T, type: KType, outputStream: OutputStream) = error("unsupported")
    override fun <T> encode(data: T, type: KType, output: Output) {
        ioJson.stringify(serializer(type), data, output)
    }
}

@UseExperimental(ImplicitReflectionSerializer::class)
object SerializationJson : JsonAdapter {
    override fun <T> parse(content: String, type: KType, clazz: Class<T>): T =
        Json.parse(serializer(type), content) as T

    override fun <T> parse(content: InputStream, type: KType, clazz: Class<T>): T = error("unsuppored")
    override fun <T> parse(content: Input, type: KType, clazz: Class<T>): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T> encode(data: T, type: KType): String =
        Json.stringify(serializer(type), data)

    override fun <T> encode(data: T, type: KType, outputStream: OutputStream) = error("unsupported")
    override fun <T> encode(data: T, type: KType, output: Output) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}


object GsonJson : JsonAdapter {
    private val gson = Gson()

    override fun <T> parse(content: String, type: KType, clazz: Class<T>): T = gson.fromJson(content, clazz)

    override fun <T> parse(content: InputStream, type: KType, clazz: Class<T>): T =
        gson.fromJson(content.bufferedReader(), clazz)

    override fun <T> parse(content: Input, type: KType, clazz: Class<T>): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T> encode(data: T, type: KType): String = gson.toJson(data)

    override fun <T> encode(data: T, type: KType, outputStream: OutputStream) {
        gson.toJson(data, outputStream.bufferedWriter())
    }

    override fun <T> encode(data: T, type: KType, output: Output) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

//
//object JacksonJson : JsonAdapter {
//    private val jackson = jacksonObjectMapper()
//
//    override fun <T> parse(content: String, type: KType, clazz: Class<T>): T = jackson.readValue(content, clazz)
//
//    override fun <T> encode(data: T, type: KType): String = jackson
//        .writeValueAsString(data)
//}
