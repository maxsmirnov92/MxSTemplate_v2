package net.maxsmr.designsystem.compose

import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Modifier.motionEventListener(
    hasTimePassedFunc: (Long) -> Boolean,
    onMotionEvent: (MotionEvent) -> Boolean,
    onClickEvent: () -> Unit
): Modifier {
    var startTime by remember { mutableLongStateOf(0) }
    return pointerInteropFilter {
        when (it.action) {
            MotionEvent.ACTION_DOWN -> {
                startTime = System.currentTimeMillis()
                onMotionEvent(it)
            }

            MotionEvent.ACTION_UP -> {
                if (!hasTimePassedFunc(startTime)) {
                    onClickEvent()
                    startTime = 0
                    true
                } else {
                    onMotionEvent(it)
                }
            }

            else -> onMotionEvent(it)
        }
    }
}

@Composable
fun Modifier.detectSwipeGestures(
    positionChangeThreshold: Float = 0f,
    onSwipe: (SwipeDirection) -> Boolean,
): Modifier {
    return pointerInput(Unit) {
        // detectDragGestures/detectVerticalDragGestures препятствует дальнейшему срабатыванию
        // MotionEvent в других листенерах
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val drag = event.changes.firstOrNull() ?: return@awaitPointerEventScope

                if (drag.pressed) {
                    val dx = drag.positionChange().x
                    val dy = drag.positionChange().y

                    val consumed = if (abs(dy) > abs(dx)) {
                        if (abs(dy) > positionChangeThreshold) {
                            onSwipe(
                                if (dy < 0) {
                                    SwipeDirection.UP
                                } else {
                                    SwipeDirection.DOWN
                                }
                            )
                        } else {
                            false
                        }
                    } else {
                        if (abs(dx) > positionChangeThreshold) {
                            onSwipe(
                                if (dx < 0) {
                                    SwipeDirection.LEFT
                                } else {
                                    SwipeDirection.RIGHT
                                }
                            )
                        } else {
                            false
                        }
                    }
                    if (consumed) {
                        drag.consume()
                    }
                }
            }
        }
    }
}

enum class SwipeDirection {

    UP, DOWN, LEFT, RIGHT;

    val isVertical get() = this in listOf(UP, DOWN)

    val isHorizontal get() = this in listOf(LEFT, RIGHT)
}