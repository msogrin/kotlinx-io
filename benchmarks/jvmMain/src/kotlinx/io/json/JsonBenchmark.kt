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
 * ./gradlew :kotlinx-io-benchmarks:jvmBenchmark -PbenchmarkName="JsonBenchmark"
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@UseExperimental(ExperimentalStdlibApi::class, ImplicitReflectionSerializer::class)
class JsonBenchmark {
    @Param("canada", "citm", "twitter")
    var datasetName: String = ""

    // Enable to full comparison
    @Param("io"/*, "default", "gson"*/)
    var jsonName: String = ""

    private lateinit var data: Any
    private lateinit var content: String
    private lateinit var json: JsonAdapter
    private lateinit var type: KType
    private lateinit var clazz: Class<*>

    class Dataset(val content: String, val type: KType, val clazz: Class<*>, val data: Any)

    @Setup
    fun setup() {
        val dataset = JsonStreamBenchmark.datasets[datasetName]!!

        json = JsonStreamBenchmark.jsons[jsonName]!!
        content = dataset.content
        type = dataset.type
        clazz = dataset.clazz
        data = dataset.data
    }

    /**
     * Canada
     *
     * kotlinx.io.json.JsonBenchmark.benchmarkRead | datasetName=canada, jsonName=io
     * Success: 59.194 ±(99.9%) 0.238 ms/op [Average]
     *
     * kotlinx.io.json.JsonBenchmark.benchmarkRead | datasetName=canada, jsonName=default
     * Success: 35.438 ±(99.9%) 3.842 ms/op [Average]
     *
     * kotlinx.io.json.JsonBenchmark.benchmarkRead | datasetName=canada, jsonName=gson
     * Success: 38.080 ±(99.9%) 2.733 ms/op [Average]
     *
     * Citm
     *
     * kotlinx.io.json.JsonBenchmark.benchmarkRead | datasetName=citm, jsonName=io
     * Success: 23.625 ±(99.9%) 3.009 ms/op [Average]
     *
     * kotlinx.io.json.JsonBenchmark.benchmarkRead | datasetName=citm, jsonName=default
     * Success: 5.133 ±(99.9%) 0.358 ms/op [Average]
     *
     * kotlinx.io.json.JsonBenchmark.benchmarkRead | datasetName=citm, jsonName=gson
     * Success: 4.566 ±(99.9%) 0.332 ms/op [Average]
     *
     * Twitter
     *
     * kotlinx.io.json.JsonBenchmark.benchmarkRead | datasetName=twitter, jsonName=io
     * Success: 9.468 ±(99.9%) 0.629 ms/op [Average]
     *
     * kotlinx.io.json.JsonBenchmark.benchmarkRead | datasetName=twitter, jsonName=default
     * Success: 2.731 ±(99.9%) 0.206 ms/op [Average]
     *
     * kotlinx.io.json.JsonBenchmark.benchmarkRead | datasetName=twitter, jsonName=gson
     * Success: 2.071 ±(99.9%) 0.105 ms/op [Average]
     */
    @Benchmark
    fun benchmarkRead() = json.parse(content, type, clazz)

    /**
     * Canada
     *
     * kotlinx.io.json.JsonBenchmark.benchmarkWrite | datasetName=canada, jsonName=io
     * Success: 47.396 ±(99.9%) 4.178 ms/op [Average]
     *
     * kotlinx.io.json.JsonBenchmark.benchmarkWrite | datasetName=canada, jsonName=default
     * Success: 39.998 ±(99.9%) 1.937 ms/op [Average]
     *
     * kotlinx.io.json.JsonBenchmark.benchmarkWrite | datasetName=canada, jsonName=gson
     * Success: 41.044 ±(99.9%) 0.419 ms/op [Average]
     *
     * Citm
     *
     * kotlinx.io.json.JsonBenchmark.benchmarkWrite | datasetName=citm, jsonName=io
     * Success: 12.392 ±(99.9%) 0.769 ms/op [Average]
     *
     * kotlinx.io.json.JsonBenchmark.benchmarkWrite | datasetName=citm, jsonName=default
     * Success: 2.824 ±(99.9%) 0.332 ms/op [Average]
     *
     * kotlinx.io.json.JsonBenchmark.benchmarkWrite | datasetName=citm, jsonName=gson
     * Success: 4.598 ±(99.9%) 0.452 ms/op [Average]
     *
     * Twitter
     *
     * kotlinx.io.json.JsonBenchmark.benchmarkWrite | datasetName=twitter, jsonName=io
     * Success: 10.641 ±(99.9%) 0.565 ms/op [Average]
     *
     * kotlinx.io.json.JsonBenchmark.benchmarkWrite | datasetName=twitter, jsonName=default
     * Success: 1.470 ±(99.9%) 0.095 ms/op [Average]
     *
     * kotlinx.io.json.JsonBenchmark.benchmarkWrite | datasetName=twitter, jsonName=gson
     * Success: 2.445 ±(99.9%) 0.209 ms/op [Average]
     */
    @Benchmark
    fun benchmarkWrite() = json.encode(data, type)

    companion object {
        val canada = Resource("canada.json").readText()
        val citmCatalog = Resource("citm_catalog.json").readText()
        val twitter = Resource("twitter.json").readText()

        val jsons = mapOf(
            "io" to KxJson,
            "default" to SerializationJson,
            "gson" to GsonJson
        )

        val canadaObject = ioJson.parse<Canada>(canada)
        val citmObject = ioJson.parse<CitmCatalog>(citmCatalog)
        val twitterObject = ioJson.parse<Twitter>(twitter)

        val datasets = mapOf(
            "canada" to Dataset(canada, typeOf<Canada>(), Canada::class.java, canadaObject),
            "citm" to Dataset(citmCatalog, typeOf<CitmCatalog>(), CitmCatalog::class.java, citmObject),
            "twitter" to Dataset(twitter, typeOf<Twitter>(), Twitter::class.java, twitterObject)
        )
    }
}
