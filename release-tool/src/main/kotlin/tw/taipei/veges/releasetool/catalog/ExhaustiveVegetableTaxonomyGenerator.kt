package tw.taipei.veges.releasetool.catalog

import tw.taipei.veges.releasetool.artifact.AmbiguitySetArtifact
import tw.taipei.veges.releasetool.artifact.ArtifactCodec
import tw.taipei.veges.releasetool.artifact.ImageMetadataArtifact
import tw.taipei.veges.releasetool.artifact.OfficialMappingArtifact
import tw.taipei.veges.releasetool.artifact.PublicationState
import tw.taipei.veges.releasetool.artifact.ReviewMetadata
import tw.taipei.veges.releasetool.artifact.ReviewStatus
import tw.taipei.veges.releasetool.artifact.TaxonomyArtifact
import tw.taipei.veges.releasetool.artifact.TaxonomyCategory
import tw.taipei.veges.releasetool.artifact.TaxonomyConceptArtifact
import tw.taipei.veges.releasetool.artifact.VegetableCatalogArtifact
import tw.taipei.veges.releasetool.artifact.VegetableVarietyArtifact
import tw.taipei.veges.releasetool.audit.AI_IMAGE_DISCLOSURE
import tw.taipei.veges.releasetool.audit.ArtifactValidator

