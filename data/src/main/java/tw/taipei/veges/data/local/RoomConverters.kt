package tw.taipei.veges.data.local

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import androidx.room.TypeConverter
import tw.taipei.veges.domain.Freshness
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.ProduceCategory
import tw.taipei.veges.domain.PriceUnit
import tw.taipei.veges.domain.SourceDayState
import tw.taipei.veges.domain.SourceKind
import tw.taipei.veges.domain.UnavailableReason

class RoomConverters {
    @TypeConverter
    fun instantToString(value: Instant?): String? = value?.toString()

    @TypeConverter
    fun stringToInstant(value: String?): Instant? = value?.let(Instant::parse)

    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun decimalToString(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun stringToDecimal(value: String?): BigDecimal? = value?.let(::BigDecimal)

    @TypeConverter
    fun marketToString(value: MarketBasis?): String? = value?.name

    @TypeConverter
    fun stringToMarket(value: String?): MarketBasis? = value?.let(MarketBasis::valueOf)

    @TypeConverter
    fun sourceToString(value: SourceKind?): String? = value?.name

    @TypeConverter
    fun stringToSource(value: String?): SourceKind? = value?.let(SourceKind::valueOf)

    @TypeConverter
    fun categoryToString(value: ProduceCategory?): String? = value?.name

    @TypeConverter
    fun stringToCategory(value: String?): ProduceCategory? = value?.let(ProduceCategory::valueOf)

    @TypeConverter
    fun unitToString(value: PriceUnit?): String? = value?.name

    @TypeConverter
    fun stringToUnit(value: String?): PriceUnit? = value?.let(PriceUnit::valueOf)

    @TypeConverter
    fun dayStateToString(value: SourceDayState?): String? = value?.name

    @TypeConverter
    fun stringToDayState(value: String?): SourceDayState? = value?.let(SourceDayState::valueOf)

    @TypeConverter
    fun freshnessToString(value: Freshness?): String? = value?.name

    @TypeConverter
    fun stringToFreshness(value: String?): Freshness? = value?.let(Freshness::valueOf)

    @TypeConverter
    fun unavailableReasonToString(value: UnavailableReason?): String? = value?.name

    @TypeConverter
    fun stringToUnavailableReason(value: String?): UnavailableReason? =
        value?.let(UnavailableReason::valueOf)
}
