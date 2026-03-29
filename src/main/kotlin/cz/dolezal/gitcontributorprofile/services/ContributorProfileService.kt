package cz.dolezal.gitcontributorprofile.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.VcsNotifier
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ContentRevision
import com.intellij.vcs.log.VcsUser
import com.intellij.vcs.log.data.AbstractDataGetter.Companion.getCommitDetails
import com.intellij.vcs.log.data.VcsLogData
import com.intellij.vcs.log.impl.VcsProjectLog
import cz.dolezal.gitcontributorprofile.MyBundle
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.collections.mutableMapOf

@Service(Service.Level.PROJECT)
internal class ContributorProfileService(
    private val project: Project,
    private val coroutineScope: CoroutineScope,
) {
    init {
        thisLogger().info(MyBundle.message("projectService", project.name))
    }

    sealed interface UiState {
        data object Loading : UiState
        data object Empty : UiState
        data object Error : UiState
        data class Success(val contributions: Map<Author, ImmutableContributionStats>) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun load(maxCommits: Int = 100) {
        coroutineScope.launch {
            _uiState.value = UiState.Loading
            try {
                val stats = getContributorStats(maxCommits, project)
                _uiState.value = when {
                    stats.isEmpty() -> UiState.Empty
                    else -> UiState.Success(stats)
                }
            } catch (vcsException: VcsException) {
                thisLogger().error(vcsException)
                showError(vcsException)
                _uiState.value = UiState.Error
            }
        }
    }

    private fun showError(vcsException: VcsException) {
        VcsNotifier.getInstance(project)
            .notifyError("gitcontributorprofile.commit.error", "Can't load commit history", vcsException.message)
    }

    private fun getContributorStats(
        maxCommits: Int,
        project: Project,
    ): Map<Author, ImmutableContributionStats> {
        val logManager = VcsProjectLog.getInstance(project).logManager ?: return emptyMap()
        val dataManager = logManager.dataManager
        val contributions = analyzeCommits(dataManager, maxCommits)

        dataManager.addDataPackChangeListener {
            it.toString()
        }
        return contributions
    }

    private fun analyzeCommits(
        dataManager: VcsLogData,
        maxCommits: Int,
    ): Map<Author, ImmutableContributionStats> {
        val contributions = createInitialContributions(dataManager.allUsers)
        var commitIndex = 0

        dataManager.storage.iterateCommits { commitId ->
            val commitDetails = dataManager.commitDetailsGetter.getCommitDetails(commitId.hash, commitId.root)
            val author = commitDetails.author.toAuthor()

            contributions[author]?.let { stats ->
                stats.commits++
                updateFileStats(commitDetails.changes, stats)
                updateLanguageStats(commitDetails.changes, stats)
            }
            commitIndex++
            commitIndex < maxCommits
        }
        return contributions.mapValues { (_, mutableStats) -> mutableStats.toImmutable() }
    }

    private fun createInitialContributions(
        vcsUsers: Set<VcsUser>,
    ): Map<Author, MutableContributionStats> {
        val authors = vcsUsers.map { vcsUser ->
            vcsUser.toAuthor()
        }
        return authors.fold(mutableMapOf()) { map, author ->
            map[author] = MutableContributionStats()
            map
        }
    }

    private fun updateFileStats(
        changes: Collection<Change>,
        stats: MutableContributionStats,
    ) {
        changes.forEach { change ->
            when (change.type) {
                Change.Type.MODIFICATION -> {
                    stats.modifiedFiles++
                }
                Change.Type.NEW -> {
                    stats.addedFiles++
                }
                Change.Type.DELETED -> {
                    stats.removedFiles++
                }
                Change.Type.MOVED -> {
                    stats.movedFiles++
                }
            }
        }
    }

    private fun updateLanguageStats(
        changes: Collection<Change>,
        stats: MutableContributionStats,
    ) {
        changes.forEach { change ->
            val revision = when (change.type) {
                Change.Type.MODIFICATION -> {
                    change.afterRevision
                }
                Change.Type.NEW -> {
                    change.afterRevision
                }
                Change.Type.DELETED -> {
                    change.beforeRevision
                }
                Change.Type.MOVED -> {
                    change.afterRevision
                }
            }
            if (revision != null) {
                updateLanguageStats(revision, stats)
            }
        }
    }

    private fun updateLanguageStats(
        revision: ContentRevision,
        stats: MutableContributionStats,
    ) {
        val fileName = revision.file.name.lowercase()

        val language = Language.entries.find { language ->
            fileName.endsWith(language.suffix)
        } ?: return

        val languageCount = stats.languages[language] ?: 0
        stats.languages[language] = languageCount + 1
    }

    private fun VcsUser.toAuthor() = Author(
        name = name,
        email = email,
    )

    private fun MutableContributionStats.toImmutable() =
        ImmutableContributionStats(
            commits = commits,
            addedFiles = addedFiles,
            removedFiles = removedFiles,
            modifiedFiles = modifiedFiles,
            movedFiles = movedFiles,
            languages = languages.toImmutableMap(),
        )
}
