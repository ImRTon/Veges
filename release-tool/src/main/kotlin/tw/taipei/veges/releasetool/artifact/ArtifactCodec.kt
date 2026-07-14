package tw.taipei.veges.releasetool.artifact

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
object ArtifactCodec {
    val strictJson = Json {
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = false
        isLenient = false
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    private val checksumJson = Json {
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = false
        isLenient = false
        prettyPrint = false
    }

    fun encodeTaxonomy(artifact: TaxonomyArtifact): String =
        strictJson.encodeToString(artifact.canonical())

    fun encodeSnapshot(snapshot: SourceSnapshotArtifact): String =
        strictJson.encodeToString(snapshot.canonical())

    fun taxonomyChecksum(artifact: TaxonomyArtifact): String = sha256(
        checksumJson.encodeToString(artifact.copy(artifactChecksum = "").canonical()),
    )

    fun snapshotChecksum(snapshot: SourceSnapshotArtifact): String = sha256(
        checksumJson.encodeToString(snapshot.copy(snapshotChecksum = "").canonical()),
    )

    fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }

    private fun TaxonomyArtifact.canonical(): TaxonomyArtifact = copy(
        concepts = concepts.sortedBy { it.stableId }.map { concept ->
            concept.copy(
                aliases = concept.aliases.sortedWith(String.CASE_INSENSITIVE_ORDER),
                officialMappings = concept.officialMappings.sortedWith(
                    compareBy({ it.commodityCode }, { it.market }, { it.officialName }),
                ),
            )
        },
        ambiguitySets = ambiguitySets.sortedBy { it.normalizedAlias }.map {
            it.copy(targetConceptIds = it.targetConceptIds.sorted())
        },
    )

    private fun SourceSnapshotArtifact.canonical(): SourceSnapshotArtifact = copy(
        observations = observations.sortedWith(
            compareBy({ it.observedOn }, { it.market }, { it.commodityCode }, { it.officialName }),
        ),
    )
}
