package cz.dolezal.gitcontributorprofile.ui

import androidx.compose.runtime.LaunchedEffect
import com.intellij.openapi.wm.ToolWindow
import cz.dolezal.gitcontributorprofile.domain.ContributorProfileService
import org.jetbrains.jewel.bridge.JewelComposePanel

internal class ContributorProfileToolWindow(
    toolWindow: ToolWindow,
) {
    private val service = ContributorProfileService.getInstance(toolWindow.project)

    val content = JewelComposePanel {
        ContributorProfile(service)
        LaunchedEffect(Unit) { service.load() }
    }
}
