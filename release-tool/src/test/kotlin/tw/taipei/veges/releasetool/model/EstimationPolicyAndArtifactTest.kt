package tw.taipei.veges.releasetool.model

import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.taipei.veges.domain.BacktestMetrics
import tw.taipei.veges.domain.CALIBRATED_ESTIMATOR_FAMILIES
import tw.taipei.veges.domain.EstimatorFamily
import tw.taipei.veges.releasetool.artifact.ArtifactCodec

class EstimationPolicyAndArtifactTest {
    @Test
    fun approvedThresholdsPassAtBoundaryAndKeepUncertaintyDisabled() {
        val gate = EstimationGateEvaluator.evaluate(
            policy = policy(),
            pairedCalibrationPeriods = 18,
            retailTargetPeriods = 20,
            calibrationCutoff = LocalDate.parse("2026-06-11"),
            generatedOn = LocalDate.parse("2026-07-26"),
            metrics = metrics(mae = "15.00", rmse = "22.00"),
        )

        assertTrue(gate.pointEligible)
        assertFalse(gate.intervalEligible)
        assertFalse(gate.confidenceEligible)
        assertEquals("0.9", gate.coverageRatio)
        assertTrue(gate.exclusionReasons.isEmpty())
    }

    @Test
    fun eachFailedGateProducesAnAuditableReason() {
        val gate = EstimationGateEvaluator.evaluate(
            policy = policy(),
            pairedCalibrationPeriods = 17,
            retailTargetPeriods = 20,
            calibrationCutoff = LocalDate.parse("2026-06-10"),
            generatedOn = LocalDate.parse("2026-07-26"),
            metrics = metrics(mae = "15.01", rmse = "22.01"),
        )

        assertFalse(gate.pointEligible)
        assertEquals(
            setOf(
                "CALIBRATION_TOO_OLD_AT_GENERATION",
                "INSUFFICIENT_PAIRED_CALIBRATION_PERIODS",
                "MAE_ABOVE_MAXIMUM",
                "PAIRED_COVERAGE_BELOW_MINIMUM",
                "RMSE_ABOVE_MAXIMUM",
            ),
            gate.exclusionReasons.toSet(),
        )
    }

    @Test
    fun modelArtifactSelectsLowestRmseEligibleFamilyDeterministically() {
        val report = report(
            candidates = CALIBRATED_ESTIMATOR_FAMILIES.map { family ->
                candidate(
                    family = family,
                    rmse = when (family) {
                        EstimatorFamily.LINEAR_CALIBRATION -> "12"
                        EstimatorFamily.LOG_LINEAR_CALIBRATION -> "10"
                        EstimatorFamily.SEASONAL_BASELINE -> "14"
                        EstimatorFamily.WHOLESALE_MULTIPLIER_REFERENCE ->
                            error("Temporary multiplier is not a calibration candidate")
                    },
                )
            },
        )

        val first = ModelArtifactGenerator.generate(report, policy(), "mvp-2026-07-26")
        val second = ModelArtifactGenerator.generate(
            report.copy(result = report.result.reversed()),
            policy(),
            "mvp-2026-07-26",
        )

        ModelArtifactGenerator.validate(first)
        assertEquals(EstimatorFamily.LOG_LINEAR_CALIBRATION.name, first.entries.single().family)
        assertEquals(first.artifactChecksum, second.artifactChecksum)
        assertEquals(ArtifactCodec.encodeModelArtifact(first), ArtifactCodec.encodeModelArtifact(second))
    }

    @Test
    fun missingFamilySpecificEvidenceExcludesConceptBasis() {
        val report = report(
            candidates = listOf(candidate(EstimatorFamily.SEASONAL_BASELINE, rmse = "10")),
        )

        val artifact = ModelArtifactGenerator.generate(report, policy(), "mvp-2026-07-26")

        assertTrue(artifact.entries.isEmpty())
        assertEquals("MISSING_FAMILY_SPECIFIC_BACKTEST", artifact.exclusions.single().reasonCodes.single())
    }

