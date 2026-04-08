package cz.dolezal.gitcontributorprofile.domain

import com.intellij.openapi.diagnostic.Logger
import com.intellij.vcs.log.data.VcsLogData
import com.intellij.vcs.log.graph.GraphCommit
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus

internal class FilterCommitsUseCase(
    private val logger: Logger,
) {
    private var previousHash: Int = 0

    // "Parse, don't validate" architecture pattern
    operator fun invoke(
        dataManager: VcsLogData,
        lastDays: LastDays,
        wasSuccessful: Boolean,
    ): FilteredCommits? {
        if (dataManager.isNotInitialized()) {
            logger.info("DataPack is empty")
            return null
        }
        val commits = dataManager.getLastCommits(lastDays)
        val currentHash = commits.hashCode()

        if (wasSuccessful && (currentHash == previousHash)) {
            logger.info("DataPack contains the same commits")
            return null
        }
        logger.info("Valid DataPack verified with ${commits.size} commits")
        previousHash = currentHash
        return FilteredCommits(commits)
    }

    private fun VcsLogData.isNotInitialized(): Boolean =
        dataPack.logProviders.isEmpty()

    private fun VcsLogData.getLastCommits(lastDays: LastDays): List<GraphCommit<Int>> {
        val now = Clock.System.now()
        val pastThreshold = now.minus(lastDays.value, DateTimeUnit.DAY, TimeZone.UTC)
        val thresholdMillis = pastThreshold.toEpochMilliseconds()

        // `take` is called before `takeWhile` on purpose
        // For huge repos `lastDays` around 30 already yields huge number of commits
        return dataPack.permanentGraph.allCommits
            .asSequence()
            .take(AnalysisConfig.Commits.MAX)
            .takeWhile { commit -> commit.timestamp >= thresholdMillis }
            .toList()
    }
}
