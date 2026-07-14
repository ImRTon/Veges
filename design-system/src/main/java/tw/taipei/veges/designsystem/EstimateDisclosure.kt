package tw.taipei.veges.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import tw.taipei.veges.domain.EstimateDisclosure

@Composable
fun EstimateTag(
    modifier: Modifier = Modifier,
    disclosure: EstimateDisclosure = EstimateDisclosure(),
) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(disclosure.shortTag) },
        modifier = modifier.semantics {
            contentDescription = EstimateDisclosure.ACCESSIBILITY_LABEL
        },
    )
}

@Composable
fun EstimateDisclosureLabel(
    modifier: Modifier = Modifier,
    disclosure: EstimateDisclosure = EstimateDisclosure(),
) {
    Row(
        modifier = modifier
            .padding(vertical = 4.dp)
            .semantics { contentDescription = EstimateDisclosure.ACCESSIBILITY_LABEL },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EstimateTag(disclosure = disclosure)
        Text(
            text = disclosure.fullLabel,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
