package tw.taipei.veges.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class MoaWholesaleRecordDto(
    @SerialName("交易日期") val transactionDate: String,
    @SerialName("種類代碼") val kindCode: String?,
    @SerialName("作物代號") val cropCode: String,
    @SerialName("作物名稱") val cropName: String?,
    @SerialName("市場代號") val marketCode: String,
    @SerialName("市場名稱") val marketName: String,
    @SerialName("上價") val upperPrice: JsonElement,
    @SerialName("中價") val middlePrice: JsonElement,
    @SerialName("下價") val lowerPrice: JsonElement,
    @SerialName("平均價") val averagePrice: JsonElement,
    @SerialName("交易量") val volume: JsonElement,
)

@Serializable
data class MoaCropDto(
    @SerialName("CropCode") val code: String,
    @SerialName("CropName") val name: String,
)

@Serializable
data class MoaCropResponseDto(
    @SerialName("RS") val result: String,
    @SerialName("Data") val data: List<MoaCropDto>,
)

internal fun JsonElement.decimalValue(): java.math.BigDecimal? {
    val primitive = jsonPrimitive
    if (primitive.isString && primitive.content.isBlank()) return null
    return runCatching { java.math.BigDecimal(primitive.content) }.getOrNull()
}

internal val strictSourceJson = Json {
    ignoreUnknownKeys = false
    isLenient = false
    explicitNulls = false
}
