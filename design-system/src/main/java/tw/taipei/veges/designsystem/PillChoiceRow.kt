package tw.taipei.veges.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun <T> PillChoiceRow(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    itemLabel: (T) -> String,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    selectedContainerColor: Color? = null,
    selectedContentColor: Color? = null,
) {
    val background = containerColor ?: MaterialTheme.colorScheme.surfaceContainerLow
    val selectedBackground = selectedContainerColor
        ?: MaterialTheme.colorScheme.surfaceContainerHighest
    val selectedForeground = selectedContentColor
        ?: MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
        color = background,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items.forEach { item ->
                val selected = item == selectedItem
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .selectable(
                            selected = selected,
                            onClick = { onItemSelected(item) },
                            role = Role.Tab,
                        )
                        .semantics(mergeDescendants = true) { },
                    shape = RoundedCornerShape(50),
                    color = if (selected) {
                        selectedBackground
                    } else {
                        Color.Transparent
                    },
                    contentColor = if (selected) {
                        selectedForeground
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = itemLabel(item),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}
