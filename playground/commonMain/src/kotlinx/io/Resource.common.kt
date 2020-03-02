package kotlinx.io

expect class Resource(filename: String) {
    val filename: String

    fun readText(): String
}