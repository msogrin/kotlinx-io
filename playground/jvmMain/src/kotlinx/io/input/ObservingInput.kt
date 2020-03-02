package kotlinx.io.input

import kotlinx.io.*
import kotlinx.io.buffer.*

class ObservingInput(
    private val source: Input,
    private val block: (Buffer, Int) -> Unit
): Input() {

    override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
        val count = source.readAvailableTo(buffer, startIndex, endIndex)
        block(buffer, count)
        return count
    }

    override fun closeSource() {
        source.close()
    }
}
