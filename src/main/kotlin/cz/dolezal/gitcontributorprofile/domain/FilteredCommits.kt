package cz.dolezal.gitcontributorprofile.domain

import com.intellij.vcs.log.graph.GraphCommit

internal data class FilteredCommits(
    val commits: List<GraphCommit<Int>>,
)