    @Test
    fun changedArtifactContentInvalidatesChecksum() {
        val report = report(
            candidates = CALIBRATED_ESTIMATOR_FAMILIES.map { candidate(it, rmse = "10") },
        )
        val valid = ModelArtifactGenerator.generate(report, policy(), "mvp-2026-07-26")
        val changed = valid.copy(artifactVersion = "changed")

        assertNotEquals(changed.artifactChecksum, ArtifactCodec.modelArtifactChecksum(changed))
        assertTrue(runCatching { ModelArtifactGenerator.validate(changed) }.isFailure)
    }

    @Test
    fun bundledAuditAndArtifactRemainFailClosedAndChecksummed() {
        val audit = resourceJson<HistoricalAuditReport>("model/historical-audit-2026-07-26.json")
        val artifact = resourceJson<ModelArtifactBundle>("model/mvp-model-artifact.json")

        assertEquals(45, audit.result.size)
        audit.result
            .groupBy { it.conceptId to it.basis }
            .values
            .forEach { candidates ->
                assertEquals(CALIBRATED_ESTIMATOR_FAMILIES.map { it.name }.toSet(), candidates.map { it.family }.toSet())
            }
        assertTrue(audit.result.none { it.gate.pointEligible })
        assertTrue(artifact.entries.isEmpty())
        assertEquals(15, artifact.exclusions.size)
        assertTrue(artifact.exclusions.all { it.reasonCodes.isNotEmpty() })
        assertEquals("2025-12-01", artifact.dataCutoff)
        ModelArtifactGenerator.validate(artifact)
    }

    private inline fun <reified T> resourceJson(path: String): T {
        val text = requireNotNull(javaClass.classLoader.getResource(path)) {
            "Missing test resource $path"
        }.readText()
        return ArtifactCodec.strictJson.decodeFromString(text)
    }

    private fun candidate(
        family: EstimatorFamily,
        rmse: String,
    ): HistoricalConceptAudit {
        val metrics = metrics(mae = "8", rmse = rmse)
        val gate = EstimationGateEvaluator.evaluate(
            policy = policy(),
            pairedCalibrationPeriods = 18,
            retailTargetPeriods = 18,
            calibrationCutoff = LocalDate.parse("2026-07-01"),
            generatedOn = LocalDate.parse("2026-07-26"),
            metrics = metrics,
        )
        return HistoricalConceptAudit(
            conceptId = "vegetable.small-bok-choy",
            basis = "TAIPEI_FIRST",
            family = family.name,
            wholesaleValidDays = 586,
            retailPeriods = 18,
            calibrationRows = 18,
            backtestPeriods = 16,
            calibrationCutoff = "2026-07-01",
            meanAbsoluteError = metrics.meanAbsoluteError?.toPlainString(),
            rootMeanSquaredError = metrics.rootMeanSquaredError?.toPlainString(),
            fittedParameters = when (family) {
                EstimatorFamily.LINEAR_CALIBRATION ->
                    mapOf("intercept" to "20", "wholesaleAverageSlope" to "0.8")
                EstimatorFamily.LOG_LINEAR_CALIBRATION ->
                    mapOf("logIntercept" to "1", "logWholesaleAverageSlope" to "0.8")
                EstimatorFamily.SEASONAL_BASELINE ->
                    mapOf("medianRetailNtdPerTaiJin" to "52")
                EstimatorFamily.WHOLESALE_MULTIPLIER_REFERENCE ->
                    error("Temporary multiplier is not a calibration candidate")
            },
            gate = gate,
            exclusions = emptyList(),
        )
    }

    private fun report(candidates: List<HistoricalConceptAudit>) = HistoricalAuditReport(
        schemaVersion = 1,
        generatedAt = "2026-07-26T00:00:00Z",
        wholesaleFileCount = 1,
        retailFileCount = 1,
        wholesaleFiles = listOf(SourceFileProvenance("wholesale.json", "a".repeat(64), "MOA")),
        retailFiles = listOf(SourceFileProvenance("retail.csv", "b".repeat(64), "Taipei")),
        result = candidates,
        thresholdStatus = "APPROVED_POLICY_ENFORCED",
        notes = emptyList(),
    )

