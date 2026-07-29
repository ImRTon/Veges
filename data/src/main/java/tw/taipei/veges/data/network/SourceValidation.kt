package tw.taipei.veges.data.network

import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.PriceUnit
import tw.taipei.veges.domain.Provenance
import tw.taipei.veges.domain.ScaledPrice
import tw.taipei.veges.domain.SourceDayState
import tw.taipei.veges.domain.SourceKind
import tw.taipei.veges.domain.SourceObservation
import tw.taipei.veges.domain.OfficialCommodityCode

sealed interface WholesaleValidation {
    data class Valid(val observation: SourceObservation) : WholesaleValidation
    data class Closed(
        val market: MarketBasis,
        val observedOn: LocalDate,
        val kindCode: String,
    ) : WholesaleValidation
    data class OutOfScope(val marketCode: String) : WholesaleValidation
    data class Invalid(val reason: String) : WholesaleValidation
}

sealed interface RetailValidation {
    data class Valid(
        val itemName: String,
        val average: ScaledPrice,
    ) : RetailValidation

    data class Unavailable(val itemName: String) : RetailValidation
    data class Invalid(val reason: String) : RetailValidation
}

fun validateWholesale(
    dto: MoaWholesaleRecordDto,
    retrievedAt: java.time.Instant,
): WholesaleValidation {
    val market = when (dto.marketCode.trim()) {
        "104" -> MarketBasis.TAIPEI_SECOND
        "109" -> MarketBasis.TAIPEI_FIRST
        else -> return WholesaleValidation.OutOfScope(dto.marketCode)
    }
    val date = parseRocDate(dto.transactionDate)
        ?: return WholesaleValidation.Invalid("Invalid ROC transaction date: ${dto.transactionDate}")
    val upper = dto.upperPrice.decimalValue()
    val middle = dto.middlePrice.decimalValue()
    val lower = dto.lowerPrice.decimalValue()
    val average = dto.averagePrice.decimalValue()
    val volume = dto.volume.decimalValue()
    val closureIdentity =
        dto.cropCode.trim().equals("rest", ignoreCase = true) &&
            dto.cropName?.trim() == "休市"
    if (closureIdentity) {
        val kindCode = dto.kindCode?.trim()
        val hasExactZeroValues =
            listOf(upper, middle, lower, average, volume).all {
                it?.compareTo(BigDecimal.ZERO) == 0
            }
        if (kindCode !in OFFICIAL_CLOSURE_KIND_CODES || !hasExactZeroValues) {
            return WholesaleValidation.Invalid("Malformed official closure signal")
        }
        return WholesaleValidation.Closed(
            market = market,
            observedOn = date,
            kindCode = requireNotNull(kindCode),
        )
    }
    if (dto.cropCode.isBlank() || dto.cropName.isNullOrBlank()) {
        return WholesaleValidation.Invalid("Commodity code and name are required")
    }
    if (listOf(upper, middle, lower, average, volume).any { it == null || it <= BigDecimal.ZERO }) {
        return WholesaleValidation.Invalid("Price and volume must be positive numbers")
    }
    if (lower!! > middle!! || middle > upper!! || average!! < lower || average > upper) {
        return WholesaleValidation.Invalid("Wholesale price range is inconsistent")
    }

    return WholesaleValidation.Valid(
        SourceObservation(
            source = SourceKind.MOA_WHOLESALE,
            market = market,
            commodityCode = OfficialCommodityCode(dto.cropCode),
            officialName = dto.cropName,
            observedOn = date,
            lowerPrice = ScaledPrice(lower, PriceUnit.NTD_PER_KILOGRAM),
            averagePrice = ScaledPrice(average, PriceUnit.NTD_PER_KILOGRAM),
            upperPrice = ScaledPrice(upper, PriceUnit.NTD_PER_KILOGRAM),
            volume = volume,
            state = SourceDayState.VALID,
            provenance = Provenance(
                source = SourceKind.MOA_WHOLESALE,
                attribution = OfficialEndpoints.moaAttribution,
                sourceUrl = OfficialEndpoints.moaWholesale,
                retrievedAt = retrievedAt,
            ),
        ),
    )
}

private val OFFICIAL_CLOSURE_KIND_CODES = setOf("N04", "N06")

fun validateTaipeiRetail(dto: TaipeiRetailRecordDto): RetailValidation {
    if (dto.countyCode != "63000" || dto.countyName != "臺北市") {
        return RetailValidation.Invalid("Unexpected county: ${dto.countyName}/${dto.countyCode}")
    }
    if (dto.itemName.isBlank()) return RetailValidation.Invalid("Retail item name is blank")
    if (dto.averageNtdPerTaiJin.trim() == "-") return RetailValidation.Unavailable(dto.itemName)
    val value = runCatching { BigDecimal(dto.averageNtdPerTaiJin.trim()) }.getOrNull()
        ?: return RetailValidation.Invalid("Invalid retail average: ${dto.averageNtdPerTaiJin}")
    if (value <= BigDecimal.ZERO) return RetailValidation.Invalid("Retail average must be positive")
    return RetailValidation.Valid(
        itemName = dto.itemName,
        average = ScaledPrice(value, PriceUnit.NTD_PER_TAI_JIN),
    )
}

fun classifySourceDay(
    hasAnyRecords: Boolean,
    hasInvalidRecords: Boolean,
    officialClosureConfirmed: Boolean,
): SourceDayState = when {
    officialClosureConfirmed -> SourceDayState.CLOSED
    hasInvalidRecords -> SourceDayState.INVALID
    hasAnyRecords -> SourceDayState.VALID
    else -> SourceDayState.MISSING
}

fun parseRocDate(value: String): LocalDate? {
    val parts = value.trim().split('.')
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    return runCatching { LocalDate.of(year + 1911, month, day) }.getOrNull()
}
