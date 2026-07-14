package tw.taipei.veges.data.repository

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import tw.taipei.veges.data.local.VegesDatabase
import tw.taipei.veges.domain.ProduceCategory
import tw.taipei.veges.domain.ProduceConcept
import tw.taipei.veges.domain.ProduceRepository

class RoomProduceRepository @Inject constructor(
    private val database: VegesDatabase,
) : ProduceRepository {
    override fun search(query: String): Flow<List<ProduceConcept>> {
        val normalized = normalizeSearchQuery(query)
        if (normalized.isEmpty()) return flowOf(emptyList())

        return database.taxonomyDao()
            .observeSearch("%$normalized%")
            .map { concepts -> concepts.map { it.toDomain() } }
    }

    override fun browse(category: ProduceCategory): Flow<List<ProduceConcept>> =
        database.taxonomyDao()
            .observePublished(category)
            .map { concepts -> concepts.map { it.toDomain() } }
}
