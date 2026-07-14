package tw.taipei.veges.catalog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tw.taipei.veges.domain.ProduceCategory
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import tw.taipei.veges.designsystem.AiIllustrationDisclosure

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CatalogRoute(
    onConceptSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CatalogViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::updateQuery,
            label = { Text("搜尋蔬果名稱") },
            modifier = Modifier.fillMaxWidth(),
        )
        Column {
            FilterChip(
                selected = state.category == ProduceCategory.VEGETABLE,
                onClick = { viewModel.selectCategory(ProduceCategory.VEGETABLE) },
                label = { Text("蔬菜") },
            )
            FilterChip(
                selected = state.category == ProduceCategory.FRUIT,
                onClick = { viewModel.selectCategory(ProduceCategory.FRUIT) },
                label = { Text("水果") },
            )
        }
        if (state.results.isEmpty()) Text("找不到已審查的蔬果名稱")
        state.results.forEach { concept ->
            Text(concept.householdName)
            AiIllustrationDisclosure()
        }
        state.ambiguity?.let { ambiguity ->
            ModalBottomSheet(onDismissRequest = viewModel::clearAmbiguity) {
                Text("請選擇「${ambiguity.query}」的意思")
                ambiguity.concepts.forEach { concept -> Text(concept.householdName) }
            }
        }
    }
}

// TODO(7.3): Add reviewed search, category browsing, and ambiguity selection.
