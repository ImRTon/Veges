package tw.taipei.veges.designsystem

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import tw.taipei.veges.domain.PriceUnit
import tw.taipei.veges.domain.SourceKind

fun formatNtdPerTaiJin(value: BigDecimal): String =
    NumberFormat.getNumberInstance(Locale.TAIWAN).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
        roundingMode = RoundingMode.HALF_UP
    }.format(value) + " 元 / 台斤"

fun formatPrice(value: BigDecimal, unit: PriceUnit): String = when (unit) {
    PriceUnit.NTD_PER_TAI_JIN -> formatNtdPerTaiJin(value)
    PriceUnit.NTD_PER_KILOGRAM -> NumberFormat.getNumberInstance(Locale.TAIWAN).format(value) + " 元 / 公斤"
    PriceUnit.NTD_PER_PIECE -> NumberFormat.getNumberInstance(Locale.TAIWAN).format(value) + " 元 / 件"
}

fun sourceLabel(source: SourceKind): String = when (source) {
    SourceKind.MOA_WHOLESALE -> "農業部批發行情"
    SourceKind.TAIPEI_RETAIL_HISTORY -> "臺北市公有零售市場歷史資料"
    SourceKind.BUNDLED_TAXONOMY -> "審查版蔬果目錄"
    SourceKind.BUNDLED_CALIBRATION -> "審查版估價校準資料"
}
