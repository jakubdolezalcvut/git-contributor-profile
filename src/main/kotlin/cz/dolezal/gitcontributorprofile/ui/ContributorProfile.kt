package cz.dolezal.gitcontributorprofile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import cz.dolezal.gitcontributorprofile.domain.Author
import cz.dolezal.gitcontributorprofile.domain.ImmutableContributionStats
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cz.dolezal.gitcontributorprofile.domain.ContributorProfileService
import cz.dolezal.gitcontributorprofile.domain.LanguageStat
import cz.dolezal.gitcontributorprofile.ui.controls.JewelPreview
import cz.dolezal.gitcontributorprofile.ui.controls.Prompt
import io.github.koalaplot.core.pie.BezierLabelConnector
import io.github.koalaplot.core.pie.CircularLabelPositionProvider
import io.github.koalaplot.core.pie.DefaultSlice
import io.github.koalaplot.core.pie.PieChart
import io.github.koalaplot.core.pie.PieLabelPlacement
import io.github.koalaplot.core.pie.PieSliceScope
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.util.generateHueColorPalette
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

        if (stats.languages.isNotEmpty()) {
            GroupHeader(text = "Languages")
            LanguagesChart(stats.languages)
        }
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
            modifier = Modifier.width(80.dp),
            text = "$label:",
            maxLines = 1,
            style = JewelTheme.typography.medium,
            color = JewelTheme.globalColors.text.info,
        )
        InfoText(
            modifier = Modifier.width(40.dp),
            text = value.toString(),
            textAlign = TextAlign.End,
            maxLines = 1,
            style = JewelTheme.typography.medium,
            color = JewelTheme.globalColors.text.normal,
        )
    }
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
private fun LanguagesChart(
    languages: ImmutableList<LanguageStat>,
) {
    val values = remember(languages) { languages.map { language -> language.count.toFloat() } }
    val colors = remember(languages) { generateHueColorPalette(languages.size) }

    PieChart(
        modifier = Modifier.size(300.dp),
        values = values,
        labelPositionProvider = CircularLabelPositionProvider(
            labelSpacing = 1.1f,
            labelPlacement = PieLabelPlacement.External,
        ),
        slice = { index ->
            LanguageSlice(
                color = colors[index],
                language = languages[index],
            )
        },
        label = { index ->
            InfoText(
                text = languages[index].name,
                maxLines = 1,
                style = JewelTheme.typography.medium,
                color = JewelTheme.globalColors.text.normal,
            )
        },
        labelConnector = { index ->
            BezierLabelConnector(
                connectorColor = colors[index],
            )
        },
        holeSize = 0.7f,
        holeContent = { },
        maxPieDiameter = Dp.Infinity,
        forceCenteredPie = false,
    )
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
private fun PieSliceScope.LanguageSlice(
    color: Color,
    language: LanguageStat,
) {
    val hoverText = remember(language) {
        if (language.count == 1) "${language.count} file" else "${language.count} files"
    }
    DefaultSlice(
        color = color,
        border = BorderStroke(
            width = 6.dp,
            color = lerp(color, Color.White, 0.2f),
        ),
        hoverExpandFactor = 1.05f,
        hoverElement = {
            InfoText(
                text = hoverText,
                maxLines = 1,
                style = JewelTheme.typography.medium,
                color = JewelTheme.globalColors.text.normal,
            )
        },
        antiAlias = true,
        gap = 0f,
    )
}

@Composable
@Preview
private fun LoadingPreview() {
    JewelPreview {
        Loading()
    }
}
