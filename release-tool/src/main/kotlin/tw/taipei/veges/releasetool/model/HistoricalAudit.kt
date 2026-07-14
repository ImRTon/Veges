package tw.taipei.veges.releasetool.model

import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import tw.taipei.veges.domain.CalibrationTarget
import tw.taipei.veges.domain.EstimatorFamily
import tw.taipei.veges.domain.EstimatorFeatureSet
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.MarketWholesaleInput
import tw.taipei.veges.domain.combineTaipeiWholesale
import tw.taipei.veges.releasetool.artifact.ArtifactCodec
import tw.taipei.veges.releasetool.artifact.OfficialMappingArtifact
import tw.taipei.veges.releasetool.artifact.TaxonomyArtifact
import tw.taipei.veges.releasetool.audit.ArtifactValidator

@Serializable
data class RetailCalibrationMappingsArtifact(
    val schemaVersion: Int,
    val mappings: List<RetailCalibrationMapping>,
)

@Serializable
data class RetailCalibrationMapping(
    val conceptId: String,
    val retailItemNames: List<String>,
)

@Serializable
data class HistoricalAuditReport(
    val schemaVersion: Int,
    val generatedAt: String,
    val wholesaleFileCount: Int,
    val retailFileCount: Int,
    val wholesaleFiles: List<SourceFileProvenance>,
    val retailFiles: List<SourceFileProvenance>,
    val result: List<HistoricalConceptAudit>,
    val thresholdStatus: String,
    val notes: List<String>,
)

@Serializable
data class SourceFileProvenance(
    val fileName: String,
    val sha256: String,
    val source: String,
)

@Serializable
data class HistoricalConceptAudit(
    val conceptId: String,
    val basis: String,
    val wholesaleValidDays: Int,
    val retailPeriods: Int,
    val calibrationRows: Int,
    val backtestPeriods: Int,
    val meanAbsoluteError: String?,
    val rootMeanSquaredError: String?,
    val exclusions: List<String>,
)

@Serializable
private data class MoaHistoricalRecord(
    @SerialName("交易日期") val transactionDate: String,
    @SerialName("種類代碼") val kindCode: String,
    @SerialName("作物代號") val cropCode: String,
    @SerialName("作物名稱") val cropName: String,
    @SerialName("市場代號") val marketCode: String,
    @SerialName("市場名稱") val marketName: String,
    @SerialName("上價") val upperPrice: kotlinx.serialization.json.JsonElement,
    @SerialName("中價") val middlePrice: kotlinx.serialization.json.JsonElement,
    @SerialName("下價") val lowerPrice: kotlinx.serialization.json.JsonElement,
    @SerialName("平均價") val averagePrice: kotlinx.serialization.json.JsonElement,
    @SerialName("交易量") val volume: kotlinx.serialization.json.JsonElement,
)

private data class WholesaleRow(
    val observedOn: LocalDate,
    val cropCode: String,
    val market: MarketBasis,
    val average: BigDecimal,
    val lower: BigDecimal,
    val upper: BigDecimal,
    val volume: BigDecimal,
)

private data class RetailRow(
    val observedOn: LocalDate,
    val itemName: String,
    val average: BigDecimal?,
)

class HistoricalAuditRunner {
    fun run(
        taxonomy: TaxonomyArtifact,
        mappings: RetailCalibrationMappingsArtifact,
        wholesaleFiles: List<Path>,
        retailFiles: List<Path>,
        generatedAt: String,
    ): HistoricalAuditReport {
        ArtifactValidator.validateTaxonomy(taxonomy)
        require(mappings.schemaVersion == 1) { "Unsupported retail calibration mapping schema" }
        val conceptIds = taxonomy.concepts.map { it.stableId }.toSet()
        require(mappings.mappings.all { it.conceptId in conceptIds }) { "Retail mapping references unknown concept" }

        val wholesale = wholesaleFiles.flatMap(::readWholesale)
        val retail = retailFiles.flatMap(::readRetail)
        val retailByConcept = mappings.mappings.associate { mapping ->
            mapping.conceptId to retail.filter { it.itemName in mapping.retailItemNames }
        }
        val results = taxonomy.concepts.sortedBy { it.stableId }.flatMap { concept ->
            listOf(MarketBasis.TAIPEI_COMBINED, MarketBasis.TAIPEI_FIRST, MarketBasis.TAIPEI_SECOND).map { basis ->
                auditConcept(concept.stableId, concept.officialMappings, basis, wholesale, retailByConcept[concept.stableId].orEmpty())
            }
        }

        return HistoricalAuditReport(
            schemaVersion = 1,
            generatedAt = generatedAt,
            wholesaleFileCount = wholesaleFiles.size,
            retailFileCount = retailFiles.size,
            wholesaleFiles = wholesaleFiles.map { provenance(it, "MOA wholesale historical API") },
            retailFiles = retailFiles.map { provenance(it, "Taipei public retail-market CSV") },
            result = results,
            thresholdStatus = "PENDING_USER_APPROVAL",
            notes = listOf(
                "This report compares candidate estimators; it does not publish a model artifact.",
                "The approved minimum calibration history is 30 valid observation days.",
                "Coverage, recency, point-error, interval, confidence, and staleness thresholds remain pending audit approval.",
                "All model-derived prices must carry the 估算 tag and Taipei retail reference estimate label.",
            ),
        )
    }

