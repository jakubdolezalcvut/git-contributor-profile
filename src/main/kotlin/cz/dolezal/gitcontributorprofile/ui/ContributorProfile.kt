package cz.dolezal.gitcontributorprofile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import cz.dolezal.gitcontributorprofile.domain.Author
import cz.dolezal.gitcontributorprofile.domain.ImmutableContributionStats
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.dolezal.gitcontributorprofile.domain.ContributorProfileService
import cz.dolezal.gitcontributorprofile.ui.controls.JewelPreview
import cz.dolezal.gitcontributorprofile.ui.controls.Prompt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicatorBig
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.component.DefaultInformationBanner
import org.jetbrains.jewel.ui.painter.PainterHint

@Composable
internal fun ContributorProfile(
    service: ContributorProfileService,
) {
    val uiState by service.uiState.collectAsState()
    ContributorProfile(uiState)
}

@Composable
private fun ContributorProfile(
    uiState: ContributorProfileService.UiState,
) {
    when (uiState) {
        is ContributorProfileService.UiState.Loading -> {
            Loading()
        }
        is ContributorProfileService.UiState.Error -> {
            Error()
        }
        is ContributorProfileService.UiState.Empty -> {
            Empty()
        }
        is ContributorProfileService.UiState.Success -> {
            Success(
                authors = uiState.authors,
                contributions = uiState.contributions,
                totalAuthors = uiState.totalAuthors,
            )
        }
    }
}

@Composable
private fun Loading() {
    Prompt(
        text = "Crunching commits...",
        color = JewelTheme.globalColors.text.info,
    ) {
        CircularProgressIndicatorBig()
    }
}

@Composable
private fun Error() {
    Prompt(
        text = "Loading commits failed!",
        color = JewelTheme.globalColors.text.error,
    ) {
        Icon(
            key = AllIconsKeys.General.Error,
            contentDescription = null,
            hint = PainterHint.None,
        )
    }
}

@Composable
private fun Empty() {
    Prompt(
        text = "No commits found",
        color = JewelTheme.globalColors.text.warning,
    ) {
        Icon(
            key = AllIconsKeys.Vcs.Vendors.Github,
            contentDescription = null,
            hint = PainterHint.None,
        )
    }
}

@Composable
private fun Success(
    authors: ImmutableList<Author>,
    contributions: ImmutableMap<Author, ImmutableContributionStats>,
    totalAuthors: Int,
) {
    Column(
        modifier = Modifier.padding(8.dp    ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (contributions.size < totalAuthors) {
            val message = remember(contributions, totalAuthors) {
                if (contributions.size == 1) {
                    "Showing single author out of $totalAuthors"
                } else {
                    "Showing ${contributions.size} authors out of $totalAuthors"
                }
            }
            DefaultInformationBanner(
                text = message,
            )
        }
        MasterDetail(
            authors = authors,
            contributions = contributions,
        )
    }
}

@Composable
@Preview
private fun LoadingPreview() {
    JewelPreview {
        Loading()
    }
}
