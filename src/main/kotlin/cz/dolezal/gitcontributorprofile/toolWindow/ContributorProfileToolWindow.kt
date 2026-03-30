package cz.dolezal.gitcontributorprofile.toolWindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import cz.dolezal.gitcontributorprofile.services.Author
import cz.dolezal.gitcontributorprofile.services.ContributorProfileService
import cz.dolezal.gitcontributorprofile.services.ImmutableContributionStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.awt.Component
import javax.swing.SwingConstants

internal class ContributorProfileToolWindow(
    toolWindow: ToolWindow,
) : Disposable {

    private val service = toolWindow.project.service<ContributorProfileService>()
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.EDT)

    val content = JBPanel<JBPanel<*>>()

    init {
        service.uiState.onEach { uiState ->
            when (uiState) {
                is ContributorProfileService.UiState.Loading -> showLoading()
                is ContributorProfileService.UiState.Error -> showError()
                is ContributorProfileService.UiState.Empty -> showEmpty()
                is ContributorProfileService.UiState.Success -> showSuccess(uiState.contributions)
            }
        }
            .launchIn(uiScope)
    }

    override fun dispose() {
        uiScope.cancel("ToolWindow disposed")
    }

    fun load() {
        service.load()
    }

    private fun showLoading() {
        setContent {
            JBLabel(
                "Crunching commits...",
                AnimatedIcon.Default(),
                SwingConstants.LEFT,
            )
        }
    }

    private fun showError() {
        setContent {
            JBLabel(
                "Loading commits failed!",
            )
        }
    }

    private fun showEmpty() {
        setContent {
            JBLabel(
                "No commits found",
            )
        }
    }

    private fun showSuccess(
        contributions: Map<Author, ImmutableContributionStats>,
    ) {
        setContent {

        }
    }

    private fun setContent(block: () -> Component) {
        content.removeAll()
        content.add(block())
    }
}
