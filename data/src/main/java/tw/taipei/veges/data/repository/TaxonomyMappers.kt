package tw.taipei.veges.data.repository

import java.text.Normalizer
import tw.taipei.veges.data.local.TaxonomyConceptWithDetails
import tw.taipei.veges.domain.OfficialCommodityCode
import tw.taipei.veges.domain.OfficialVariant
import tw.taipei.veges.domain.ProduceConcept
import tw.taipei.veges.domain.ProduceConceptId

const val AI_ILLUSTRATION_DISCLOSURE = "AI 生成示意圖，非實物照片。"

fun normalizeSearchQuery(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFKC)
        .lowercase()
        .filterNot(Char::isWhitespace)

fun TaxonomyConceptWithDetails.toDomain(): ProduceConcept = ProduceConcept(
    id = ProduceConceptId(concept.stableId),
    householdName = concept.householdName,
    aliases = aliases.map { it.displayAlias },
    category = concept.category,
    published = concept.published,
    illustrationAsset = concept.illustrationAsset,
    officialVariants = variants.map {
        OfficialVariant(
            code = OfficialCommodityCode(it.commodityCode),
            officialName = it.officialName,
            market = it.market,
        )
    },
)
