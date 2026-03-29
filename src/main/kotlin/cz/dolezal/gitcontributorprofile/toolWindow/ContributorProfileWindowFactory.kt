package cz.dolezal.gitcontributorprofile.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class ContributorProfileWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contributorProfileToolWindow = ContributorProfileToolWindow(toolWindow)

        val content = ContentFactory.getInstance()
            .createContent(contributorProfileToolWindow.content, null, false)

        contributorProfileToolWindow.load()
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true
}
