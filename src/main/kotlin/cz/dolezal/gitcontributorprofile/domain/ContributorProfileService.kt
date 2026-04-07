package cz.dolezal.gitcontributorprofile.domain

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.VcsNotifier
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.vcs.log.data.DataPackChangeListener
import com.intellij.vcs.log.data.VcsLogData
import com.intellij.vcs.log.impl.VcsLogManager
import com.intellij.vcs.log.impl.VcsProjectLog
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest

@Service(Service.Level.PROJECT)
@OptIn(ExperimentalCoroutinesApi::class)
internal class ContributorProfileService(
    private val project: Project,
    private val coroutineScope: CoroutineScope,
) : Disposable {

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ContributorProfileService = project.service()
    }

    private object Defaults {
        const val MAX_COMMITS = 100
    }

    // region Types

    sealed interface UiState {

        data object Loading : UiState
        data object Empty : UiState
        data object Error : UiState

        data class Success(
            val authors: ImmutableList<Author>,
            val contributions: ImmutableMap<Author, ImmutableContributionStats>,
            val totalAuthors: Int,
        ) : UiState
    }

    private data class LoadRequest(
        val maxCommits: Int,
    )

    // endregion Types

    // region properties

    private val vcsListener = DataPackChangeListener {
        loadRequests.tryEmit(LoadRequest(lastMaxCommits))
    }
    private val managerListener = object : VcsProjectLog.ProjectLogListener {
        override fun logCreated(manager: VcsLogManager) {
            thisLogger().info("VcsLogManager created")
            storedDataManager = manager.dataManager
            manager.dataManager.addDataPackChangeListener(vcsListener)
            startCollectingLoadRequests(manager.dataManager)
        }
        override fun logDisposed(manager: VcsLogManager) {
            thisLogger().info("VcsLogManager disposed")
            storedDataManager = null
            stopCollectingLoadRequests()
        }
    }
    private var lastMaxCommits: Int = Defaults.MAX_COMMITS
    private var storedDataManager: VcsLogData? = null
    private var loadRequestsJob: Job? = null

    private val analyzeContributionsUseCase = AnalyzeContributionsUseCase(thisLogger())

    private val loadRequests: MutableSharedFlow<LoadRequest> = MutableSharedFlow(
        // LoadRequest can wait till startCollectingLoadRequests is called after VcsLogManager is created
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // endregion properties

    // region functions

    init {
        project.messageBus.connect(coroutineScope).subscribe(
            topic = VcsProjectLog.VCS_PROJECT_LOG_CHANGED,
            handler = managerListener,
        )
    }

    override fun dispose() {
        storedDataManager?.removeDataPackChangeListener(vcsListener)
        stopCollectingLoadRequests() // Actually not needed because of structured concurrency but feels better this way
    }

    fun load(maxCommits: Int = Defaults.MAX_COMMITS) {
        loadRequests.tryEmit(LoadRequest(maxCommits))
    }

    private fun startCollectingLoadRequests(dataManager: VcsLogData) {
        stopCollectingLoadRequests()

        // Not using distinctUntilChanged since newer request may load different commits
        loadRequestsJob = loadRequests.mapLatest { request ->
            lastMaxCommits = request.maxCommits
            _uiState.value = UiState.Loading
            _uiState.value = loadContributorStats(dataManager, request.maxCommits)
        }
            // Because low-level IO logic is in Java, hence blocking, see VcsLogStorageImpl
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    private fun stopCollectingLoadRequests() {
        loadRequestsJob?.cancel()
    }

    @RequiresBackgroundThread
    private fun loadContributorStats(
        dataManager: VcsLogData,
        maxCommits: Int,
    ): UiState =
        try {
            val stats = analyzeContributionsUseCase(dataManager, maxCommits)
            when {
                stats.isEmpty() -> UiState.Empty
                else -> UiState.Success(
                    authors = stats.toSortedAuthors(),
                    contributions = stats,
                    totalAuthors = dataManager.allUsers.size,
                )
            }
        } catch (vcsException: VcsException) {
            thisLogger().error(vcsException)
            showError(vcsException)
            UiState.Error
        }

    private fun ImmutableMap<Author, ImmutableContributionStats>.toSortedAuthors(): ImmutableList<Author> =
        keys.sortedByDescending { author -> this[author]?.commits ?: 0 }
            .toImmutableList()

    private fun showError(vcsException: VcsException) {
        VcsNotifier.getInstance(project)
            .notifyError("gitcontributorprofile.commit.error", "Can't load commit history", vcsException.message)
    }

    // endregion functions
}
