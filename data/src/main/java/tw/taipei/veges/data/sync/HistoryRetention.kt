package tw.taipei.veges.data.sync

import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import tw.taipei.veges.data.local.VegesDatabase
import tw.taipei.veges.domain.SourceKind

class HistoryRetention @Inject constructor(
    private val database: VegesDatabase,
    private val clock: Clock,
) {
    suspend fun pruneReplaceableWholesaleHistory(): Int {
        val cutoff = LocalDate.now(clock).minusYears(1).minusDays(1)
        return database.sourceDao().deleteSourceBefore(cutoff, SourceKind.MOA_WHOLESALE)
    }
}
