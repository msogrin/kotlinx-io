@file:Suppress("FORBIDDEN_IDENTITY_EQUALS")

package kotlinx.io

import kotlinx.io.buffer.*
import kotlinx.io.bytes.*
import kotlinx.io.pool.*
import kotlin.test.*

class InputOutputTest {

    val EmptyInput = object : Input() {
        override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
            return 0
        }

        override fun closeSource() {
        }
    }

    val error = IllegalStateException("Custom fill error")

    val ErrorInput = object : Input() {
        override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
            throw error
        }

        override fun closeSource() {
        }
    }

    @Test
    fun testReadAvailableToWithSameBuffer() {
        var instance: Buffer = Buffer.EMPTY
        var result: Buffer = Buffer.EMPTY

        val input: Input = LambdaInput { buffer, start, end ->
            instance = buffer
            return@LambdaInput 42
        }

        val output = LambdaOutput { source, startIndex, endIndex ->
            result = source
            assertEquals(42, endIndex)
        }

        input.readAvailableTo(output)
        output.flush()

        assertNotNull(instance)
        assertTrue(instance === result)
    }

    @Test
    fun testCopyToOutput() {
        val source = ByteArray(4 * 1024 + 1) { it.toByte() }
        val input = buildInput {
            writeByteArray(source)
        }

        val output = ByteArrayOutput()
        input.copyTo(output)

        val result = output.toByteArray()
        assertArrayEquals(source, result)
    }

    @Test
    fun testCopyToWithSize() {
        val source = ByteArray(4 * 1024 + 1) { it.toByte() }
        val input = buildInput {
            writeByteArray(source)
        }

        val output = ByteArrayOutput()
        input.copyTo(output, 4 * 1024)

        val lastByte = input.readByte()
        val result = output.toByteArray()

        assertTrue(input.eof())
        assertArrayEquals(source, result + lastByte)
    }

    @Test
    fun testReadByteArray() {
        val source = ByteArray(4 * 1024 + 1) { it.toByte() }

        val input = buildInput {
            writeByteArray(source)
        }

        val result = input.readByteArray()
        assertArrayEquals(source, result)
    }

    @Test
    fun testCopyAvailableToOnEmpty() {
        val input = buildInput { }
        val output = ByteArrayOutput()

        input.readAvailableTo(output)

        assertTrue(input.eof())
        assertArrayEquals(ByteArray(0), output.toByteArray())
    }

    @Test
    fun testCopyAvailableToInPreview() {
        val size = 2048 * 3 + 42
        val array = ByteArray(size)
        val input = buildInput { writeByteArray(array) }
        val output = ByteArrayOutput()

        input.preview {
            copyTo(output)
            assertTrue(eof())
        }

        assertTrue(!input.eof())
        assertArrayEquals(array, output.toByteArray())
    }

    @Test
    fun testCloseAfterPreview() {
        val size = 2048 * 3 + 42
        val array = ByteArray(size)
        val input = buildInput { writeByteArray(array) }
        val output = ByteArrayOutput()

        input.preview {
            copyTo(output)
            assertTrue(eof())
        }

        input.close()
        assertFails {
            input.readByte()
        }
    }

    @Test
    fun testCloseInPreview() {
        val size = 2048 * 3 + 42
        val array = ByteArray(size)
        val input = buildInput { writeByteArray(array) }
        val output = ByteArrayOutput()

        input.preview {
            close()
            assertFails { readByte() }

            assertEquals(0, copyTo(output))
            assertTrue(eof())

            assertFails { preview { } }
        }

        assertFails { input.readByte() }
        assertEquals(0, input.copyTo(output))

        input.close()

        assertFails { input.readByte() }
        assertEquals(0, input.copyTo(output))
    }

    @Test
    fun testCustomPools() {
        val inputBuffer = bufferOf(ByteArray(10))
        val inputPool = SingleShotPool(inputBuffer)
        val outputBuffer = bufferOf(ByteArray(10))
        val outputPool = SingleShotPool(outputBuffer)

        val input = object : Input(inputPool) {
            override fun closeSource() {
            }

            override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
                assertTrue { outputBuffer === buffer }
                buffer.storeByteAt(startIndex, 42)
                return 1
            }
        }


        val output = object : Output(outputPool) {
            override fun flush(source: Buffer, startIndex: Int, endIndex: Int) {
                assertTrue(source === outputBuffer)
                assertTrue(endIndex == 1)

            }

            override fun closeSource() {
            }

        }

        input.readAvailableTo(output)
        output.flush()
    }

    @Test
    fun testFillDirect() {
        val myBuffer = bufferOf(ByteArray(1024))
        val input = object : Input() {
            override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
                assertTrue { myBuffer === buffer }
                buffer[startIndex] = 42
                return 1
            }

            override fun closeSource() {

            }
        }

        assertEquals(1, input.readAvailableTo(myBuffer))
    }

    @Test
    fun testDiscardOnEmpty() {
        assertFails {
            EmptyInput.discard(1)
        }
    }

    @Test
    fun testPrefetchOnEmpty() {
        assertFalse { EmptyInput.prefetch(1) }
    }

    @Test
    fun testPreviewOnEmpty() {
        assertFails { EmptyInput.preview { } }
    }

    @Test
    fun testBypassFillExceptions() {
        checkException { ErrorInput.readByte() }
        checkException { ErrorInput.preview { } }
        checkException { ErrorInput.prefetch(1) }
        checkException { ErrorInput.discard(1) }
        checkException { ErrorInput.eof() }
        checkException {
            ErrorInput.readAvailableTo(
                object : Output() {
                    override fun flush(source: Buffer, startIndex: Int, endIndex: Int) {
                        error("flush")
                    }

                    override fun closeSource() {
                        error("close")
                    }
                }
            )
        }

        checkException { ErrorInput.readAvailableTo(bufferOf(ByteArray(10))) }

        ErrorInput.close()
    }

    @Test
    fun testWriteDirectAfterSmallWrites() {
        val input = buildInput {
            writeByte(42)
            writeBuffer(bufferOf(ByteArray(4097)))
        }

        assertEquals(42, input.readByte())
        assertArrayEquals(ByteArray(4097), input.readByteArray())
    }

    @Test
    fun testInputCopyTo() {
        val content = ByteArray(1024) { it.toByte() }
        val input = ByteArrayInput(content)
        val output = ByteArrayOutput()

        val count = input.copyTo(output)

        assertTrue(input.eof())
        assertEquals(content.size, count)

        assertArrayEquals(content, output.toByteArray())
    }

    @Test
    fun testInputCopyToWithSize(): Unit = listOf(0, 1, 128 + 1, 1024 + 1, 4096 + 1).forEach { size ->
        val content = ByteArray(8 * 1024) { it.toByte() }
        val input = ByteArrayInput(content)
        val output = ByteArrayOutput()

        val count = input.copyTo(output, size)

        assertEquals(size, count)
        assertTrue(!input.eof())

        assertArrayEquals(content.sliceArray(0 until size), output.toByteArray())
    }

    @Test
    fun testReadAvailableToRange() {
        var executed = false
        val input: Input = object : Input() {
            override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
                assertEquals(1024, endIndex)
                executed = true
                return endIndex - startIndex
            }

            override fun closeSource() {
            }

        }
        val buffer = bufferOf(ByteArray(1024))
        val end = input.readAvailableTo(buffer, 1)
        assertTrue(executed)
        assertEquals(1023, end)
    }

    @Test
    fun testReadUntilNotConsume() {
        var count = 0
        val input = LambdaInput { buffer, startIndex, endIndex ->
            when (count++) {
                0 -> {
                    buffer.storeByteAt(startIndex, 'a'.toByte())
                    1
                }
                1 -> {
                    buffer.storeByteAt(startIndex, 'b'.toByte())
                    1
                }
                else -> 0
            }
        }

        assertEquals(1, input.readUntil { it != 'a'.toByte() })
        assertEquals('b'.toByte(), input.readByte())
    }

    @Test
    fun testReadUntilEof() {
        val input = LambdaInput { _, _, _ -> 0 }
        input.readUntil { true }
    }

    private fun checkException(block: () -> Unit) {
        var fail = false
        try {
            block()
        } catch (exception: Throwable) {
            fail = true
            assertEquals(error, exception)
        }

        assertTrue(fail)
    }
}

internal class SingleShotPool(private val buffer: Buffer) : DefaultPool<Buffer>(1) {
    private var produced = false
    private var disposed = false

    override fun produceInstance(): Buffer {
        produced = true
        return buffer
    }

    override fun disposeInstance(instance: Buffer) {
        assertFalse(disposed)
        disposed = false

        assertTrue { buffer === instance }
    }
}