    private fun metrics(
        mae: String,
        rmse: String,
    ) = BacktestMetrics(
        periods = emptyList(),
        meanAbsoluteError = BigDecimal(mae),
        rootMeanSquaredError = BigDecimal(rmse),
        intervalCoverage = null,
        meanIntervalWidth = null,
    )

    private fun policy() = EstimationPublicationPolicy(
        schemaVersion = 1,
        policyVersion = "catalog-publication-2026-07-26",
        approvedAt = "2026-07-26",
        approvedBy = "product-owner",
        minimumValidObservationDays = 30,
        minimumPairedCalibrationPeriods = 18,
        minimumCoverageRatio = "0.90",
        maximumRecencyDays = 45,
        maximumMeanAbsoluteErrorNtdPerTaiJin = "15.00",
        maximumRootMeanSquaredErrorNtdPerTaiJin = "22.00",
        wholesaleFreshnessHours = 36,
        calibrationArtifactLifetimeDays = 62,
        intervalEnabled = false,
        confidenceEnabled = false,
        publishCandidates = false,
        requireExplicitAmbiguityChoice = true,
        estimateDisclosureShortTag = "估算",
        estimateDisclosureFullLabel = "Taipei retail reference estimate",
        notes = "Approved test policy",
    )

    @Test
    fun temporaryFactorArtifactIsDeterministicAndRejectsMutation() {
        val policy = TemporaryFactorPolicy(
            schemaVersion = 1,
            policyVersion = "temporary-wholesale-x2-policy-2026-07-26",
            approvedAt = "2026-07-26",
            approvedBy = "product-owner",
            wholesaleToMarketFactor = "2.0",
            kilogramsPerTaiJin = "0.6",
            wholesaleFreshnessHours = 36,
            supportedBases = listOf("TAIPEI_SECOND", "TAIPEI_COMBINED", "TAIPEI_FIRST"),
            intervalEnabled = false,
            confidenceEnabled = false,
            disclosure = "估算：批發價換算成每台斤後 × 2 的粗略市場參考；不是零售實價。",
        )

        val first = TemporaryFactorArtifactGenerator.generate(
            policy,
            "temporary-wholesale-x2-2026-07-26",
            "2026-07-26T00:00:00Z",
        )
        val second = TemporaryFactorArtifactGenerator.generate(
            policy.copy(supportedBases = policy.supportedBases.reversed()),
            "temporary-wholesale-x2-2026-07-26",
            "2026-07-26T00:00:00Z",
        )

        TemporaryFactorArtifactGenerator.validate(first)
        assertEquals(first.artifactChecksum, second.artifactChecksum)
        assertEquals("2.0", first.wholesaleToMarketFactor)
        assertEquals("0.6", first.kilogramsPerTaiJin)
        assertTrue(
            runCatching {
                TemporaryFactorArtifactGenerator.validate(first.copy(wholesaleToMarketFactor = "2.1"))
            }.isFailure,
        )
    }

    @Test
    fun bundledTemporaryFactorArtifactIsApprovedAndChecksummed() {
        val artifact = resourceJson<TemporaryFactorArtifact>(
            "model/mvp-temporary-factor-artifact.json",
        )

        TemporaryFactorArtifactGenerator.validate(artifact)
        assertEquals("WHOLESALE_MULTIPLIER_REFERENCE", artifact.family)
        assertEquals("2.0", artifact.wholesaleToMarketFactor)
        assertEquals("0.6", artifact.kilogramsPerTaiJin)
        assertEquals("2026-07-26", artifact.approvedAt)
        assertFalse(artifact.intervalEnabled)
        assertFalse(artifact.confidenceEnabled)
    }
}
