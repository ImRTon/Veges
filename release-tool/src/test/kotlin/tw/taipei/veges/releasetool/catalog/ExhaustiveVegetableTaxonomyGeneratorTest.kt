package tw.taipei.veges.releasetool.catalog

import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.taipei.veges.releasetool.artifact.ArtifactCodec
import tw.taipei.veges.releasetool.artifact.PublicationState
import tw.taipei.veges.releasetool.artifact.TaxonomyArtifact
import tw.taipei.veges.releasetool.artifact.TaxonomyCategory
import tw.taipei.veges.releasetool.artifact.VegetableCatalogArtifact
import tw.taipei.veges.releasetool.artifact.VegetableVarietyArtifact
import tw.taipei.veges.releasetool.audit.ArtifactValidator

class ExhaustiveVegetableTaxonomyGeneratorTest {
    @Test
    fun generationKeepsEveryOfficialCodeIncludingSparseVarieties() {
        val inventory = inventory()
        val artifact = generate(inventory)
        val vegetables = artifact.concepts.filter { it.category == TaxonomyCategory.VEGETABLE }

        assertEquals(2, vegetables.size)
        assertEquals(setOf("LA1", "LA2", "ZZ1"), vegetables.flatMap { it.officialMappings }.map { it.commodityCode }.toSet())
        assertEquals("vegetable.cabbage", vegetables.first { it.officialMappings.first().commodityCode == "LA1" }.stableId)
        assertEquals(PublicationState.CANDIDATE, vegetables.first { it.officialMappings.first().commodityCode == "ZZ1" }.publicationState)
        assertTrue(vegetables.first { it.officialMappings.first().commodityCode == "ZZ1" }.aliases.contains("ZZ1"))
        assertEquals(
            vegetables.map { it.image.assetPath }.size,
            vegetables.map { it.image.assetPath }.distinct().size,
        )
        assertTrue(vegetables.all { it.image.assetPath.endsWith(".webp") })
        assertTrue(artifact.concepts.any { it.category == TaxonomyCategory.FRUIT && it.stableId == "fruit.banana" })
        assertEquals(artifact.artifactChecksum, ArtifactCodec.taxonomyChecksum(artifact))
        ArtifactValidator.validateTaxonomy(artifact)
        ExhaustiveVegetableTaxonomyGenerator.validateCoverage(artifact, inventory)
    }

    @Test
    fun identicalInventoryProducesDeterministicTaxonomyAndChecksum() {
        val inventory = inventory()
        val first = generate(inventory)
        val second = generate(inventory.copy(varieties = inventory.varieties.reversed()))

        assertEquals(ArtifactCodec.encodeTaxonomy(first), ArtifactCodec.encodeTaxonomy(second))
        assertEquals(first.artifactChecksum, second.artifactChecksum)
    }

    @Test
    fun completenessValidationRejectsDroppedOfficialCode() {
        val inventory = inventory()
        val incomplete = generate(inventory).copy(
            concepts = generate(inventory).concepts.filterNot { concept ->
                concept.officialMappings.any { it.commodityCode == "ZZ1" }
            },
        )

        val failure = runCatching {
            ExhaustiveVegetableTaxonomyGenerator.validateCoverage(incomplete, inventory)
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun reviewedTaiwanHouseholdNameAndOfficialCodeAreSearchAliases() {
        val artifact = generate(
            inventory().copy(
                varieties = listOf(variety("LF2", "蕹菜-小葉", 35)),
            ),
        )
        val concept = artifact.concepts.single { it.category == TaxonomyCategory.VEGETABLE }

        assertEquals("空心菜", concept.householdName)
        assertTrue(concept.aliases.containsAll(listOf("蕹菜", "空心菜", "LF2", "蕹菜-小葉")))
    }

    @Test
    fun reviewedHouseholdNameOverridesLegacyOfficialStyleName() {
        val artifact = generate(
            inventory().copy(
                varieties = listOf(
                    variety("LD1", "青江白菜-小梗", 35),
                    variety("LD2", "青江白菜-大梗", 35),
                    variety("LD8", "青江白菜-水耕", 35),
                ),
            ),
        )
        val concept = artifact.concepts.single { it.category == TaxonomyCategory.VEGETABLE }

        assertEquals("青江菜", concept.householdName)
        assertTrue(concept.aliases.containsAll(listOf("青江菜", "青江白菜", "LD1", "LD2", "LD8")))
    }

    @Test
    fun reviewedSproutCodesRemainDistinctHouseholdProduce() {
        val artifact = generate(
            inventory().copy(
                varieties = listOf(
                    variety("SX0", "芽菜類-其他", 35),
                    variety("SX1", "芽菜類-綠豆芽", 35),
                    variety("SX2", "芽菜類-黃豆芽", 35),
                    variety("SX3", "芽菜類-豌豆芽", 35),
                    variety("SX4", "芽菜類-苜蓿芽", 35),
                ),
            ),
        )
        val vegetables = artifact.concepts.filter { it.category == TaxonomyCategory.VEGETABLE }

        assertEquals(5, vegetables.size)
        assertEquals(
            setOf("其他芽菜", "綠豆芽", "黃豆芽", "豌豆芽", "苜蓿芽"),
            vegetables.map { it.householdName }.toSet(),
        )
        assertEquals(
            setOf(
                "illustrations/catalog/vegetable-sx0.webp",
                "illustrations/catalog/vegetable-sx1.webp",
                "illustrations/catalog/vegetable-sx2.webp",
                "illustrations/catalog/vegetable-sx3.webp",
                "illustrations/catalog/vegetable-sx4.webp",
            ),
            vegetables.map { it.image.assetPath }.toSet(),
        )
    }

    private fun generate(inventory: VegetableCatalogArtifact): TaxonomyArtifact =
        ExhaustiveVegetableTaxonomyGenerator.generate(
            base = baseTaxonomy(),
            inventory = inventory,
            taxonomyVersion = "candidate-all-vegetables-2026-07-26",
            reviewedAt = "2026-07-26",
            reviewedBy = "project-owner",
        )

    private fun baseTaxonomy(): TaxonomyArtifact {
        val json = requireNotNull(javaClass.classLoader?.getResourceAsStream("catalog/candidate-taxonomy.json"))
            .bufferedReader()
            .use { it.readText() }
        return ArtifactCodec.strictJson.decodeFromString(json)
    }

    private fun inventory() = VegetableCatalogArtifact(
        schemaVersion = 1,
        source = "MOA_FARM_TRANS_DATA",
        snapshotId = "test-n04",
        kindCode = "N04",
        startRocDate = "115.04.27",
        endRocDate = "115.07.26",
        markets = listOf("TAIPEI_FIRST", "TAIPEI_SECOND"),
        pageSize = 9999,
        pagesFetched = 2,
        varieties = listOf(
            variety("LA1", "甘藍-初秋", 70),
            variety("LA2", "甘藍-改良種", 70),
            variety("ZZ1", "稀有菜-測試", 1, listOf("TAIPEI_FIRST")),
        ),
    )

    private fun variety(
        code: String,
        name: String,
        days: Int,
        markets: List<String> = listOf("TAIPEI_FIRST", "TAIPEI_SECOND"),
    ) = VegetableVarietyArtifact(
        commodityCode = code,
        officialName = name,
        markets = markets,
        observedDayCount = days,
        recordCount = days * markets.size,
        firstObservedRocDate = "115.04.28",
        lastObservedRocDate = "115.07.24",
    )
}
