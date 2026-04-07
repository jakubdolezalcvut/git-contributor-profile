package cz.dolezal.gitcontributorprofile.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.components.JBPanel
import cz.dolezal.gitcontributorprofile.domain.ContributorProfileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.jetbrains.jewel.bridge.JewelComposePanel
import java.awt.BorderLayout

internal class ContributorProfileToolWindow(
    toolWindow: ToolWindow,
) : Disposable {

    private val service = ContributorProfileService.getInstance(toolWindow.project)
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.EDT)

    val content = JBPanel<JBPanel<*>>(BorderLayout())

    init {
        val composePanel = JewelComposePanel {
           ContributorProfile(service)
        }
        content.add(composePanel, BorderLayout.CENTER)
    }

    override fun dispose() {
        uiScope.cancel("ToolWindow disposed")
    }

    fun load() {
        service.load()
    }
}
