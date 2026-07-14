package tw.taipei.veges.domain

/** User-visible semantics that must travel with every model-derived price. */
data class EstimateDisclosure(
    val shortTag: String = SHORT_TAG,
    val fullLabel: String = FULL_LABEL,
) {
    init {
        require(shortTag == SHORT_TAG) { "Estimate short tag must remain '$SHORT_TAG'" }
        require(fullLabel == FULL_LABEL) { "Estimate full label must remain '$FULL_LABEL'" }
    }

    companion object {
        const val SHORT_TAG = "估算"
        const val FULL_LABEL = "Taipei retail reference estimate"
        const val ACCESSIBILITY_LABEL = "估算，Taipei retail reference estimate"
    }
}
