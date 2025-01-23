package net.maxsmr.designsystem.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun SwipeToDismissContainer(
    dismissDirections: Set<SwipeToDismissBoxValue>,
    onDelete: () -> Unit,
    content: @Composable RowScope.() -> Unit,
    iconContent: @Composable (RowScope.() -> Unit)? = null,
    positionalThreshold: Float = 0.3f,
    backgroundColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
) {
    require(positionalThreshold > 0) {
        "positionalThreshold must be > 0"
    }
    require(!dismissDirections.contains(SwipeToDismissBoxValue.Settled)) {
        "dismissDirections cannot contain Settled value"
    }

    // для предотвращения повторного onDelete
    val lastValue = remember { mutableStateOf<SwipeToDismissBoxValue?>(null) }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * positionalThreshold },
        confirmValueChange = { newValue ->
            if (lastValue.value != newValue
                    && dismissDirections.contains(newValue)
            ) {
                lastValue.value = newValue
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            iconContent?.let {
                if (dismissDirections.contains(dismissState.dismissDirection)) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(backgroundColor),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement =
                        if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                            Arrangement.End
                        } else {
                            Arrangement.Start
                        }
                    ) {
                        iconContent()
                    }
                }
            }
        },
        enableDismissFromStartToEnd = dismissDirections.contains(SwipeToDismissBoxValue.StartToEnd),
        enableDismissFromEndToStart = dismissDirections.contains(SwipeToDismissBoxValue.EndToStart),
        content = { content() }
    )
}