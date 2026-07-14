package tw.taipei.veges.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tw.taipei.veges.designsystem.EstimateDisclosureLabel
import tw.taipei.veges.designsystem.AsyncStateLabel
import tw.taipei.veges.designsystem.UiState

@Composable
fun HomeRoute(
    onBrowseCatalog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val items by viewModel.items.collectAsStateWithLifecycle()
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (items.isEmpty()) Text("尚未追蹤蔬果")
        items.forEach { item ->
            Text(item.concept.householdName)
            item.latestEstimate?.let { estimate ->
                EstimateDisclosureLabel(disclosure = estimate.disclosure)
                Text(estimate.point?.let { price -> "${price.amount} 元 / 台斤" } ?: "估價尚不可用")
            } ?: Text("估價尚不可用：資料不足")
        }
        AsyncStateLabel(UiState.Current(items))
        EstimateDisclosureLabel()
        Button(onClick = onBrowseCatalog) {
            Text("瀏覽蔬果")
        }
    }
}

// TODO(7.2): Replace this architecture placeholder with tracked-produce state and content.
