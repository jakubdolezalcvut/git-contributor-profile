package cz.dolezal.gitcontributorprofile.domain

internal object AnalysisConfig {

    object Commits {
        const val MAX = 10_000
        const val BATCH = 100
    }

    object Days {
        const val MIN = 1
        const val MAX = 365
        val DEFAULT = LastDays(7)
    }
}