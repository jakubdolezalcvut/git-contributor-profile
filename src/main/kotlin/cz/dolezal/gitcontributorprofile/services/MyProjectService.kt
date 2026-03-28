package cz.dolezal.gitcontributorprofile.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.vcs.log.VcsUser
import com.intellij.vcs.log.VcsLog
import com.intellij.vcs.log.data.AbstractDataGetter.Companion.getCommitDetails
import com.intellij.vcs.log.impl.VcsProjectLog
import cz.dolezal.gitcontributorprofile.MyBundle
import kotlinx.coroutines.CoroutineScope

@Service(Service.Level.PROJECT)
internal class MyProjectService(
    project: Project,
    private val coroutineScope: CoroutineScope,
) {
    init {
        thisLogger().info(MyBundle.message("projectService", project.name))
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    fun getRandomNumber() = (1..100).random()

    private fun Project.isUsingGit(): Boolean {
        val vcsManager = ProjectLevelVcsManager.getInstance(this)

        return vcsManager.getAllActiveVcss()
            .map { vcs -> vcs.name.lowercase() }
            .any { vcsName -> vcsName == "git" }
    }

    fun getContributorStats(project: Project): Map<String, Int> {
        val logManager = VcsProjectLog.getInstance(project).logManager ?: return emptyMap()
        val dataManager = logManager.dataManager

        dataManager.allUsers

        val result = mutableMapOf<String, Int>()

        val commits = dataManager.storage.iterateCommits { commitId ->
            val details = dataManager.commitDetailsGetter.getCommitDetails(commitId.hash, commitId.root)
            val author = details.author.toAuthor()

            details.changes
            true
        }
        return result
    }

    private fun VcsUser.toAuthor() = Author(
        name = name,
        email = email,
    )
}

internal data class Author(
    val name: String,
    val email: String,
)
