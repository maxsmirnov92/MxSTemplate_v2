package net.maxsmr.feature.compose_sample.ui.presentation.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension

@Preview
@Composable
fun ConstrainLayoutHorizontalSample() {
    val longText = "long text 11111111111111111111111111111111111111111111"
    val shortText = "short text"

    ConstraintLayout(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .fillMaxHeight()
            .fillMaxWidth() // Заполняем весь доступный родительский контейнер
    ) {
        val (left, right) = createRefs()

        // Блок с текстом 1 (он будет слева и займет все пространство ДО box)
        Text(
            text = longText,
            fontSize = 14.sp,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier
                .background(Color.Green)
                .constrainAs(left) {
                    start.linkTo(parent.start)
                    end.linkTo(right.start, margin = 8.dp) // Ограничиваем правую границу
                    width = Dimension.fillToConstraints // Фиксируем в пределах
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    height = Dimension.wrapContent // по дефолту
                }
        )

        // Блок с текстом 2 (он будет справа и займет только необходимое место)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .background(Color.Blue)
                .constrainAs(right) {
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    height =
                        Dimension.matchParent // это + Alignment.Center или без Aligment с height = Dimension.wrapContent
                }
                .wrapContentWidth() // Минимально возможная ширина
        ) {
            Text(
                text = shortText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 20.sp,
                modifier = Modifier.wrapContentHeight()
            )
        }
    }
}

@Preview
@Composable
fun ConstrainLayoutWithMiddleHorizontalSample() {
    val longText = "long text 11111111111111111111111111111111111111111111"
    val shortText1 = "short text 1"
    val shortText2 = "short text 2"

    ConstraintLayout(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .fillMaxHeight()
            .fillMaxWidth()
    ) {
        val (left, middle, right) = createRefs()


        Text(
            text = shortText2,
            modifier = Modifier
                .background(Color.Green)
                .constrainAs(left) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    height = Dimension.wrapContent
                }.wrapContentWidth()
        )

        Text(
            text = longText,
            fontSize = 14.sp,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier
                .constrainAs(middle) {
                    start.linkTo(left.end, margin = 8.dp)
                    end.linkTo(right.start, margin = 8.dp)
                    width = Dimension.fillToConstraints
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    height = Dimension.wrapContent
                }
        )

        Box(
            modifier = Modifier
                .background(Color.Blue)
                .constrainAs(right) {
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    height = Dimension.wrapContent
                }
                .wrapContentWidth()
        ) {
            Text(
                text = shortText1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 20.sp,
                modifier = Modifier.wrapContentHeight()
            )
        }
    }
}



@Preview
@Composable
fun ConstrainLayoutVerticalSample() {
    ConstraintLayout(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxHeight()
            .fillMaxWidth()
    ) {
        val (up, down) = createRefs()

        Box(
            modifier = Modifier
                .background(Color.Green)
                .constrainAs(up) {
                    end.linkTo(parent.end)
                    start.linkTo(parent.start)
                    width = Dimension.matchParent
                    top.linkTo(parent.top)
                    bottom.linkTo(down.top,margin = 8.dp)
                    height = Dimension.fillToConstraints
                    // или для math: Dimension.percent(1f)
                }
        )

        Box(
            modifier = Modifier
                .background(Color.Blue)
                .height(100.dp)
                .constrainAs(down) {
                    end.linkTo(parent.end)
                    start.linkTo(parent.start)
                    width = Dimension.matchParent
                    bottom.linkTo(parent.bottom)
                }
        )
    }
}

@Preview
@Composable
fun ConstrainLayoutWithMiddleVerticalSample() {
    ConstraintLayout(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxHeight()
            .fillMaxWidth()
    ) {
        val (up, middle, down) = createRefs()

        Box(
            modifier = Modifier
                .background(Color.Green)
                .height(100.dp)
                .constrainAs(up) {
                    end.linkTo(parent.end)
                    start.linkTo(parent.start)
                    width = Dimension.matchParent
                    top.linkTo(parent.top)
                }
        )

        Box(
            modifier = Modifier
                .background(Color.Red)
                .constrainAs(middle) {
                    end.linkTo(parent.end)
                    start.linkTo(parent.start)
                    width = Dimension.matchParent
                    top.linkTo(up.bottom,  margin = 8.dp)
                    bottom.linkTo(down.top,  margin = 8.dp)
                    height = Dimension.fillToConstraints
                }
        )

        Box(
            modifier = Modifier
                .background(Color.Blue)
                .height(100.dp)
                .constrainAs(down) {
                    end.linkTo(parent.end)
                    start.linkTo(parent.start)
                    width = Dimension.matchParent
                    bottom.linkTo(parent.bottom)
                }
        )
    }
}