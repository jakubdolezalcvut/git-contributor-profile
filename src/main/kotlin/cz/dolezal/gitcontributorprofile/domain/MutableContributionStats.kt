package cz.dolezal.gitcontributorprofile.domain

internal class MutableContributionStats(
    val author: Author,
) {
    var commits: Int = 0
    var addedFiles: Int = 0
    var removedFiles: Int = 0
    var movedFiles: Int = 0
    var modifiedFiles: Int = 0
    val languages: MutableMap<String, Int> = mutableMapOf()
}
