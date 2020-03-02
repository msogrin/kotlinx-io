import kotlinx.io.*
import kotlinx.io.buffer.*
import java.nio.channels.*

class NioChannelOutput(
    private val channel: WritableByteChannel
) : Output() {
    override fun flush(source: Buffer, startIndex: Int, endIndex: Int) {
        with(source.buffer) {
            position(startIndex)
            limit(endIndex)

            while (hasRemaining()) {
                channel.write(this)
            }
        }
    }

    override fun closeSource() {
        channel.close()
    }
}

class NioChannelInput(
    private val channel: ReadableByteChannel
) : Input() {
    override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int = with(buffer.buffer) {
        position(startIndex)
        limit(endIndex)
        channel.read(this)
    }

    override fun closeSource() {
        channel.close()
    }
}