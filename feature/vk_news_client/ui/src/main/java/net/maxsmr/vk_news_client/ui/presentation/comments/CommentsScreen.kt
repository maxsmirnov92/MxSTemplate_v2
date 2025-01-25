package net.maxsmr.vk_news_client.ui.presentation.comments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.feature.vk_news_client.ui.R
import net.maxsmr.vk_news_client.ui.model.FeedPostCommentUI
import net.maxsmr.vk_news_client.ui.model.FeedPostUI
import net.maxsmr.vk_news_client.ui.presentation.main.EmptyErrorScreen
import net.maxsmr.vk_news_client.ui.presentation.main.LoadingScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsScreen(
    viewModel: CommentsViewModel,
    modifier: Modifier,
    post: FeedPostUI,
    onBackPressed: () -> Unit,
) {
    Scaffold(modifier = modifier, topBar = {
        // TODO задублированный TopAppBar
        TopAppBar(
            modifier = Modifier.shadow(5.dp),
            title = {
                Text(text = stringResource(R.string.vk_news_client_screen_comments_title_format, post.id))
            },
            navigationIcon = {
                IconButton(onClick = {
                    onBackPressed()
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null
                    )
                }
            },
        )
    }) { paddingValues ->
        val screenState = viewModel.screenState.observeAsState(LoadState.initial()).value
        val thisModifier = Modifier.padding(paddingValues)
        if (screenState.isLoading) {
            LoadingScreen(thisModifier)
        } else {
            if (screenState.hasData { it?.isEmpty == false }) {
                CommentsList(screenState.data?.comments.orEmpty(), thisModifier)
            } else {
                EmptyErrorScreen(screenState, thisModifier) {
                    viewModel.loadComments(post)
                }
            }
        }
    }

}

@Composable
private fun CommentsList(
    comments: List<FeedPostCommentUI>,
    modifier: Modifier = Modifier,
) {
    if (comments.isEmpty()) return
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            top = 16.dp,
            start = 8.dp,
            end = 8.dp,
            bottom = 72.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = comments,
            key = { it.id }
        ) {
            CommentItem(it)
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun CommentItem(
    comment: FeedPostCommentUI,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        GlideImage(
            model = comment.authorAvatarUrl,
            contentDescription = null,
            modifier = Modifier
                .align(CenterVertically)
                .clip(CircleShape)
                .size(48.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = comment.authorName,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = comment.commentText,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = comment.publicationDate,
                color = MaterialTheme.colorScheme.onSecondary,
                fontSize = 12.sp
            )
        }
    }
}