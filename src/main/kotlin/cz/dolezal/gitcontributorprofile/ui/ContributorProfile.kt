package cz.dolezal.gitcontributorprofile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import cz.dolezal.gitcontributorprofile.domain.Author
import cz.dolezal.gitcontributorprofile.domain.ImmutableContributionStats
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cz.dolezal.gitcontributorprofile.domain.ContributorProfileService
import cz.dolezal.gitcontributorprofile.domain.LastDays
import cz.dolezal.gitcontributorprofile.extension.parseToLastDays
import cz.dolezal.gitcontributorprofile.ui.controls.JewelPreview
import cz.dolezal.gitcontributorprofile.ui.controls.Prompt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Outline
import org.jetbrains.jewel.ui.component.CircularProgressIndicatorBig
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.component.DefaultInformationBanner
import org.jetbrains.jewel.ui.painter.PainterHint

@Composable
internal fun ContributorProfile(
    service: ContributorProfileService,
) {
    val uiState by service.uiState.collectAsState()
    ContributorProfile(uiState = uiState, onLoad = service::load)
}

@Composable
private fun ContributorProfile(
    uiState: ContributorProfileService.UiState,
    onLoad: (LastDays) -> Unit,
) {
    when (uiState) {
        is ContributorProfileService.UiState.Loading -> {
            Loading()
        }
        is ContributorProfileService.UiState.Error -> {
            Error()
        }
        is ContributorProfileService.UiState.Empty -> {
            Empty(
                lastDays = uiState.lastDays,
                onLoad = onLoad,
            )
        }
        is ContributorProfileService.UiState.Success -> {
            Success(
                authors = uiState.authors,
                contributions = uiState.contributions,
                lastDays = uiState.lastDays,
                totalAuthors = uiState.totalAuthors,
                onLoad = onLoad,
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
private fun Empty(
    lastDays: LastDays,
    onLoad: (LastDays) -> Unit,
) {
    Column(
        modifier = Modifier.padding(8.dp),
    ) {
        LastDaysInput(
            lastDays = lastDays,
            onLoad = onLoad,
        )
        Prompt(
            text = "No commits found",
            color = JewelTheme.globalColors.text.warning,
        ) {
            Icon(
                key = AllIconsKeys.Vcs.Vendors.Github,
                contentDescription = null,
                tint = JewelTheme.globalColors.text.warning,
                hint = PainterHint.None,
            )
        }
    }
}

@Composable
private fun Success(
    authors: ImmutableList<Author>,
    contributions: ImmutableMap<Author, ImmutableContributionStats>,
    lastDays: LastDays,
    totalAuthors: Int,
    onLoad: (LastDays) -> Unit,
) {
    Column(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LastDaysInput(
            lastDays = lastDays,
            onLoad = onLoad,
        )
        if (contributions.size < totalAuthors) {
            AuthorsBanner(
                showingAuthors = contributions.size,
                totalAuthors = totalAuthors,
            )
        }
        MasterDetail(
            authors = authors,
            contributions = contributions,
        )
    }
}

@Composable
private fun LastDaysInput(
    lastDays: LastDays,
    onLoad: (LastDays) -> Unit,
) {
    val textFieldState = rememberTextFieldState(
        initialText = lastDays.value.toString(),
    )
    var outline by remember { mutableStateOf(Outline.None) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextField(
            state = textFieldState,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            outline = outline,
        )
        DefaultButton(
            onClick = {
                val value = textFieldState.text.toString().parseToLastDays()

                if (value == null) {
                    outline = Outline.Error
                } else {
                    outline = Outline.None
                    onLoad(value)
                }
            },
        ) {
            Text(
                text = "Last days",
            )
        }
    }
}

@Composable
private fun AuthorsBanner(
    showingAuthors: Int,
    totalAuthors: Int,
) {
    val message = remember(showingAuthors, totalAuthors) {
        if (showingAuthors == 1) {
            "Showing single author out of $totalAuthors"
        } else {
            "Showing $showingAuthors authors out of $totalAuthors"
        }
    }
    DefaultInformationBanner(
        text = message,
    )
}

@Composable
@Preview
private fun LoadingPreview() {
    JewelPreview {
        Loading()
    }
}
