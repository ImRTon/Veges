package tw.taipei.veges.data.repository

import java.time.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tw.taipei.veges.data.local.EstimateEntity
import tw.taipei.veges.domain.Estimate
import tw.taipei.veges.domain.EstimateDisclosure
import tw.taipei.veges.domain.ProduceConceptId
import tw.taipei.veges.domain.ScaledPrice

private val datesJson = Json { explicitNulls = false }

fun Estimate.toEntity(estimateId: String, pairedCalibrationPeriods: Int): EstimateEntity {
    val pointValue = point?.amount
    return EstimateEntity(
        estimateId = estimateId,
        conceptId = conceptId.value,
        basis = basis,
        modelVersion = modelVersion,
        sourceDate = wholesaleSourceDates.maxOrNull() ?: calibrationCutoff,
        sourceDatesJson = datesJson.encodeToString(wholesaleSourceDates.map(LocalDate::toString)),
        calibrationCutoff = calibrationCutoff,
        estimatorApprovedOn = estimatorApprovedOn,
        formula = formula,
        pairedCalibrationPeriods = pairedCalibrationPeriods,
        calculatedAt = calculatedAt,
        pointValue = pointValue,
        pointUnit = point?.unit,
        intervalLower = intervalLower?.amount,
        intervalUpper = intervalUpper?.amount,
        confidence = confidence,
        unavailableReason = unavailableReason,
        disclosureShortTag = disclosure.shortTag,
        disclosureFullLabel = disclosure.fullLabel,
    )
}

fun EstimateEntity.toDomain(): Estimate {
    val dates = runCatching { datesJson.decodeFromString<List<String>>(sourceDatesJson) }
        .getOrDefault(listOf(sourceDate.toString()))
        .map(LocalDate::parse)
    return Estimate(
        conceptId = ProduceConceptId(conceptId),
        basis = basis,
        modelVersion = modelVersion,
        wholesaleSourceDates = dates,
        calibrationCutoff = calibrationCutoff,
        estimatorApprovedOn = estimatorApprovedOn,
        formula = formula,
        calculatedAt = calculatedAt,
        point = pointValue?.let { ScaledPrice(it, requireNotNull(pointUnit)) },
        intervalLower = intervalLower?.let { ScaledPrice(it, requireNotNull(pointUnit)) },
        intervalUpper = intervalUpper?.let { ScaledPrice(it, requireNotNull(pointUnit)) },
        confidence = confidence,
        unavailableReason = unavailableReason,
        disclosure = EstimateDisclosure(disclosureShortTag, disclosureFullLabel),
    )
}
