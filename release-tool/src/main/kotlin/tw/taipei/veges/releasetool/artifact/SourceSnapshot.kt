package tw.taipei.veges.releasetool.artifact

import kotlinx.serialization.Serializable

@Serializable
data class SourceSnapshotArtifact(
    val schemaVersion: Int,
    val source: String,
    val snapshotId: String,
    val dataCutoff: String,
    val snapshotChecksum: String = "",
    val observations: List<SourceObservationArtifact>,
)

@Serializable
data class SourceObservationArtifact(
    val observedOn: String,
    val commodityCode: String,
    val officialName: String,
    val market: String,
    val valid: Boolean,
    val averagePriceNtdPerKg: String? = null,
    val volumeKg: String? = null,
    val invalidReason: String? = null,
)