    private fun auditConcept(
        conceptId: String,
        mappings: List<OfficialMappingArtifact>,
        basis: MarketBasis,
        wholesale: List<WholesaleRow>,
        retail: List<RetailRow>,
    ): HistoricalConceptAudit {
        val sourceInputs = wholesaleInputs(mappings, basis, wholesale)
        val retailByMonth = retail.associateBy { YearMonth.from(it.observedOn) }
        val rows = sourceInputs.mapNotNull { (month, input) ->
            val target = retailByMonth[month]?.average ?: return@mapNotNull null
            HistoricalCalibrationRow(
                observedOn = month.atDay(1),
                features = EstimatorFeatureSet(
                    wholesaleAverageNtdPerKg = input.averageNtdPerKg,
                    wholesaleLowerNtdPerKg = input.lowerNtdPerKg,
                    wholesaleUpperNtdPerKg = input.upperNtdPerKg,
                    transactionVolumeKg = input.volumeKg,
                    monthOfYear = month.monthValue,
                    marketBasis = basis,
                ),
                target = CalibrationTarget(month.atDay(1), target),
            )
        }
        val exclusions = mutableListOf<String>()
        if (sourceInputs.isEmpty()) exclusions += "NO_VALID_WHOLESALE_INPUT"
        if (retailByMonth.isEmpty()) exclusions += "NO_RETAIL_CALIBRATION_ROWS"
        val backtest = if (rows.size >= 2) {
            ExpandingWindowBacktest().run(
                rows = rows,
                family = EstimatorFamily.SEASONAL_BASELINE,
                basis = basis,
                config = BacktestConfig(minimumTrainingRows = 1),
            )
        } else {
            exclusions += "INSUFFICIENT_ALIGNED_CALIBRATION_ROWS"
            null
        }
        backtest?.exclusions?.let(exclusions::addAll)
        return HistoricalConceptAudit(
            conceptId = conceptId,
            basis = basis.name,
            wholesaleValidDays = sourceInputs.values.sumOf { it.validDays },
            retailPeriods = retailByMonth.size,
            calibrationRows = rows.size,
            backtestPeriods = backtest?.metrics?.periods?.size ?: 0,
            meanAbsoluteError = backtest?.metrics?.meanAbsoluteError?.toPlainString(),
            rootMeanSquaredError = backtest?.metrics?.rootMeanSquaredError?.toPlainString(),
            exclusions = exclusions.distinct().sorted(),
        )
    }

    private fun wholesaleInputs(
        mappings: List<OfficialMappingArtifact>,
        basis: MarketBasis,
        wholesale: List<WholesaleRow>,
    ): Map<YearMonth, MonthlyInput> {
        val rowsByDate = wholesale.groupBy { it.observedOn }
        val daily = rowsByDate.mapNotNull { (date, rows) ->
            val first = aggregateMarket(rows, mappings, MarketBasis.TAIPEI_FIRST)
            val second = aggregateMarket(rows, mappings, MarketBasis.TAIPEI_SECOND)
            val input = when (basis) {
                MarketBasis.TAIPEI_FIRST -> first
                MarketBasis.TAIPEI_SECOND -> second
                MarketBasis.TAIPEI_COMBINED -> when (val combined = combineTaipeiWholesale(first, second)) {
                    is tw.taipei.veges.domain.CombinedWholesaleResult.Success -> MarketWholesaleInput(
                        market = MarketBasis.TAIPEI_COMBINED,
                        observedOn = combined.observedOn,
                        averageNtdPerKg = combined.averageNtdPerKg,
                        lowerNtdPerKg = combined.lowerNtdPerKg,
                        upperNtdPerKg = combined.upperNtdPerKg,
                        volumeKg = combined.volumeKg,
                    )
                    is tw.taipei.veges.domain.CombinedWholesaleResult.Unavailable -> null
                }
            }
            input?.let { date to it }
        }
        return daily.groupBy { YearMonth.from(it.first) }.mapValues { (_, values) ->
            val totalVolume = values.sumOf { it.second.volumeKg }
            MonthlyInput(
                averageNtdPerKg = weighted(values.map { it.second.averageNtdPerKg to it.second.volumeKg }, totalVolume),
                lowerNtdPerKg = weighted(values.map { it.second.lowerNtdPerKg to it.second.volumeKg }, totalVolume),
                upperNtdPerKg = weighted(values.map { it.second.upperNtdPerKg to it.second.volumeKg }, totalVolume),
                volumeKg = totalVolume,
                validDays = values.size,
            )
        }
    }

