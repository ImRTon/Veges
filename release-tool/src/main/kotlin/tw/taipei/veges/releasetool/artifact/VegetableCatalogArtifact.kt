package tw.taipei.veges.releasetool.artifact

import kotlinx.serialization.Serializable

@Serializable
data class VegetableCatalogArtifact(
    val schemaVersion: Int,
    val source: String,
    val snapshotId: String,
    val kindCode: String,
    val startRocDate: String,
    val endRocDate: String,
    val markets: List<String>,
    val pageSize: Int,
    val pagesFetched: Int,
    val varieties: List<VegetableVarietyArtifact>,
)

@Serializable
data class VegetableVarietyArtifact(
    val commodityCode: String,
    val officialName: String,
    val markets: List<String>,
    val observedDayCount: Int,
    val recordCount: Int,
    val firstObservedRocDate: String,
    val lastObservedRocDate: String,
)
