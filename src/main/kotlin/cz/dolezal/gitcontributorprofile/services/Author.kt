package cz.dolezal.gitcontributorprofile.services

import androidx.compose.runtime.Immutable

@Immutable
internal data class Author(
    val name: String,
    val email: String,
)
