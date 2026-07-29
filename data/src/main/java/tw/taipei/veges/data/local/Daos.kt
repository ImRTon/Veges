package tw.taipei.veges.data.local

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
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

    @Transaction
    @Query(
        """
        SELECT * FROM taxonomy_concepts
        WHERE published = 1
        ORDER BY stableId
        """,
    )
    abstract suspend fun publishedConceptsWithDetails(): List<TaxonomyConceptWithDetails>

    @Transaction
    @Query(
        """
        SELECT * FROM taxonomy_concepts
        WHERE stableId = :conceptId AND published = 1
        LIMIT 1
        """,
    )
    abstract suspend fun publishedConceptWithDetails(conceptId: String): TaxonomyConceptWithDetails?

    @Query(
        """
        SELECT COUNT(DISTINCT official_variants.commodityCode)
        FROM official_variants
        INNER JOIN taxonomy_concepts
          ON taxonomy_concepts.stableId = official_variants.conceptId
        WHERE taxonomy_concepts.published = 1
          AND taxonomy_concepts.category = 'VEGETABLE'
        """,
    )
    abstract suspend fun publishedVegetableCommodityCodeCount(): Int

    @Transaction
    @Query(
        """
        SELECT * FROM taxonomy_concepts
        WHERE stableId = :conceptId AND published = 1
        LIMIT 1
        """,
    )
    abstract fun observePublishedConcept(conceptId: String): Flow<TaxonomyConceptWithDetails?>

    @Upsert
    abstract suspend fun upsertConcepts(concepts: List<TaxonomyConceptEntity>)

    open suspend fun replaceConcepts(concepts: List<TaxonomyConceptEntity>) =
        upsertConcepts(concepts)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun replaceAliases(aliases: List<TaxonomyAliasEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun replaceVariants(variants: List<OfficialVariantEntity>)

    @Query("DELETE FROM taxonomy_aliases WHERE conceptId IN (:conceptIds)")
    abstract suspend fun deleteAliases(conceptIds: List<String>)

    @Query("DELETE FROM official_variants WHERE conceptId IN (:conceptIds)")
    abstract suspend fun deleteVariants(conceptIds: List<String>)

    @Query("DELETE FROM taxonomy_concepts WHERE stableId NOT IN (:conceptIds)")
    abstract suspend fun deleteConceptsMissingFrom(conceptIds: List<String>)

    @Transaction
    open suspend fun replaceVersion(
        version: String,
        concepts: List<TaxonomyConceptEntity>,
        aliases: List<TaxonomyAliasEntity>,
        variants: List<OfficialVariantEntity>,
    ) {
        require(concepts.isNotEmpty()) { "Taxonomy import must contain at least one concept" }
        require(concepts.all { it.taxonomyVersion == version }) {
            "Every concept must match the imported taxonomy version"
        }
        val conceptIds = concepts.map { it.stableId }
        upsertConcepts(concepts)
        deleteAliases(conceptIds)
        deleteVariants(conceptIds)
        replaceAliases(aliases)
        replaceVariants(variants)
        deleteConceptsMissingFrom(conceptIds)
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
        SELECT * FROM source_observations
        WHERE commodityCode IN (:commodityCodes)
          AND market = :market
          AND sourceKind = 'MOA_WHOLESALE'
          AND state = 'VALID'
        ORDER BY observedOn DESC, commodityCode
        """,
    )
    abstract suspend fun validWholesaleObservations(
        commodityCodes: List<String>,
        market: MarketBasis,
    ): List<SourceObservationEntity>

    @Query(
        """
        SELECT * FROM source_observations
        WHERE commodityCode IN (:commodityCodes)
          AND sourceKind = 'MOA_WHOLESALE'
          AND state = 'VALID'
          AND observedOn >= :from
        ORDER BY observedOn, market, commodityCode
        """,
    )
    abstract fun observeValidWholesaleObservations(
        commodityCodes: List<String>,
        from: LocalDate,
    ): Flow<List<SourceObservationEntity>>

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

    @Query(
        """
        SELECT MIN(observedOn) FROM source_observations
        WHERE state = 'VALID' AND sourceKind = 'MOA_WHOLESALE'
        """,
    )
    abstract suspend fun earliestValidWholesaleObservationDate(): LocalDate?

    @Query(
        """
        SELECT MIN(observedOn) FROM source_observations
        WHERE commodityCode IN (:commodityCodes)
          AND state = 'VALID'
          AND sourceKind = 'MOA_WHOLESALE'
        """,
    )
    abstract suspend fun earliestValidWholesaleObservationDate(
        commodityCodes: List<String>,
    ): LocalDate?

    @Query(
        """
        SELECT COUNT(DISTINCT source_observations.commodityCode)
        FROM source_observations
        INNER JOIN official_variants
          ON official_variants.commodityCode = source_observations.commodityCode
         AND official_variants.market = source_observations.market
        INNER JOIN taxonomy_concepts
          ON taxonomy_concepts.stableId = official_variants.conceptId
        WHERE source_observations.state = 'VALID'
          AND source_observations.sourceKind = 'MOA_WHOLESALE'
          AND source_observations.observedOn >= :from
          AND taxonomy_concepts.published = 1
          AND taxonomy_concepts.category = 'VEGETABLE'
        """,
    )
    abstract suspend fun recentObservedPublishedVegetableCodeCount(from: LocalDate): Int

    @Query(
        """
        SELECT COUNT(DISTINCT source_observations.observedOn)
        FROM source_observations
        INNER JOIN official_variants
          ON official_variants.commodityCode = source_observations.commodityCode
         AND official_variants.market = source_observations.market
        INNER JOIN taxonomy_concepts
          ON taxonomy_concepts.stableId = official_variants.conceptId
        WHERE source_observations.state = 'VALID'
          AND source_observations.sourceKind = 'MOA_WHOLESALE'
          AND source_observations.observedOn >= :from
          AND taxonomy_concepts.published = 1
          AND taxonomy_concepts.category = 'VEGETABLE'
        """,
    )
    abstract suspend fun recentPublishedVegetableTradingDayCount(from: LocalDate): Int

    @Query(
        """
        SELECT MAX(observedOn) FROM source_observations
        WHERE state = 'VALID'
          AND sourceKind = :sourceKind
          AND market = :market
          AND observedOn < :before
        """,
    )
    abstract suspend fun latestValidObservationDateBefore(
        sourceKind: SourceKind,
        market: MarketBasis,
        before: LocalDate,
    ): LocalDate?

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
        WHERE conceptId = :conceptId AND basis = :basis
        ORDER BY sourceDate DESC, calculatedAt DESC
        LIMIT 1
        """,
    )
    abstract fun observeLatest(conceptId: String, basis: MarketBasis): Flow<EstimateEntity?>

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

    @Query(
        """
        SELECT estimates.* FROM estimates
        INNER JOIN tracked_concepts ON tracked_concepts.conceptId = estimates.conceptId
        ORDER BY estimates.conceptId, estimates.sourceDate DESC, estimates.calculatedAt DESC
        """,
    )
    abstract fun observeTrackedHistory(): Flow<List<EstimateEntity>>

    @Query(
        """
        SELECT estimates.* FROM estimates
        INNER JOIN taxonomy_concepts
          ON taxonomy_concepts.stableId = estimates.conceptId
        WHERE taxonomy_concepts.published = 1
          AND taxonomy_concepts.category = :category
        ORDER BY estimates.conceptId, estimates.sourceDate DESC, estimates.calculatedAt DESC
        """,
    )
    abstract fun observePublishedHistory(category: ProduceCategory): Flow<List<EstimateEntity>>
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

    @Query(
        """
        SELECT notification_events.eventId AS eventId,
               notification_events.conceptId AS conceptId,
               taxonomy_concepts.householdName AS householdName,
               notification_events.basis AS basis,
               estimates.pointValue AS estimateNtdPerTaiJin,
               alert_rules.thresholdNtdPerTaiJin AS thresholdNtdPerTaiJin,
               notification_events.sourceDate AS sourceDate
        FROM notification_events
        INNER JOIN alert_rules ON alert_rules.ruleId = notification_events.ruleId
        INNER JOIN estimates ON estimates.estimateId = notification_events.estimateId
        INNER JOIN taxonomy_concepts ON taxonomy_concepts.stableId = notification_events.conceptId
        WHERE notification_events.deliveredAt IS NULL
          AND estimates.pointValue IS NOT NULL
          AND estimates.unavailableReason IS NULL
        ORDER BY notification_events.createdAt, notification_events.eventId
        LIMIT :limit
        """,
    )
    abstract suspend fun pendingNotificationDeliveries(limit: Int): List<PendingNotificationDelivery>

    @Query(
        """
        UPDATE notification_events
        SET deliveredAt = :deliveredAt
        WHERE eventId = :eventId AND deliveredAt IS NULL
        """,
    )
    abstract suspend fun markNotificationDelivered(eventId: String, deliveredAt: Instant): Int

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

    @Query("SELECT EXISTS(SELECT 1 FROM tracked_concepts WHERE conceptId = :conceptId)")
    abstract fun observeIsTracked(conceptId: String): Flow<Boolean>
}
