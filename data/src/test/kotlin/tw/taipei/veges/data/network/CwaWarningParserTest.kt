package tw.taipei.veges.data.network

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.taipei.veges.domain.MarketShockKind

class CwaWarningParserTest {
    private val now = Instant.parse("2026-07-29T14:00:00Z")

    @Test
    fun parsesActiveAlertAndAffectedAreas() {
        val signals = CwaWarningParser.parse(
            raw = alert(
                urgency = "Future",
                headline = "豪雨特報",
                effective = "2026-07-29T20:00:00+08:00",
                expires = "2026-07-30T08:00:00+08:00",
            ),
            kind = MarketShockKind.HEAVY_RAIN,
            now = now,
        )

        assertEquals(1, signals.size)
        assertEquals(setOf("雲林縣", "屏東縣"), signals.single().affectedAreas)
        assertEquals(MarketShockKind.TYPHOON, signals.single().cause)
    }

    @Test
    fun leavesNonTyphoonRainWithoutCauseAttribution() {
        val signals = CwaWarningParser.parse(
            raw = alert(
                urgency = "Future",
                headline = "豪雨特報",
                effective = "2026-07-29T20:00:00+08:00",
                expires = "2026-07-30T08:00:00+08:00",
                description = "午後對流影響，山區有局部豪雨。",
            ),
            kind = MarketShockKind.HEAVY_RAIN,
            now = now,
        )

        assertEquals(null, signals.single().cause)
    }

    @Test
    fun ignoresReleasedOrExpiredAlert() {
        val released = CwaWarningParser.parse(
            raw = alert(
                urgency = "Past",
                headline = "解除颱風警報",
                effective = "2026-07-29T08:00:00+08:00",
                expires = "2026-07-29T22:10:00+08:00",
            ),
            kind = MarketShockKind.TYPHOON,
            now = now,
        )

        assertTrue(released.isEmpty())
    }

    private fun alert(
        urgency: String,
        headline: String,
        effective: String,
        expires: String,
        description: String = "第 8 號颱風及其外圍環流影響，產地有局部豪雨。",
    ) = """
        {
          "success": "true",
          "records": {
            "info": [
              {
                "urgency": "$urgency",
                "severity": "Severe",
                "effective": "$effective",
                "expires": "$expires",
                "headline": "$headline",
                "description": {
                  "section": [
                    { "title": "天氣影響", "value": "$description" }
                  ]
                },
                "area": [
                  { "areaDesc": "雲林縣" },
                  { "areaDesc": "屏東縣" }
                ]
              }
            ]
          }
        }
    """.trimIndent()
}