    private fun aggregateMarket(
        rows: List<WholesaleRow>,
        mappings: List<OfficialMappingArtifact>,
        market: MarketBasis,
    ): MarketWholesaleInput? {
        val allowedCodes = mappings.filter { it.market == market.name }.map { it.commodityCode }.toSet()
        val matching = rows.filter { it.market == market && it.cropCode in allowedCodes }
        if (matching.isEmpty()) return null
        val totalVolume = matching.sumOf { it.volume }
        return MarketWholesaleInput(
            market = market,
            observedOn = matching.first().observedOn,
            averageNtdPerKg = weighted(matching.map { it.average to it.volume }, totalVolume),
            lowerNtdPerKg = weighted(matching.map { it.lower to it.volume }, totalVolume),
            upperNtdPerKg = weighted(matching.map { it.upper to it.volume }, totalVolume),
            volumeKg = totalVolume,
        )
    }

    private fun weighted(values: List<Pair<BigDecimal, BigDecimal>>, total: BigDecimal): BigDecimal =
        values.fold(BigDecimal.ZERO) { result, (value, weight) -> result + value * weight }
            .divide(total, 8, java.math.RoundingMode.HALF_UP)

    private fun readWholesale(path: Path): List<WholesaleRow> {
        val records = ArtifactCodec.strictJson.decodeFromString<List<MoaHistoricalRecord>>(Files.readString(path))
        return records.mapNotNull { record ->
            val date = parseRocDate(record.transactionDate) ?: return@mapNotNull null
            val market = when (record.marketCode) {
                "109" -> MarketBasis.TAIPEI_FIRST
                "104" -> MarketBasis.TAIPEI_SECOND
                else -> return@mapNotNull null
            }
            val average = decimal(record.averagePrice) ?: return@mapNotNull null
            val lower = decimal(record.lowerPrice) ?: return@mapNotNull null
            val upper = decimal(record.upperPrice) ?: return@mapNotNull null
            val volume = decimal(record.volume) ?: return@mapNotNull null
            if (average <= BigDecimal.ZERO || lower <= BigDecimal.ZERO || upper <= BigDecimal.ZERO || volume <= BigDecimal.ZERO) {
                return@mapNotNull null
            }
            WholesaleRow(date, record.cropCode, market, average, lower, upper, volume)
        }
    }

    private fun readRetail(path: Path): List<RetailRow> {
        val date = path.fileName.toString().removeSuffix(".csv").split('-').let { parts ->
            LocalDate.of(parts[0].toInt() + 1911, parts[1].toInt(), 1)
        }
        return Files.readAllLines(path, StandardCharsets.UTF_8).drop(1).mapNotNull { line ->
            val fields = line.split(',')
            if (fields.size < 5 || fields[1] != "臺北市") return@mapNotNull null
            val value = fields[4].trim().takeUnless { it == "-" }?.let { raw ->
                runCatching { BigDecimal(raw) }.getOrNull()
            }
            RetailRow(date, fields[3].trim(), value)
        }
    }

    private fun decimal(element: kotlinx.serialization.json.JsonElement): BigDecimal? =
        runCatching { BigDecimal(element.toString().trim('"')) }.getOrNull()

    private fun parseRocDate(value: String): LocalDate? {
        val parts = value.split('.')
        if (parts.size != 3) return null
        return runCatching { LocalDate.of(parts[0].toInt() + 1911, parts[1].toInt(), parts[2].toInt()) }.getOrNull()
    }

    private fun provenance(path: Path, source: String): SourceFileProvenance =
        SourceFileProvenance(path.fileName.toString(), sha256(path), source)

    private fun sha256(path: Path): String = MessageDigest.getInstance("SHA-256")
        .digest(Files.readAllBytes(path))
        .joinToString("") { byte -> "%02x".format(byte) }

    private data class MonthlyInput(
        val averageNtdPerKg: BigDecimal,
        val lowerNtdPerKg: BigDecimal,
        val upperNtdPerKg: BigDecimal,
        val volumeKg: BigDecimal,
        val validDays: Int,
    )
}
