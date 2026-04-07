package cz.dolezal.gitcontributorprofile.domain

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ContentRevision
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
) {
    private companion object {
        const val MAX_COMMITS = 1000
        const val BATCH_SIZE = 100
    }
    private val fileTypeManager = FileTypeManager.getInstance()

    @RequiresBackgroundThread
    operator fun invoke(
        dataManager: VcsLogData,
        maxCommits: Int,
    ): ImmutableMap<Author, ImmutableContributionStats> {
        val contributions = mutableMapOf<Author, MutableContributionStats>()

        val chunks = dataManager.dataPack.permanentGraph.allCommits
            .take(MAX_COMMITS)
            .chunked(BATCH_SIZE)

        chunks.forEach { chunk ->
            if (dataManager.isDisposed) return@forEach
            analyzeChunk(
                chunk = chunk,
                contributions = contributions,
                dataManager = dataManager,
            )
        }
        // Loaded stats aren't reliable -> return empty
        return if (dataManager.isDisposed) {
            persistentMapOf()
        } else {
            contributions.mapValues { (_, mutableStats) -> mutableStats.toImmutable() }
                .toImmutableMap()
        }
    }

    private fun analyzeChunk(
        chunk: List<GraphCommit<Int>>,
        contributions: MutableMap<Author, MutableContributionStats>,
        dataManager: VcsLogData,
    ) {
        val commitIndexes = chunk.map { shorageIndex -> shorageIndex.id }
        val commitDetails = dataManager.commitDetailsGetter.getCommitDetails(commitIndexes)

        commitDetails.forEach { commitDetails ->
            if (dataManager.isDisposed) return@forEach
            val author = commitDetails.author.toAuthor()
            val stats = contributions[author] ?: MutableContributionStats()
            updateStats(commitDetails.changes, stats)
            contributions[author] = stats
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
