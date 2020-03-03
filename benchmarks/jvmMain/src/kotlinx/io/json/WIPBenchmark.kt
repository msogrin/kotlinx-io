package kotlinx.io.json

import kotlinx.io.*
import kotlinx.io.bytes.*
import kotlinx.io.json.data.*
import kotlinx.io.json.utils.*
import kotlinx.io.text.*
import org.openjdk.jmh.annotations.*
import java.util.concurrent.*
import kotlin.reflect.*

/**
 * ./gradlew :kotlinx-io-benchmarks:jvmBenchmark -PbenchmarkName="WIPBenchmark"
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class WIPBenchmark {
//    @Benchmark
//    fun largeASCIIKt() = largeTextPacketASCII.createInput().readUtf8String()

//    @Benchmark
//    fun largeAsciiBA() = buildString {
//        decodeAsciiChars(largeTextBytesASCII, this)
//    }

//    @Benchmark
//    fun largeAsciiBB() = buildString {
//        decodeAsciiCharsBB(java.nio.ByteBuffer.wrap(largeTextBytesASCII, 0, largeTextBytesASCII.size), this)
//    }

//    @Benchmark
//    fun largeAsciiReader() = buildString {
//        ByteArrayInputStream(largeTextBytesASCII).bufferedReader().readText()
//    }

//    @Benchmark
//    fun largeASCIIStringCtor() = String(largeTextBytesASCII, Charsets.UTF_8)

//    @UseExperimental(ExperimentalStdlibApi::class)
//    @Benchmark
//    fun benchmarkParseCanada() = KxJson.parse(ByteArrayInput(canadaBytes), typeOf<Canada>(), Canada::class.java)

//    @OptIn(ExperimentalStdlibApi::class)
//    @Benchmark
//    fun benchmarkWrite(): Output {
//        val stream = BytesOutput()
//        KxJson.encode(canadaData, typeOf<Canada>())
//        return stream
//    }

    /**
     * initial: Success: 6.731 ±(99.9%) 0.262 ms/op [Average]
     * previous + explicit buffer and array size: Success: 3.048 ±(99.9%) 0.096 ms/op [Average]
     * previous + utf8 -> ascii writer: Success: 1.408 ±(99.9%) 0.056 ms/op [Average]
     */
    @Benchmark
    fun benchmarkStringByteArrayWithInput(): ByteArray = buildInput(canada.length * 2) {
        writeASCIIString(canada)
    }.readByteArray(canadaBytes.size)

//    Success: 0.280 ±(99.9%) 0.030 ms/op [Average]
//    @Benchmark
//    fun benchmarkStringToByteArray(): ByteArray = canada.toByteArray()

//    @Benchmark
//    fun benchmarkByteArrayToStringWithInput() {
//
//    }
//
//    @Benchmark
//    fun benchmarkByteArrayToString() {
//
//    }

    companion object {
        private val canada = Resource("canada.json").readText()
        private val canadaBytes = canada.toByteArray()

        @OptIn(ExperimentalStdlibApi::class)
        private val canadaData = KxJson.parse(canada, typeOf<Canada>(), Canada::class.java)

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