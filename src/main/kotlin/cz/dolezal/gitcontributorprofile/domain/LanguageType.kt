package cz.dolezal.gitcontributorprofile.domain

internal enum class LanguageType(
    val suffix: String,
) {
    HTML(".html"),
    JAVA(".java"),
    KOTLIN(".kt"),
    XML(".xml"),
}
