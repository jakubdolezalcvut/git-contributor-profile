package cz.dolezal.gitcontributorprofile.toolWindow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import cz.dolezal.gitcontributorprofile.services.Author
import cz.dolezal.gitcontributorprofile.services.ImmutableContributionStats
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.dolezal.gitcontributorprofile.services.ContributorProfileService
import cz.dolezal.gitcontributorprofile.toolWindow.controls.JewelPreview
import cz.dolezal.gitcontributorprofile.toolWindow.controls.Prompt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.foundation.lazy.SelectableLazyColumn
import org.jetbrains.jewel.foundation.lazy.SelectionMode
import org.jetbrains.jewel.foundation.lazy.items
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicatorBig
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.component.InfoText
import org.jetbrains.jewel.ui.component.DefaultInformationBanner
import org.jetbrains.jewel.ui.component.GroupHeader
import org.jetbrains.jewel.ui.component.HorizontalSplitLayout
import org.jetbrains.jewel.ui.component.rememberSplitLayoutState
import org.jetbrains.jewel.ui.painter.PainterHint
import org.jetbrains.jewel.ui.typography

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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (contributions.size < totalAuthors) {
            DefaultInformationBanner(
                text = "Showing ${contributions.size} authors out of $totalAuthors.",
            )
        }
        MasterDetail(
            authors = authors,
            contributions = contributions,
        )
    }
}

@Composable
private fun MasterDetail(
    authors: ImmutableList<Author>,
    contributions: ImmutableMap<Author, ImmutableContributionStats>,
) {
    val splitLayoutState = rememberSplitLayoutState(
        initialSplitFraction = 0.3f,
    )
    var selectedDetail by remember {
        mutableStateOf<ImmutableContributionStats?>(null)
    }
    HorizontalSplitLayout(
        first = {
            Master(
                authors = authors,
            ) { selectedAuthor ->
                selectedDetail = contributions[selectedAuthor]
            }
        },
        second = {
            val stats = selectedDetail

            if (stats == null) {
                EmptyDetail()
            } else {
                Detail(
                    stats = stats,
                )
            }
        },
        state = splitLayoutState,
    )
}

@Composable
private fun EmptyDetail() {
    Prompt(
        text = "Select a user",
        color = JewelTheme.globalColors.text.info,
    ) {
        Icon(
            key = AllIconsKeys.General.User,
            contentDescription = null,
            hint = PainterHint.None,
        )
    }
}

@Composable
private fun Master(
    authors: ImmutableList<Author>,
    onAuthorSelect: (Author) -> Unit,
) {
    var selectedAuthor by remember {
        mutableStateOf<Author?>(null)
    }
    SelectableLazyColumn(
        selectionMode = SelectionMode.Single,
        contentPadding = PaddingValues(8.dp),
        onSelectedIndexesChange = { indexes ->
            val author = authors[indexes.first()]
            selectedAuthor = author
            onAuthorSelect(author)
        },
    ) {
        items(
            items = authors,
            key = { author -> author.email }
        ) { author ->
            AuthorItem(
                author = author,
                showSelected = selectedAuthor == author,
            )
        }
    }
}

@Composable
private fun AuthorItem(
    author: Author,
    showSelected: Boolean,
) {
    val textColor = JewelTheme.globalColors.text

    val background = if (showSelected) {
        Modifier.background(
            color = JewelTheme.globalColors.outlines.focused,
            shape = RoundedCornerShape(4.dp),
        )
    } else {
        Modifier
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(background)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        InfoText(
            text = author.name,
            maxLines = 1,
            style = JewelTheme.typography.regular,
            color = if (showSelected) textColor.selected else textColor.normal,
        )
        InfoText(
            text = author.email,
            maxLines = 1,
            style = JewelTheme.typography.medium,
            color = if (showSelected) textColor.selected else textColor.info,
        )
    }
}

@Composable
private fun Detail(
    stats: ImmutableContributionStats,
) {
    Column(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        GroupHeader(text = "Commits")
        DetailRow(label = "Commits", value = stats.commits)

        GroupHeader(text = "Files")
        DetailRow(label = "Added", value = stats.addedFiles)
        DetailRow(label = "Removed", value = stats.removedFiles)
        DetailRow(label = "Moved", value = stats.movedFiles)
        DetailRow(label = "Modified", value = stats.modifiedFiles)

        GroupHeader(text = "Languages")
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: Int,
) {
    Row(
        modifier = Modifier.padding(start = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        InfoText(
            modifier = Modifier.width(100.dp),
            text = "$label:",
            maxLines = 1,
            style = JewelTheme.typography.medium,
            color = JewelTheme.globalColors.text.info,
        )
        InfoText(
            text = value.toString(),
            maxLines = 1,
            style = JewelTheme.typography.medium,
            color = JewelTheme.globalColors.text.normal,
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
