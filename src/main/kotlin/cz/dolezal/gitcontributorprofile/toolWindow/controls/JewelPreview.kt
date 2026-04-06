package cz.dolezal.gitcontributorprofile.toolWindow.controls

import androidx.compose.runtime.Composable
import org.jetbrains.jewel.bridge.theme.SwingBridgeTheme
import org.jetbrains.jewel.foundation.ExperimentalJewelApi

@OptIn(ExperimentalJewelApi::class)
@Composable
internal fun JewelPreview(
    block: @Composable () -> Unit,
) {
    SwingBridgeTheme {
        block()
    }
}
