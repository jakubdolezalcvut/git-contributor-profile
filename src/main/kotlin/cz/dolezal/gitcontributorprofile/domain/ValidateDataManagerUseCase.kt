package cz.dolezal.gitcontributorprofile.domain

import com.intellij.openapi.diagnostic.Logger
import com.intellij.vcs.log.data.VcsLogData

internal class ValidateDataManagerUseCase(
    private val logger: Logger,
) {
    private var previousHash: Int = 0

    operator fun invoke(dataManager: VcsLogData): Boolean {
        // Not initialized yet
        if (dataManager.dataPack.logProviders.isEmpty()) {
            logger.info("DataPack is empty")
            return false
        }
        val commits = dataManager.dataPack.permanentGraph.allCommits
            .take(AnalysisConfig.MAX_COMMITS)

        val nextHash = commits.hashCode()

        // Same commits
        if (previousHash == nextHash) {
            logger.info("DataPack contains the same commits")
            return false
        }
        logger.info("Valid DataPack verified")
        previousHash = nextHash
        return true
    }
}
