package cz.dolezal.gitcontributorprofile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cz.dolezal.gitcontributorprofile.domain.Author
import cz.dolezal.gitcontributorprofile.domain.ImmutableContributionStats
import cz.dolezal.gitcontributorprofile.ui.controls.Prompt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import org.jetbrains.jewel.foundation.lazy.SelectableLazyColumn
import org.jetbrains.jewel.foundation.lazy.SelectionMode
import org.jetbrains.jewel.foundation.lazy.items
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.GroupHeader
import org.jetbrains.jewel.ui.component.HorizontalSplitLayout
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.InfoText
import org.jetbrains.jewel.ui.component.rememberSplitLayoutState
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.painter.PainterHint
import org.jetbrains.jewel.ui.typography

@Composable
internal fun MasterDetail(
    authors: ImmutableList<Author>,
    contributions: ImmutableMap<Author, ImmutableContributionStats>,
) {
    val splitLayoutState = rememberSplitLayoutState(
        initialSplitFraction = 0.4f,
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
                Detail(stats)
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
        contentPadding = PaddingValues(end = 8.dp),
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
            .padding(bottom = 2.dp)
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
        modifier = Modifier.padding(start = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GroupHeader(text = "Commits")
        DetailRow(label = "Commits", value = stats.commits)

        GroupHeader(text = "Files")
        DetailRow(label = "Added", value = stats.addedFiles)
        DetailRow(label = "Removed", value = stats.removedFiles)
        DetailRow(label = "Moved", value = stats.movedFiles)
        DetailRow(label = "Modified", value = stats.modifiedFiles)

        if (stats.languages.isNotEmpty()) {
            LanguageChart(stats.languages)
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
