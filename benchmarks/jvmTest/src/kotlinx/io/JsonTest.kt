package kotlinx.io

import kotlinx.io.json.data.*
import kotlinx.io.json.utils.*
import kotlinx.serialization.*
import kotlin.reflect.*
import kotlin.test.*

class JsonTest {
    val canada = Resource("canada.json").readText()
    val citm = Resource("citm_catalog.json").readText()
    val twitter = Resource("twitter.json").readText()

    val jsons = listOf(
        KxJson,
        SerializationJson,
        GsonJson
//        JacksonJson
    )

    @Test
    fun testBenchmarks() {
        jsons.forEach {
            equalsBackAndForth<Canada>(canada, it)
            equalsBackAndForth<CitmCatalog>(citm, it)
            equalsBackAndForth<Twitter>(twitter, it)
        }
    }

}

@UseExperimental(ImplicitReflectionSerializer::class, ExperimentalStdlibApi::class)
inline fun <reified T : Any> equalsBackAndForth(content: String, json: JsonAdapter) {
    val firstData = json.parse(content, typeOf<T>(), T::class.java)
    val secondContent = json.encode(firstData, typeOf<T>())
    val secondData = json.parse(secondContent, typeOf<T>(), T::class.java)
    assertEquals(secondData, firstData)
}