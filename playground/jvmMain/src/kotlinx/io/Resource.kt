package kotlinx.io

actual class Resource actual constructor(
    actual val filename: String
) {
    actual fun readText(): String {
        return javaClass.getResource("/$filename").readText()
    }
}