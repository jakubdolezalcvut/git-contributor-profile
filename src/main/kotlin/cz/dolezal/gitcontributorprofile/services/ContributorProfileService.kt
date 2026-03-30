package cz.dolezal.gitcontributorprofile.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.VcsNotifier
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ContentRevision
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.vcs.log.VcsUser
import com.intellij.vcs.log.data.AbstractDataGetter.Companion.getCommitDetails
import com.intellij.vcs.log.data.DataPackChangeListener
import com.intellij.vcs.log.data.VcsLogData
import com.intellij.vcs.log.impl.VcsProjectLog
import cz.dolezal.gitcontributorprofile.MyBundle
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlin.collections.mutableMapOf

@Service(Service.Level.PROJECT)
@OptIn(ExperimentalCoroutinesApi::class)
internal class ContributorProfileService(
    private val project: Project,
    coroutineScope: CoroutineScope,
) : Disposable {

    private object Defaults {
        const val MAX_COMMITS = 100
    }

    sealed interface UiState {
        data object Loading : UiState
        data object Empty : UiState
        data object Error : UiState
        data class Success(val contributions: Map<Author, ImmutableContributionStats>) : UiState
    }

    private data class LoadRequest(
        val maxCommits: Int,
    )
    private val loadRequests: MutableSharedFlow<LoadRequest> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private var lastMaxCommits: Int = Defaults.MAX_COMMITS
    private var dataManager: VcsLogData? = null

    private val vcsListener = DataPackChangeListener { loadRequests.tryEmit(LoadRequest(lastMaxCommits)) }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadRequests.distinctUntilChanged()
            .mapLatest { request ->
                lastMaxCommits = request.maxCommits
                _uiState.value = UiState.Loading
                _uiState.value = loadContributorStats(request.maxCommits)
            }
            .flowOn(Dispatchers.Default)
            .launchIn(coroutineScope)

        setup()
    }

    override fun dispose() {
        dataManager?.removeDataPackChangeListener(vcsListener)
    }

    fun load(maxCommits: Int = Defaults.MAX_COMMITS) {
        loadRequests.tryEmit(LoadRequest(maxCommits))
    }

    private fun setup() {
        thisLogger().info(MyBundle.message("projectService", project.name))

        val logManager = VcsProjectLog.getInstance(project).logManager ?: return
        dataManager = logManager.dataManager
        dataManager?.addDataPackChangeListener(vcsListener)
    }

    @RequiresBackgroundThread
    private fun loadContributorStats(maxCommits: Int): UiState =
        try {
            val stats = analyzeCommits(dataManager!!, maxCommits)
            when {
                stats.isEmpty() -> UiState.Empty
                else -> UiState.Success(stats)
            }
        } catch (vcsException: VcsException) {
            thisLogger().error(vcsException)
            showError(vcsException)
            UiState.Error
        }

    private fun showError(vcsException: VcsException) {
        VcsNotifier.getInstance(project)
            .notifyError("gitcontributorprofile.commit.error", "Can't load commit history", vcsException.message)
    }

    @RequiresBackgroundThread
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