object ExhaustiveVegetableTaxonomyGenerator {
    fun generate(
        base: TaxonomyArtifact,
        inventory: VegetableCatalogArtifact,
        taxonomyVersion: String,
        reviewedAt: String,
        reviewedBy: String,
    ): TaxonomyArtifact {
        ArtifactValidator.validateTaxonomy(base)
        validateInventory(inventory)

        val legacyVegetables = base.concepts
            .filter { it.category == TaxonomyCategory.VEGETABLE }
            .associateBy { it.stableId }
        val concepts = buildList {
            addAll(base.concepts.filter { it.category == TaxonomyCategory.FRUIT })
            inventory.varieties
                .groupBy(::reviewedGroupingName)
                .toSortedMap()
                .forEach { (baseName, varieties) ->
                    add(varieties.toConcept(baseName, legacyVegetables))
                }
        }
        val aliasesByNormalizedName = concepts
            .flatMap { concept ->
                concept.aliases.map { alias -> ArtifactValidator.normalize(alias) to concept.stableId }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, ids) -> ids.distinct().sorted() }
        val ambiguities = aliasesByNormalizedName
            .filterValues { it.size > 1 }
            .map { (normalizedAlias, targetIds) ->
                val displayAlias = concepts
                    .asSequence()
                    .flatMap { it.aliases.asSequence() }
                    .first { ArtifactValidator.normalize(it) == normalizedAlias }
                AmbiguitySetArtifact(
                    normalizedAlias = normalizedAlias,
                    displayAlias = displayAlias,
                    targetConceptIds = targetIds,
                )
            }
            .sortedBy { it.normalizedAlias }
        val draft = TaxonomyArtifact(
            schemaVersion = 1,
            taxonomyVersion = taxonomyVersion,
            artifactChecksum = "",
            review = ReviewMetadata(
                status = ReviewStatus.APPROVED,
                reviewedAt = reviewedAt,
                reviewedBy = reviewedBy,
                notes = "Project owner approved exhaustive N04 vegetable breadth; added family illustration hashes remain pending visual approval.",
            ),
            concepts = concepts,
            ambiguitySets = ambiguities,
        )
        val result = draft.copy(artifactChecksum = ArtifactCodec.taxonomyChecksum(draft))
        ArtifactValidator.validateTaxonomy(result)
        validateCoverage(result, inventory)
        return result
    }

    fun validateInventory(inventory: VegetableCatalogArtifact) {
        require(inventory.schemaVersion == 1) { "Unsupported vegetable inventory schema" }
        require(inventory.source == "MOA_FARM_TRANS_DATA") { "Unexpected vegetable inventory source" }
        require(inventory.kindCode == "N04") { "Only the official N04 vegetable catalog is supported" }
        require(inventory.snapshotId.isNotBlank()) { "Vegetable inventory snapshot ID is required" }
        require(inventory.pageSize in 1..10_000 && inventory.pagesFetched > 0) {
            "Vegetable inventory pagination metadata is invalid"
        }
        require(inventory.markets.toSet() == ALLOWED_MARKETS) {
            "Vegetable inventory must audit Taipei First and Taipei Second"
        }
        require(inventory.varieties.isNotEmpty()) { "Vegetable inventory is empty" }
        require(inventory.varieties.map { it.commodityCode }.distinct().size == inventory.varieties.size) {
            "Vegetable inventory commodity codes must be unique"
        }
        inventory.varieties.forEach { variety ->
            require(variety.commodityCode.matches(Regex("[A-Za-z0-9]+"))) {
                "Invalid vegetable commodity code ${variety.commodityCode}"
            }
            require(variety.officialName.isNotBlank()) {
                "Official name is required for ${variety.commodityCode}"
            }
            require(variety.markets.isNotEmpty() && variety.markets.all { it in ALLOWED_MARKETS }) {
                "Invalid markets for ${variety.commodityCode}"
            }
            require(variety.observedDayCount > 0 && variety.recordCount >= variety.observedDayCount) {
                "Invalid observation evidence for ${variety.commodityCode}"
            }
            require(ROC_DATE.matches(variety.firstObservedRocDate) && ROC_DATE.matches(variety.lastObservedRocDate)) {
                "Invalid ROC date evidence for ${variety.commodityCode}"
            }
        }
    }

    fun validateCoverage(taxonomy: TaxonomyArtifact, inventory: VegetableCatalogArtifact) {
        val expected = inventory.varieties.map { it.commodityCode }.toSet()
        val vegetableConcepts = taxonomy.concepts.filter { it.category == TaxonomyCategory.VEGETABLE }
        val mappingsByCode = vegetableConcepts
            .flatMap { concept -> concept.officialMappings.map { it.commodityCode to concept.stableId } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, ids) -> ids.distinct() }
        require(mappingsByCode.keys == expected) {
            val missing = (expected - mappingsByCode.keys).sorted()
            val unexpected = (mappingsByCode.keys - expected).sorted()
            "Vegetable taxonomy completeness mismatch; missing=$missing unexpected=$unexpected"
        }
        require(mappingsByCode.values.all { it.size == 1 }) {
            "Every official vegetable code must map to exactly one variety entry"
        }
        inventory.varieties.forEach { variety ->
            val conceptId = requireNotNull(mappingsByCode[variety.commodityCode]).single()
            val concept = vegetableConcepts.first { it.stableId == conceptId }
            val actualMarkets = concept.officialMappings
                .filter { it.commodityCode == variety.commodityCode }
                .map { it.market }
                .toSet()
            require(actualMarkets == variety.markets.toSet()) {
                "Market coverage mismatch for ${variety.commodityCode}"
            }
        }
    }

    private fun List<VegetableVarietyArtifact>.toConcept(
        baseName: String,
        legacyVegetables: Map<String, TaxonomyConceptArtifact>,
    ): TaxonomyConceptArtifact {
        val ordered = sortedBy { it.commodityCode }
        val primaryCode = ordered.first().commodityCode
        val legacy = LEGACY_BASE_NAMES[baseName]?.let(legacyVegetables::get)
        val householdName = REVIEWED_HOUSEHOLD_NAMES[baseName]
            ?: legacy?.householdName
            ?: baseName
        val aliases = buildList {
            add(householdName)
            add(baseName)
            ordered.forEach { variety ->
                add(variety.officialName)
                add(variety.commodityCode)
            }
            legacy?.aliases?.let(::addAll)
            REVIEWED_ALIASES[baseName]?.let(::addAll)
        }.map(String::trim).filter(String::isNotBlank).distinct()
        return TaxonomyConceptArtifact(
            stableId = legacy?.stableId ?: "vegetable.moa.${primaryCode.lowercase()}",
            householdName = householdName,
            aliases = aliases,
            category = TaxonomyCategory.VEGETABLE,
            publicationState = if (legacy != null) PublicationState.PUBLISHED else PublicationState.CANDIDATE,
            officialMappings = ordered.flatMap { variety ->
                variety.markets.sorted().map { market ->
                    OfficialMappingArtifact(
                        commodityCode = variety.commodityCode,
                        officialName = variety.officialName,
                        market = market,
                    )
                }
            },
            image = legacy?.image ?: ImageMetadataArtifact(
                assetPath = "illustrations/catalog/vegetable-${primaryCode.lowercase()}.webp",
                generated = true,
                disclosure = AI_IMAGE_DISCLOSURE,
                reviewedAt = null,
                reviewedBy = null,
                reviewStatus = ReviewStatus.DRAFT,
            ),
        )
    }

    private val ALLOWED_MARKETS = setOf("TAIPEI_FIRST", "TAIPEI_SECOND")
    private val ROC_DATE = Regex("\\d{3}\\.\\d{2}\\.\\d{2}")
    private val REVIEWED_DISTINCT_VARIETY_CODES = setOf("SX0", "SX1", "SX2", "SX3", "SX4")
    private fun reviewedGroupingName(variety: VegetableVarietyArtifact): String =
        if (variety.commodityCode in REVIEWED_DISTINCT_VARIETY_CODES) {
            variety.officialName.trim()
        } else {
            variety.officialName.substringBefore('-').trim()
        }

    private val LEGACY_BASE_NAMES = mapOf(
        "甘藍" to "vegetable.cabbage",
        "小白菜" to "vegetable.small-bok-choy",
        "包心白" to "vegetable.napa-cabbage",
        "青江白菜" to "vegetable.qingjiang-bok-choy",
    )
    private val REVIEWED_HOUSEHOLD_NAMES = mapOf(
        "蕹菜" to "空心菜",
        "甘薯葉" to "地瓜葉",
        "青花苔" to "青花菜",
        "隼人瓜" to "佛手瓜",
        "芫荽" to "香菜",
        "甘薯" to "地瓜",
        "薯蕷" to "山藥",
        "金絲菇" to "金針菇",
        "藤川七" to "川七",
        "球莖甘藍" to "大頭菜",
        "青江白菜" to "青江菜",
        "芽菜類-其他" to "其他芽菜",
        "芽菜類-綠豆芽" to "綠豆芽",
        "芽菜類-黃豆芽" to "黃豆芽",
        "芽菜類-豌豆芽" to "豌豆芽",
        "芽菜類-苜蓿芽" to "苜蓿芽",
    )
    private val REVIEWED_ALIASES = mapOf(
        "胡瓜" to listOf("大黃瓜", "刺瓜"),
        "花胡瓜" to listOf("小黃瓜"),
        "絲瓜" to listOf("菜瓜"),
        "扁蒲" to listOf("蒲仔"),
        "甜椒" to listOf("彩椒", "青椒"),
        "豌豆" to listOf("荷蘭豆"),
        "菜豆" to listOf("豇豆", "長豆"),
        "敏豆" to listOf("四季豆"),
        "萊豆" to listOf("皇帝豆"),
        "越瓜" to listOf("醃瓜"),
        "蕹菜" to listOf("空心菜"),
        "甘薯葉" to listOf("地瓜葉", "番薯葉"),
        "青花苔" to listOf("青花菜", "綠花椰菜"),
        "隼人瓜" to listOf("佛手瓜"),
        "芫荽" to listOf("香菜"),
        "萵苣菜" to listOf("萵苣"),
        "甘薯" to listOf("地瓜", "番薯"),
        "薯蕷" to listOf("山藥"),
        "金絲菇" to listOf("金針菇"),
        "藤川七" to listOf("川七", "藤三七"),
        "球莖甘藍" to listOf("大頭菜"),
        "黑甜仔菜" to listOf("龍葵"),
        "菾菜" to listOf("甜菜"),
        "蕎頭" to listOf("藠頭"),
        "茭白筍" to listOf("美人腿"),
        "荸薺" to listOf("馬蹄"),
        "豆薯" to listOf("涼薯"),
    )
}
