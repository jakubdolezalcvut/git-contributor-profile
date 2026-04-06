package cz.dolezal.gitcontributorprofile.services

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableMap

@Immutable
internal data class ImmutableContributionStats(
    val commits: Int,
    val addedFiles: Int,
    val removedFiles: Int,
    val movedFiles: Int,
    val modifiedFiles: Int,
    val languages: ImmutableMap<Language, Int>,
)
