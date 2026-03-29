package cz.dolezal.gitcontributorprofile.services

import kotlinx.collections.immutable.ImmutableMap

internal class ImmutableContributionStats(
    val commits: Int,
    val addedFiles: Int,
    val removedFiles: Int,
    val modifiedFiles: Int,
    val movedFiles: Int,
    val languages: ImmutableMap<Language, Int>,
)
