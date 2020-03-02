package kotlinx.io.json

import kotlinx.benchmark.*
import kotlinx.io.*
import kotlinx.io.json.data.*
import kotlinx.io.json.utils.*
import kotlinx.serialization.*
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.Setup
import java.util.concurrent.*
import kotlin.reflect.*

/**
 * ./gradlew :kotlinx-io-benchmarks:jvmBenchmark -PbenchmarkName="JsonStreamBenchmark"
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@UseExperimental(ExperimentalStdlibApi::class, ImplicitReflectionSerializer::class)
class JsonStreamBenchmark {
    @Param("canada", "citm", "twitter")
    var datasetName: String = ""

    @Param("gson")
    var jsonName: String = ""

    private lateinit var data: Any
    private lateinit var content: ByteArray
    private lateinit var json: JsonAdapter
    private lateinit var type: KType
    private lateinit var clazz: Class<*>

    @Setup
    fun setup() {
        json = jsons[jsonName]!!
        val dataset = datasets[datasetName]!!
        content = dataset.contentBytes
        type = dataset.type
        clazz = dataset.clazz
        data = dataset.data
    }

    /**
     * kotlinx.io.json.JsonStreamBenchmark.benchmarkParse | datasetName=canada, jsonName=gson
     * Success: 39.142 ±(99.9%) 0.238 ms/op [Average]
     *
     * kotlinx.io.json.JsonStreamBenchmark.benchmarkParse | datasetName=citm, jsonName=gson
     * Success: 5.871 ±(99.9%) 0.470 ms/op [Average]
     *
     * kotlinx.io.json.JsonStreamBenchmark.benchmarkParse | datasetName=twitter, jsonName=gson
     * Success: 5.432 ±(99.9%) 0.344 ms/op [Average]
     */
    @Benchmark
    fun benchmarkParse() = json.parse(ByteArrayInputStream(content), type, clazz)

    /**
     * kotlinx.io.json.JsonStreamBenchmark.benchmarkWrite | datasetName=canada, jsonName=gson
     * Success: 42.005 ±(99.9%) 1.866 ms/op [Average]
     *
     * kotlinx.io.json.JsonStreamBenchmark.benchmarkWrite | datasetName=citm, jsonName=gson
     * Success: 4.328 ±(99.9%) 0.175 ms/op [Average]
     *
     * kotlinx.io.json.JsonStreamBenchmark.benchmarkWrite | datasetName=twitter, jsonName=gson
     * Success: 2.836 ±(99.9%) 0.334 ms/op [Average]
     */
    @Benchmark
    fun benchmarkWrite(): ByteArrayOutputStream {
        val stream = ByteArrayOutputStream()
        json.encode(data, type, stream)
        return stream
    }

    companion object {
        val canada = Resource("canada.json").readText()
        val citmCatalog = Resource("citm_catalog.json").readText()
        val twitter = Resource("twitter.json").readText()

        val canadaObject = ioJson.parse<Canada>(canada)
        val citmObject = ioJson.parse<CitmCatalog>(citmCatalog)
        val twitterObject = ioJson.parse<Twitter>(twitter)

        val jsons = mapOf(
            "io" to KxJson,
            "default" to SerializationJson,
            "gson" to GsonJson
        )

        class Dataset(val content: String, val type: KType, val clazz: Class<*>, val data: Any) {
            val contentBytes: ByteArray = content.toByteArray()
        }

        val datasets = mapOf(
            "canada" to Dataset(canada, typeOf<Canada>(), Canada::class.java, canadaObject),
            "citm" to Dataset(citmCatalog, typeOf<CitmCatalog>(), CitmCatalog::class.java, citmObject),
            "twitter" to Dataset(twitter, typeOf<Twitter>(), Twitter::class.java, twitterObject)
        )
    }
}
