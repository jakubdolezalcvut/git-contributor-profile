package cz.dolezal.gitcontributorprofile.domain

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ContentRevision
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.vcs.log.VcsUser
import com.intellij.vcs.log.data.AbstractDataGetter.Companion.getCommitDetails
import com.intellij.vcs.log.data.VcsLogData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlin.collections.forEach

internal class AnalyzeContributionsUseCase(
    private val logger: Logger,
) {
    @RequiresBackgroundThread
    operator fun invoke(
        dataManager: VcsLogData,
        maxCommits: Int,
    ): ImmutableMap<Author, ImmutableContributionStats> {
        val contributions = createInitialContributions(dataManager.allUsers)
        var commitIndex = 0

        dataManager.storage.iterateCommits { commitId ->
            if (dataManager.isDisposed) return@iterateCommits false
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
        return if (dataManager.isDisposed) {
            persistentMapOf()
        } else {
            contributions.mapValues { (_, mutableStats) -> mutableStats.toImmutable() }
                .toImmutableMap()
        }
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
        val fileName = revision.file.name.lowercase()

        val languageType = LanguageType.entries.find { language ->
            fileName.endsWith(language.suffix)
        } ?: return

        val languageCount = stats.languages[languageType] ?: 0
        stats.languages[languageType] = languageCount + 1
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

    private fun Map<LanguageType, Int>.toImmutableMap(): ImmutableList<LanguageStat> =
        map { (languageType, count) ->
            LanguageStat(
                count = count,
                name = languageType.getName(),
            )
        }
            .sortedByDescending { stat -> stat.count }
            .toImmutableList()

    private fun LanguageType.getName(): String =
        when (this) {
            LanguageType.HTML -> "HTML"
            LanguageType.JAVA -> "Java"
            LanguageType.KOTLIN -> "Kotlin"
            LanguageType.XML -> "XML"
        }
}
