package tw.taipei.veges.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.launch
import tw.taipei.veges.domain.MarketBasis

/** Returns the threshold when [input] is a positive price, otherwise null. */
fun parseAlertThreshold(input: String): BigDecimal? =
    input.trim().toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }

fun MarketBasis.marketLabel(): String = when (this) {
    MarketBasis.TAIPEI_COMBINED -> "台北合併"
    MarketBasis.TAIPEI_FIRST -> "台北一"
    MarketBasis.TAIPEI_SECOND -> "台北二"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertEditorSheet(
    produceName: String,
    currentPrice: BigDecimal?,
    basis: MarketBasis,
    thresholdInput: String,
    showError: Boolean,
    isEditing: Boolean,
    onBasisSelected: (MarketBasis) -> Unit,
    onThresholdChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun hideThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { action() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "$produceName 價格提醒",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
                currentPrice?.let { price ->
                    Text(
                        "目前市場參考 ${price.setScale(1, RoundingMode.HALF_UP).toPlainString()} 元 / 台斤",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            PillChoiceRow(
                items = MarketBasis.entries,
                selectedItem = basis,
                onItemSelected = onBasisSelected,
                itemLabel = MarketBasis::marketLabel,
            )
            OutlinedTextField(
                value = thresholdInput,
                onValueChange = onThresholdChanged,
                label = { Text("低於多少元時通知") },
                suffix = { Text("元 / 台斤") },
                isError = showError,
                supportingText = if (showError) {
                    { Text("請輸入大於 0 的價格") }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSave() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("alert-threshold"),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isEditing) {
                    TextButton(onClick = { hideThen(onDelete) }) {
                        Text("刪除提醒", color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        if (parseAlertThreshold(thresholdInput) != null) {
                            hideThen(onSave)
                        } else {
                            onSave()
                        }
                    },
                ) { Text("儲存") }
            }
        }
    }
}
