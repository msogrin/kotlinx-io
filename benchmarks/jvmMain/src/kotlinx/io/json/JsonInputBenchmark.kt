package kotlinx.io.json

import kotlinx.io.*
import kotlinx.io.bytes.*
import kotlinx.io.json.data.*
import kotlinx.io.json.ioJson.Companion.parse
import kotlinx.io.json.utils.*
import kotlinx.serialization.*
import org.openjdk.jmh.annotations.*
import java.util.concurrent.*
import kotlin.reflect.*


/**
 * ./gradlew :kotlinx-io-benchmarks:jvmBenchmark -PbenchmarkName="JsonInputBenchmark"
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@UseExperimental(ExperimentalStdlibApi::class, ImplicitReflectionSerializer::class)
class JsonInputBenchmark {
    @Param("canada", "citm", "twitter")
    var datasetName: String = ""

    @Param("io")
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
     * kotlinx.io.json.JsonInputBenchmark.benchmarkParse | datasetName=canada, jsonName=io
     * Success: 69.936 ±(99.9%) 0.189 ms/op [Average]
     * Success: 46.585 ±(99.9%) 0.741 ms/op [Average]
     *
     * kotlinx.io.json.JsonInputBenchmark.benchmarkParse | datasetName=citm, jsonName=io
     * Success: 20.409 ±(99.9%) 0.357 ms/op [Average]
     * Success: 8.466 ±(99.9%) 0.092 ms/op [Average]
     *
     * kotlinx.io.json.JsonInputBenchmark.benchmarkParse | datasetName=twitter, jsonName=io
     * Success: 8.631 ±(99.9%) 0.765 ms/op [Average]
     * Success: 4.811 ±(99.9%) 0.295 ms/op [Average]
     */
    @Benchmark
    fun benchmarkParse() = json.parse(ByteArrayInput(content), type, clazz)

    /**
     * kotlinx.io.json.JsonInputBenchmark.benchmarkWrite | datasetName=canada, jsonName=io
     * Success: 53.280 ±(99.9%) 2.214 ms/op [Average]
     * Success: 57.681 ±(99.9%) 5.365 ms/op [Average]
     * Success: 42.659 ±(99.9%) 0.222 ms/op [Average]
     *
     * kotlinx.io.json.JsonInputBenchmark.benchmarkWrite | datasetName=citm, jsonName=io
     * Success: 6.963 ±(99.9%) 0.548 ms/op [Average]
     * Success: 8.104 ±(99.9%) 1.002 ms/op [Average]
     * Success: 5.280 ±(99.9%) 0.034 ms/op [Average]
     *
     * kotlinx.io.json.JsonInputBenchmark.benchmarkWrite | datasetName=twitter, jsonName=io
     * Success: 4.226 ±(99.9%) 0.069 ms/op [Average]
     * Success: 4.920 ±(99.9%) 0.088 ms/op [Average]
     * Success: 4.647 ±(99.9%) 0.057 ms/op [Average]
     */
    @Benchmark
    fun benchmarkWrite(): Output {
        val stream = BytesOutput()
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
