package cz.dolezal.gitcontributorprofile.services

import androidx.compose.runtime.Immutable

@Immutable
internal enum class Language(
    val suffix: String,
) {
    HTML(".html"),
    JAVA(".java"),
    KOTLIN(".kt"),
    XML(".xml"),
}
