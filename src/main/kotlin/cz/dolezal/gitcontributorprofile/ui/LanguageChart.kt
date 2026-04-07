package cz.dolezal.gitcontributorprofile.ui

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cz.dolezal.gitcontributorprofile.domain.LanguageStat
import io.github.koalaplot.core.animation.StartAnimationUseCase
import io.github.koalaplot.core.pie.BezierLabelConnector
import io.github.koalaplot.core.pie.CircularLabelPositionProvider
import io.github.koalaplot.core.pie.DefaultSlice
import io.github.koalaplot.core.pie.PieChart
import io.github.koalaplot.core.pie.PieLabelPlacement
import io.github.koalaplot.core.pie.PieSliceScope
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.util.generateHueColorPalette
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.GroupHeader
import org.jetbrains.jewel.ui.component.InfoText
import org.jetbrains.jewel.ui.typography

@Composable
internal fun LanguageChart(
    languages: ImmutableList<LanguageStat>,
) {
    val slices = rememberPieSlices(languages)
    GroupHeader(text = "Languages")

    /**
     * key forces Compose to discard internal state of the Koala chart
     * Without IndexOutOfBoundsException crashes happen when switching between users
     * Some parts such as `labelConnector` rely on lambda which is out of sync with recomposition
     */
    key(slices) {
        LanguagePieChart(slices)
    }
}

@Immutable
private data class PieSliceUi(
    val color: Color,
    val count: Int,
    val name: String,
    val value: Float,
)

@Composable
private fun rememberPieSlices(
    languages: ImmutableList<LanguageStat>,
): ImmutableList<PieSliceUi> =
    remember(languages) {
        val colors = generateHueColorPalette(languages.size)

        languages.mapIndexed { index, language ->
            PieSliceUi(
                color = colors[index],
                count = language.count,
                name = language.name,
                value = language.count.toFloat(),
            )
        }
            .toImmutableList()
    }

@Composable
private fun createDisabledAnimationUseCase() = StartAnimationUseCase(
    executionType = StartAnimationUseCase.ExecutionType.None,
    // chart animation
    KoalaPlotTheme.animationSpec,
    // label animation
    tween(
        durationMillis = 1000,
        delayMillis = 0,
        easing = LinearOutSlowInEasing,
    ),
)

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
private fun LanguagePieChart(
    slices: ImmutableList<PieSliceUi>,
) {
    PieChart(
        modifier = Modifier.size(300.dp),
        values = slices.map { slice -> slice.value },
        labelPositionProvider = CircularLabelPositionProvider(
            labelSpacing = 1.1f,
            labelPlacement = PieLabelPlacement.External,
        ),
        slice = { index ->
            LanguageSlice(
                slice = slices[index],
            )
        },
        label = { index ->
            InfoText(
                text = slices[index].name,
                maxLines = 1,
                style = JewelTheme.typography.medium,
                color = JewelTheme.globalColors.text.normal,
            )
        },
        labelConnector = { index ->
            BezierLabelConnector(
                connectorColor = slices[index].color,
            )
        },
        startAnimationUseCase = createDisabledAnimationUseCase(),
        holeSize = 0.7f,
        holeContent = { },
        maxPieDiameter = Dp.Infinity,
        forceCenteredPie = false,
    )
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
private fun PieSliceScope.LanguageSlice(
    slice: PieSliceUi,
) {
    val hoverText = remember(slice) {
        if (slice.count == 1) "${slice.count} file" else "${slice.count} files"
    }
    DefaultSlice(
        color = slice.color,
        border = BorderStroke(
            width = 6.dp,
            color = lerp(slice.color, Color.White, 0.2f),
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
