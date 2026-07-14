package tw.taipei.veges.data.sync

import java.time.Instant
import java.time.LocalDate
import tw.taipei.veges.domain.Freshness
import tw.taipei.veges.domain.UnavailableReason

data class SyncStatus(
    val latestValidSourceDate: LocalDate?,
    val wholesaleFreshness: Freshness,
    val lastSuccessfulRefresh: Instant?,
    val lastAttemptedRefresh: Instant?,
    val lastFailure: UnavailableReason?,
)
