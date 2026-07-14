package tw.taipei.veges.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class TaxonomyConceptWithDetails(
    @Embedded val concept: TaxonomyConceptEntity,
    @Relation(
        parentColumn = "stableId",
        entityColumn = "conceptId",
    )
    val aliases: List<TaxonomyAliasEntity>,
    @Relation(
        parentColumn = "stableId",
        entityColumn = "conceptId",
    )
    val variants: List<OfficialVariantEntity>,
)
