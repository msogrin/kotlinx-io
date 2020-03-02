package kotlinx.io.compat

import kotlinx.io.*
import kotlinx.io.buffer.*
import java.io.*


public fun OutputStream.writeBuffer(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
    val container = buffer.buffer
    val length = endIndex - startIndex
    if (container.hasArray()) {
        write(container.array(), container.arrayOffset() + startIndex, length)
    } else {
        val content = buffer.toByteArray(startIndex, length)
        write(content)
    }

    return length
}

public fun OutputStream.toOutput(): Output = object : Output() {
    override fun flush(source: Buffer, startIndex: Int, endIndex: Int) {
        writeBuffer(source, startIndex, endIndex)
    }

    override fun closeSource() {
        this@toOutput.close()
    }
}

public fun InputStream.readTo(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
    val container = buffer.buffer
    val size = endIndex - startIndex
    return if (container.hasArray()) {
        val count = read(container.array(), container.arrayOffset() + startIndex, size)
        if (count == -1) 0 else count
    } else {
        val array = ByteArray(size)
        val count = read(array)

        return if (count == -1) {
            0
        } else {
            buffer.storeByteArray(startIndex, array)
            count
        }
    }
}

public fun InputStream.toInput(): Input = object : Input() {
    override fun closeSource() {
        this@toInput.close()
    }

    override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int = readTo(buffer, startIndex, endIndex)
}
