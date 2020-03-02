package kotlinx.io.json

import kotlinx.benchmark.*
import kotlinx.io.*
import kotlinx.io.bytes.*
import kotlinx.io.text.*
import java.util.concurrent.*

/**
 * ./gradlew :kotlinx-io-benchmarks:jvmBenchmark -PbenchmarkName="LegacyTextDecodeBenchmark"
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class LegacyTextDecodeBenchmark {

    @Benchmark
    fun smallMbKt() = smallTextPacket.createInput().readUtf8String()

    @Benchmark
    fun smallMbReader() = smallTextBytes.inputStream().reader().readText()

    @Benchmark
    fun smallMbStringCtor() = String(smallTextBytes, Charsets.UTF_8)

    @Benchmark
    fun largeMbKt() = largeTextPacket.createInput().readUtf8String()

    @Benchmark
    fun largeMbReader() = largeTextBytes.inputStream().reader().readText()

    @Benchmark
    fun largeMbStringCtor() = String(largeTextBytes, Charsets.UTF_8)

    @Benchmark
    fun smallASCIIKt() = smallTextPacketASCII.createInput().readUtf8String()

    @Benchmark
    fun smallASCIIReader() = smallTextBytesASCII.inputStream().reader().readText()

    @Benchmark
    fun smallASCIIStringCtor() = String(smallTextBytesASCII, Charsets.UTF_8)

    @Benchmark
    fun largeASCIIKt() = largeTextPacketASCII.createInput().readUtf8String()

    @Benchmark
    fun largeASCIIReader() = largeTextBytesASCII.inputStream().reader().readText()

    @Benchmark
    fun largeASCIIStringCtor() = String(largeTextBytesASCII, Charsets.UTF_8)

    companion object {
        private val smallTextBytes = "\u0422\u0432\u0437.".toByteArray()
        private val smallTextBytesASCII = "ABC.".toByteArray()

        private val largeTextBytes = ByteArray(smallTextBytes.size * 10000) {
            smallTextBytes[it % smallTextBytes.size]
        }

        private val largeTextBytesASCII = ByteArray(smallTextBytesASCII.size * 10000) {
            smallTextBytesASCII[it % smallTextBytesASCII.size]
        }

        private val smallTextPacket = BytesOutput().apply {
            writeByteArray(smallTextBytes)
        }
        private val smallTextPacketASCII = BytesOutput().apply {
            writeByteArray(smallTextBytesASCII)
        }
        private val largeTextPacket = BytesOutput().apply {
            writeByteArray(largeTextBytes)
        }
        private val largeTextPacketASCII = BytesOutput().apply {
            writeByteArray(largeTextBytesASCII)
        }
    }

}
