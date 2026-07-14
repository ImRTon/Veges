package tw.taipei.veges.releasetool.audit

import java.math.BigDecimal
import java.time.LocalDate
import tw.taipei.veges.releasetool.artifact.ArtifactCodec
import tw.taipei.veges.releasetool.artifact.ReviewStatus
import tw.taipei.veges.releasetool.artifact.SourceSnapshotArtifact
import tw.taipei.veges.releasetool.artifact.TaxonomyArtifact

object ArtifactValidator {
    private val allowedMarkets = setOf("TAIPEI_FIRST", "TAIPEI_SECOND")

    fun validateTaxonomy(artifact: TaxonomyArtifact) {
        require(artifact.schemaVersion == 1) { "Unsupported taxonomy schema version" }
        require(artifact.taxonomyVersion.isNotBlank()) { "Taxonomy version is required" }
        require(artifact.review.reviewedAt.isNotBlank() && artifact.review.reviewedBy.isNotBlank()) {
            "Review metadata is required"
        }
        require(artifact.concepts.map { it.stableId }.distinct().size == artifact.concepts.size) {
            "Taxonomy stable IDs must be unique"
        }
        val conceptIds = artifact.concepts.map { it.stableId }.toSet()
        val aliases = artifact.concepts.flatMap { concept ->
            concept.aliases.map { normalize(it) to concept.stableId }
        }.groupBy({ it.first }, { it.second })

        artifact.concepts.forEach { concept ->
            require(concept.stableId.isNotBlank()) { "Concept stable ID is required" }
            require(concept.householdName.isNotBlank()) { "Household name is required" }
            require(concept.aliases.isNotEmpty()) { "At least one alias is required for ${concept.stableId}" }
            require(concept.image.disclosure == AI_IMAGE_DISCLOSURE) {
                "Invalid illustration disclosure for ${concept.stableId}"
            }
            require(concept.image.assetPath.isNotBlank()) { "Image asset path is required for ${concept.stableId}" }
            require(concept.officialMappings.map { it.commodityCode to it.market }.distinct().size == concept.officialMappings.size) {
                "Duplicate official mapping for ${concept.stableId}"
            }
            concept.officialMappings.forEach { mapping ->
                require(mapping.commodityCode.isNotBlank() && mapping.officialName.isNotBlank()) {
                    "Official mapping identity is incomplete for ${concept.stableId}"
                }
                require(mapping.market in allowedMarkets) { "Unsupported market ${mapping.market}" }
            }
            if (concept.publicationState.name == "PUBLISHED") {
                require(artifact.review.status == ReviewStatus.APPROVED) {
                    "Published concept ${concept.stableId} requires approved review metadata"
                }
                require(concept.image.reviewStatus == ReviewStatus.APPROVED) {
                    "Published concept ${concept.stableId} requires approved image review"
                }
            }
        }

        artifact.ambiguitySets.forEach { ambiguity ->
            require(ambiguity.normalizedAlias == normalize(ambiguity.displayAlias)) {
                "Ambiguity alias normalization mismatch"
            }
            require(ambiguity.targetConceptIds.size >= 2) { "Ambiguity set needs at least two targets" }
            require(ambiguity.targetConceptIds.all { it in conceptIds }) { "Dangling ambiguity target" }
            require(ambiguity.targetConceptIds.toSet() == aliases[ambiguity.normalizedAlias]?.toSet()) {
                "Ambiguity set does not match reviewed aliases"
            }
        }
        aliases.filterValues { it.distinct().size > 1 }.forEach { (alias, targets) ->
            require(artifact.ambiguitySets.any { it.normalizedAlias == alias && it.targetConceptIds.toSet() == targets.toSet() }) {
                "Ambiguous alias $alias requires an explicit ambiguity set"
            }
        }
    }

    fun validateSnapshot(snapshot: SourceSnapshotArtifact) {
        require(snapshot.schemaVersion == 1) { "Unsupported source snapshot schema version" }
        require(snapshot.source.isNotBlank() && snapshot.snapshotId.isNotBlank()) { "Snapshot identity is required" }
        val cutoff = LocalDate.parse(snapshot.dataCutoff)
        snapshot.observations.forEach { observation ->
            val date = LocalDate.parse(observation.observedOn)
            require(!date.isAfter(cutoff)) { "Observation is after source cutoff" }
            require(observation.commodityCode.isNotBlank()) { "Snapshot commodity code is required" }
            require(observation.market in allowedMarkets) { "Unsupported snapshot market ${observation.market}" }
            if (observation.valid) {
                val average = observation.averagePriceNtdPerKg?.let(::BigDecimal)
                val volume = observation.volumeKg?.let(::BigDecimal)
                require(average != null && average > BigDecimal.ZERO) { "Valid observations need positive average price" }
                require(volume != null && volume > BigDecimal.ZERO) { "Valid observations need positive volume" }
            }
        }
    }

    fun normalize(value: String): String = value
        .trim()
        .lowercase()
        .replace(Regex("\\s+"), "")
}
