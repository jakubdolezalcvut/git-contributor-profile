package cz.dolezal.gitcontributorprofile.domain

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class ImmutableContributionStats(
    val commits: Int,
    val addedFiles: Int,
    val removedFiles: Int,
    val movedFiles: Int,
    val modifiedFiles: Int,
    val languages: ImmutableList<LanguageStat>,
)
