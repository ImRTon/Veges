package tw.taipei.veges.data.local

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.ProduceCategory
import tw.taipei.veges.domain.SourceDayState
import tw.taipei.veges.domain.SourceKind

@Dao
abstract class TaxonomyDao {
    @Transaction
    @Query(
        """
        SELECT * FROM taxonomy_concepts
        WHERE published = 1
          AND (
            normalizedHouseholdName LIKE :normalizedQuery
            OR stableId IN (
                SELECT conceptId FROM taxonomy_aliases
                WHERE normalizedAlias LIKE :normalizedQuery
            )
          )
        ORDER BY householdName
        """,
    )
    abstract fun observeSearch(normalizedQuery: String): Flow<List<TaxonomyConceptWithDetails>>

    @Transaction
    @Query(
        """
        SELECT * FROM taxonomy_concepts
        WHERE category = :category AND published = 1
        ORDER BY householdName
        """,
    )
    abstract fun observePublished(category: ProduceCategory): Flow<List<TaxonomyConceptWithDetails>>

    @Query(
        """
        SELECT taxonomy_concepts.* FROM taxonomy_concepts
        INNER JOIN tracked_concepts ON tracked_concepts.conceptId = taxonomy_concepts.stableId
        WHERE taxonomy_concepts.published = 1
        ORDER BY tracked_concepts.updatedAt DESC
        """,
    )
    abstract fun observeTrackedConcepts(): Flow<List<TaxonomyConceptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun replaceConcepts(concepts: List<TaxonomyConceptEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun replaceAliases(aliases: List<TaxonomyAliasEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun replaceVariants(variants: List<OfficialVariantEntity>)

    @Query("DELETE FROM taxonomy_concepts WHERE taxonomyVersion = :version")
    abstract suspend fun deleteVersion(version: String)

    @Transaction
    open suspend fun replaceVersion(
        version: String,
        concepts: List<TaxonomyConceptEntity>,
        aliases: List<TaxonomyAliasEntity>,
        variants: List<OfficialVariantEntity>,
    ) {
        deleteVersion(version)
        replaceConcepts(concepts)
        replaceAliases(aliases)
        replaceVariants(variants)
    }
}

@Dao
abstract class SourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun replaceObservations(observations: List<SourceObservationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun replaceDayStates(states: List<SourceDayStateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun replaceRun(run: SyncRunEntity)

    @Query("SELECT * FROM sync_runs WHERE runId = :runId")
    abstract suspend fun run(runId: String): SyncRunEntity?

    @Query(
        """
        SELECT * FROM source_observations
        WHERE commodityCode IN (:commodityCodes)
          AND market = :market
          AND state = 'VALID'
          AND observedOn BETWEEN :from AND :to
        ORDER BY observedOn
        """,
    )
    abstract suspend fun validObservations(
        commodityCodes: List<String>,
        market: MarketBasis,
        from: LocalDate,
        to: LocalDate,
    ): List<SourceObservationEntity>

    @Query(
        """
        SELECT * FROM source_day_states
        WHERE sourceKind = :sourceKind AND market = :market
        ORDER BY observedOn DESC
        """,
    )
    abstract fun observeDayStates(sourceKind: SourceKind, market: MarketBasis): Flow<List<SourceDayStateEntity>>

    @Query("SELECT MAX(observedOn) FROM source_observations WHERE state = 'VALID'")
    abstract suspend fun latestValidObservationDate(): LocalDate?

    @Query("SELECT MAX(retrievedAt) FROM source_observations WHERE state = 'VALID'")
    abstract suspend fun latestSuccessfulRefresh(): Instant?

    @Query("SELECT MAX(startedAt) FROM sync_runs")
    abstract suspend fun latestAttemptedRefresh(): Instant?

    @Query(
        """
        DELETE FROM source_observations
        WHERE observedOn < :cutoff
          AND state != 'VALID'
        """,
    )
    abstract suspend fun deleteNonValidBefore(cutoff: LocalDate): Int

    @Query(
        """
        DELETE FROM source_observations
        WHERE observedOn < :cutoff
          AND sourceKind = :sourceKind
        """,
    )
    abstract suspend fun deleteSourceBefore(cutoff: LocalDate, sourceKind: SourceKind): Int

    @Transaction
    open suspend fun publishImport(
        run: SyncRunEntity,
        observations: List<SourceObservationEntity>,
        dayStates: List<SourceDayStateEntity>,
    ) {
        replaceRun(run)
        replaceObservations(observations)
        replaceDayStates(dayStates)
    }
}

@Dao
abstract class EstimateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun replaceEstimate(estimate: EstimateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun replaceEstimates(estimates: List<EstimateEntity>)

    @Query(
        """
        SELECT * FROM estimates
        WHERE conceptId = :conceptId AND basis = :basis
        ORDER BY sourceDate DESC, calculatedAt DESC
        LIMIT 1
        """,
    )
    abstract suspend fun latest(conceptId: String, basis: MarketBasis): EstimateEntity?

    @Query(
        """
        SELECT * FROM estimates
        WHERE conceptId = :conceptId AND basis = :basis AND sourceDate >= :from
        ORDER BY sourceDate
        """,
    )
    abstract fun observeHistory(
        conceptId: String,
        basis: MarketBasis,
        from: LocalDate,
    ): Flow<List<EstimateEntity>>

    @Query(
        """
        SELECT estimates.* FROM estimates
        INNER JOIN tracked_concepts ON tracked_concepts.conceptId = estimates.conceptId
        WHERE estimates.estimateId IN (
            SELECT latest.estimateId FROM estimates latest
            WHERE latest.conceptId = estimates.conceptId AND latest.basis = estimates.basis
              AND latest.sourceDate = (
                  SELECT MAX(newer.sourceDate) FROM estimates newer
                  WHERE newer.conceptId = estimates.conceptId AND newer.basis = estimates.basis
              )
        )
        ORDER BY estimates.calculatedAt DESC
        """,
    )
    abstract fun observeTrackedLatest(): Flow<List<EstimateEntity>>
}

@Dao
abstract class AlertDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun replaceRule(rule: AlertRuleEntity)

    @Query("SELECT * FROM alert_rules WHERE ruleId = :ruleId")
    abstract suspend fun rule(ruleId: String): AlertRuleEntity?

    @Query("SELECT * FROM alert_rules ORDER BY updatedAt DESC")
    abstract fun observeAllRules(): Flow<List<AlertRuleEntity>>

    @Query(
        "SELECT * FROM alert_rules WHERE conceptId = :conceptId AND basis = :basis AND enabled = 1",
    )
    abstract suspend fun enabledRulesFor(conceptId: String, basis: MarketBasis): List<AlertRuleEntity>

    @Query("DELETE FROM alert_rules WHERE ruleId = :ruleId")
    abstract suspend fun delete(ruleId: String)

    @Query("SELECT * FROM alert_rules WHERE enabled = 1 ORDER BY updatedAt DESC")
    abstract fun observeEnabledRules(): Flow<List<AlertRuleEntity>>

    @Query("SELECT * FROM alert_rules WHERE conceptId = :conceptId ORDER BY updatedAt DESC")
    abstract fun observeRules(conceptId: String): Flow<List<AlertRuleEntity>>

    @Query(
        """
        UPDATE alert_rules
        SET conditionMet = :conditionMet,
            lastEvaluatedSourceDate = :sourceDate,
            lastTransitionEstimateId = :transitionEstimateId,
            updatedAt = :updatedAt
        WHERE ruleId = :ruleId
          AND enabled = 1
          AND (lastEvaluatedSourceDate IS NULL OR lastEvaluatedSourceDate < :sourceDate)
        """,
    )
    abstract suspend fun advanceEvaluation(
        ruleId: String,
        conditionMet: Boolean,
        sourceDate: LocalDate,
        transitionEstimateId: String?,
        updatedAt: Instant,
    ): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertNotificationEvent(event: NotificationEventEntity): Long

    @Transaction
    open suspend fun evaluateBelowThreshold(
        ruleId: String,
        estimate: EstimateEntity,
        now: Instant,
        notificationEvent: NotificationEventEntity?,
    ): Boolean {
        val current = rule(ruleId) ?: return false
        if (!current.enabled || estimate.unavailableReason != null || estimate.pointValue == null) return false
        if (current.basis != estimate.basis || current.conceptId != estimate.conceptId) return false
        if (current.lastEvaluatedSourceDate != null && estimate.sourceDate <= current.lastEvaluatedSourceDate) {
            return false
        }

        val isBelow = estimate.pointValue < current.thresholdNtdPerTaiJin
        val enteredMetState = !current.conditionMet && isBelow
        val changed = advanceEvaluation(
            ruleId = ruleId,
            conditionMet = isBelow,
            sourceDate = estimate.sourceDate,
            transitionEstimateId = if (enteredMetState) estimate.estimateId else null,
            updatedAt = now,
        )
        if (changed == 0) return false
        if (enteredMetState && notificationEvent != null) {
            insertNotificationEvent(notificationEvent)
        }
        return enteredMetState
    }
}

@Dao
abstract class TrackingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun replaceTrackedConcept(concept: TrackedConceptEntity)

    @Query("DELETE FROM tracked_concepts WHERE conceptId = :conceptId")
    abstract suspend fun deleteTrackedConcept(conceptId: String)

    @Query("SELECT COUNT(*) FROM alert_rules WHERE conceptId = :conceptId AND enabled = 1")
    abstract suspend fun activeAlertCount(conceptId: String): Int

    @Query("DELETE FROM alert_rules WHERE conceptId = :conceptId AND enabled = 1")
    abstract suspend fun deleteActiveAlerts(conceptId: String): Int

    @Query("SELECT * FROM tracked_concepts ORDER BY updatedAt DESC")
    abstract fun observeTracked(): Flow<List<TrackedConceptEntity>>
}
