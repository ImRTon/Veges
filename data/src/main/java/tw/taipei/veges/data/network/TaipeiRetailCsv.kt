package tw.taipei.veges.data.network

data class TaipeiRetailRecordDto(
    val rowNumber: String,
    val countyName: String,
    val countyCode: String,
    val itemName: String,
    val averageNtdPerTaiJin: String,
)

object TaipeiRetailCsvParser {
    fun parse(csv: String): List<TaipeiRetailRecordDto> {
        val rows = csv.lineSequence()
            .filter(String::isNotBlank)
            .map(::parseLine)
            .toList()
        require(rows.isNotEmpty()) { "Retail CSV is empty" }
        require(rows.first() == HEADER) { "Unexpected Taipei retail CSV header" }
        return rows.drop(1).map { row ->
            require(row.size == HEADER.size) { "Unexpected Taipei retail CSV column count" }
            TaipeiRetailRecordDto(
                rowNumber = row[0],
                countyName = row[1],
                countyCode = row[2],
                itemName = row[3],
                averageNtdPerTaiJin = row[4],
            )
        }
    }

    private fun parseLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var index = 0
        while (index < line.length) {
            when (val character = line[index]) {
                '"' -> {
                    if (quoted && index + 1 < line.length && line[index + 1] == '"') {
                        field.append('"')
                        index++
                    } else {
                        quoted = !quoted
                    }
                }
                ',' -> if (quoted) field.append(character) else {
                    fields += field.toString()
                    field.clear()
                }
                else -> field.append(character)
            }
            index++
        }
        check(!quoted) { "Unclosed quoted CSV field" }
        fields += field.toString()
        return fields
    }

    private val HEADER = listOf("序號", "縣市名", "縣市別代碼", "項目", "平均（元/台斤）")
}
