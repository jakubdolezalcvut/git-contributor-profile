package cz.dolezal.gitcontributorprofile.services

internal class MutableContributionStats(
    var commits: Int = 0,
    var addedFiles: Int = 0,
    var removedFiles: Int = 0,
    var movedFiles: Int = 0,
    var modifiedFiles: Int = 0,
    val languages: MutableMap<Language, Int> = mutableMapOf(),
)
