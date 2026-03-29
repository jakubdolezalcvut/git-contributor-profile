package cz.dolezal.gitcontributorprofile.toolWindow

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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.swing.SwingConstants

internal class ContributorProfileToolWindow(toolWindow: ToolWindow) {

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

    fun load() {
        service.load()
    }

    private fun showLoading() {
        setContent {
            val label = JBLabel(
                "Crunching commits...",
                AnimatedIcon.Default(),
                SwingConstants.LEFT,
            )
            add(label)
        }
    }

    private fun showError() {
        setContent {
            val label = JBLabel(
                "Loading commits failed!",
            )
            add(label)
        }
    }

    private fun showEmpty() {
        setContent {
            val label = JBLabel(
                "No commits found",
            )
            add(label)
        }
    }

    private fun showSuccess(
        contributions: Map<Author, ImmutableContributionStats>,
    ) {
        setContent {

        }
    }

    private fun setContent(block: JBPanel<JBPanel<*>>.() -> Unit) {
        block(content)
    }
}