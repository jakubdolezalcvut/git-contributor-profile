package cz.dolezal.gitcontributorprofile.domain

import androidx.compose.runtime.Immutable

@Immutable
internal data class LanguageStat(
    val count: Int,
    val name: String,
)
