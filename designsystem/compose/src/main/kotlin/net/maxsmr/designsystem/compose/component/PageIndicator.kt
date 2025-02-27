package net.maxsmr.designsystem.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.maxsmr.designsystem.compose.component.PageIndicatorContent.PageInfo.IndicatorType

@Composable
fun PageIndicator(
    content: PageIndicatorContent,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    onItemClick: ((Int) -> Unit)? = null
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        val secondaryColor = color.copy(alpha = 0.5f)

        repeat(content.pagesAmount) {

            when {
                it > content.activePage.index -> {
                    ColoredBox(
                        color = secondaryColor,
                        onItemClick?.let { click -> { click.invoke(it) } }
                    )
                }

                it == content.activePage.index -> {
                    val indicatorType = content.activePage.indicatorType

                    if (indicatorType is IndicatorType.WithProgress) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .weight(1f)
                                .let { modifier ->
                                    onItemClick?.let { onItemClick ->
                                        modifier.clickable { onItemClick.invoke(it) }
                                    } ?: modifier
                                }) {
                            RoundRectDrawProgress(
                                fillColor = color,
                                progressFlow = indicatorType.percentFlow,
                                modifier = Modifier
                                    .height(2.dp)
                                    .fillMaxWidth()
                                    .align(Alignment.Center)
                                    .background(
                                        shape = CircleShape,
                                        color = secondaryColor
                                    ),
                            )
                        }
                    } else {
                        ColoredBox(
                            color = color,
                            onItemClick?.let { click -> { click.invoke(it) } }
                        )
                    }
                }

                it < content.activePage.index -> {
                    ColoredBox(
                        color = color,
                        onItemClick?.let { click -> { click.invoke(it) } }
                    )
                }
            }
            Spacer(Modifier.size(4.dp))
        }
    }
}

@Composable
private fun RowScope.ColoredBox(color: Color, onClick: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .weight(1f)
            .let { modifier ->
                onClick?.let {
                    modifier.clickable {
                        it.invoke()
                    }
                } ?: modifier
            }
    ) {
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth()
                .align(Alignment.Center)
                .background(
                    color = color,
                    shape = CircleShape
                )
        )
    }
}

data class PageIndicatorContent(
    val pagesAmount: Int,
    val activePage: PageInfo,
) {

    /**
     * @param index индекс страницы
     * @param indicatorType тип индикатора: простой или с заполнением
     */
    data class PageInfo(
        val index: Int,
        val indicatorType: IndicatorType
    ) {

        sealed interface IndicatorType {

            data object Simple : IndicatorType

            class WithProgress(
                val percentFlow: StateFlow<Float>
            ) : IndicatorType
        }
    }
}