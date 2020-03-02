package kotlinx.io.json.data

import kotlinx.serialization.*

@Serializable
data class Canada(
    val type: String,
    val features: List<CanadaFeatures>
)

@Serializable
data class CanadaFeatures(
    val type: String,
    val properties: CanadaProperties,
    val geometry: CanadaGeometry
)

@Serializable
data class CanadaProperties(val name: String)

@Serializable
data class CanadaGeometry(
    val type: String,
    val coordinates: List<List<List<Double>>>
)