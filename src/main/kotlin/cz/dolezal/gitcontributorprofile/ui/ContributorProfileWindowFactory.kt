package cz.dolezal.gitcontributorprofile.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class ContributorProfileWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()

        val profileToolWindow = ContributorProfileToolWindow(toolWindow).apply {
            load()
        }
        val content = contentFactory.createContent(profileToolWindow.content, null, false).apply {
            setDisposer(profileToolWindow)
        }
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true
}
