package cz.dolezal.gitcontributorprofile.domain

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ContentRevision
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportProgressScope
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.vcs.log.VcsUser
import com.intellij.vcs.log.data.AbstractDataGetter.Companion.getCommitDetails
import com.intellij.vcs.log.data.VcsLogData
import com.intellij.vcs.log.graph.GraphCommit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap

internal class AnalyzeContributionsUseCase(
    private val logger: Logger,
    private val project: Project,
) {
    @JvmInline
    private value class Email(
        val value: String,
    )
    private val fileTypeManager = FileTypeManager.getInstance()

    @RequiresBackgroundThread
    suspend operator fun invoke(
        dataManager: VcsLogData,
        filteredCommits: FilteredCommits,
    ): ImmutableMap<Author, ImmutableContributionStats> =
        withBackgroundProgress(project = project, title = "Contribution Profile", cancellable = true) {
            val contributions = mutableMapOf<Email, MutableContributionStats>()
            val chunks = filteredCommits.commits.chunked(AnalysisConfig.Commits.BATCH)

            reportProgressScope(chunks.size) { progressReporter ->
                chunks.forEach { chunk ->
                    if (dataManager.isDisposed) return@forEach

                    progressReporter.itemStep {
                        analyzeChunk(chunk, contributions, dataManager)
                    }
                }
            }
            // Loaded stats aren't reliable -> return empty
            if (dataManager.isDisposed) {
                logger.info("Disposing contribution stats")
                persistentMapOf()
            } else {
                contributions.map { (_, mutableStats) ->
                    mutableStats.author to mutableStats.toImmutable()
                }
                    .toMap()
                    .toImmutableMap()
            }
        }

    private fun analyzeChunk(
        chunk: List<GraphCommit<Int>>,
        contributions: MutableMap<Email, MutableContributionStats>,
        dataManager: VcsLogData,
    ) {
        val commitIndexes = chunk.map { shorageIndex -> shorageIndex.id }
        val commitDetails = dataManager.commitDetailsGetter.getCommitDetails(commitIndexes)

        commitDetails.forEach { commitDetails ->
            if (dataManager.isDisposed) return@forEach
            val author = commitDetails.author.toAuthor()
            val email = Email(author.email)

            val stats = contributions[email] ?: MutableContributionStats(author)
            updateStats(commitDetails.changes, stats)
            contributions[email] = stats
        }
    }

    private fun updateStats(
        changes: Collection<Change>,
        stats: MutableContributionStats,
    ) {
        stats.commits++
        updateFileStats(changes, stats)
        updateLanguageStats(changes, stats)
    }

    private fun updateFileStats(
        changes: Collection<Change>,
        stats: MutableContributionStats,
    ) {
        changes.forEach { change ->
            when (change.type) {
                Change.Type.NEW -> {
                    stats.addedFiles++
                }
                Change.Type.DELETED -> {
                    stats.removedFiles++
                }
                Change.Type.MOVED -> {
                    stats.movedFiles++
                }
                Change.Type.MODIFICATION -> {
                    stats.modifiedFiles++
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
        val fileType = fileTypeManager.getFileTypeByFileName(revision.file.name)
        val language = fileType.name
        val languageCount = stats.languages[language] ?: 0
        stats.languages[language] = languageCount + 1
    }

    private fun VcsUser.toAuthor() = Author(
        name = name,
        email = email,
    )

    private fun MutableContributionStats.toImmutable() = ImmutableContributionStats(
        commits = commits,
        addedFiles = addedFiles,
        removedFiles = removedFiles,
        movedFiles = movedFiles,
        modifiedFiles = modifiedFiles,
        languages = languages.toImmutableMap(),
    )

    private fun Map<String, Int>.toImmutableMap(): ImmutableList<LanguageStat> =
        map { (language, count) ->
            LanguageStat(
                count = count,
                name = language,
            )
        }
            .sortedByDescending { stat -> stat.count }
            .toImmutableList()
}
