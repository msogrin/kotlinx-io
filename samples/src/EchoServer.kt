import kotlinx.io.*
import kotlinx.io.buffer.*
import kotlinx.io.compat.*
import kotlinx.io.input.*
import java.io.*
import java.net.*
import kotlin.io.use

fun main() {
    val server = ServerSocket(8080)

    println("Listening on: ${server.localPort}")

    while (true) {
        server.accept().use {
            println("Connected.")

            val logger = ObservingInput(it.input) { buffer, count ->
                if (count == 0) {
                    println("Connection closed.")
                } else {
                    val log = buffer.toByteArray(0, count).toString(Charsets.UTF_8)
                    println(log)
                }
            }

            it.output.use {
                logger.copyTo(it)
            }
        }
    }
}
