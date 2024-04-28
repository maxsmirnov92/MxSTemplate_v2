package net.maxsmr.designsystem.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.Placeholder


@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun RoundedCornerGlideImage(
    width: Dp,
    height: Dp,
    cornerSize: Dp,
    model: Any?,
    contentDescription: String?,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.TopStart,
    modifier: Modifier = Modifier,
    loading: Placeholder? = null,
    failure: Placeholder? = null,
    onClick: () -> Unit = {},
) {
    Card(
        shape = RoundedCornerShape(cornerSize),
        modifier = Modifier
            .width(width)
            .height(height)
            .clickable(onClick = onClick),
        elevation = 0.dp,

    ) {
        GlideImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = contentScale,
            alignment = alignment,
            loading = loading,
            failure = failure,
            modifier = modifier
                .fillMaxSize()
        )
    }
}
