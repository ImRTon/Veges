package tw.taipei.veges

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.taipei.veges.designsystem.EstimateDisclosureLabel
import tw.taipei.veges.designsystem.VegesTheme

@RunWith(AndroidJUnit4::class)
class VegesAppSemanticsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeExposesTraditionalChineseAndEstimateDisclosureSemantics() {
        composeRule.setContent {
            VegesTheme { EstimateDisclosureLabel() }
        }

        composeRule.onNodeWithText("估算").fetchSemanticsNode()
        composeRule.onNodeWithText("Taipei retail reference estimate").fetchSemanticsNode()
    }
}
