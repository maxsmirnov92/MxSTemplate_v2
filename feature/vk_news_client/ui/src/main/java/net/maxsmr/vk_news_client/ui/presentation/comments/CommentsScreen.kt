package net.maxsmr.vk_news_client.ui.presentation.comments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import net.maxsmr.commonutils.flow.field.Field
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
    onContent: @Composable () -> Unit,
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
        onContent()
        val screenState = viewModel.screenState.observeAsState(LoadState.initial()).value
        val thisModifier = Modifier.padding(paddingValues)
        if (screenState.isLoading) {
            LoadingScreen(thisModifier)
        } else {
            val commentsLoadState = viewModel.commentsLoadState.observeAsState(LoadState.initial()).value
            ConstraintLayout(modifier = thisModifier.fillMaxSize()) {
                val (content, commentSend) = createRefs()
                Column(Modifier
                    .constrainAs(content) {
                        top.linkTo(parent.top)
                        bottom.linkTo(commentSend.top)
                        height = androidx.constraintlayout.compose.Dimension.fillToConstraints
                    }
                    .fillMaxWidth()
                ) {
                    if (screenState.hasData { it?.isEmpty == false }) {
                        CommentsList(screenState.data?.comments.orEmpty())
                    } else {
                        EmptyErrorScreen(screenState) {
                            viewModel.loadComments()
                        }
                    }
                }
                CommentSendContainer(
                    viewModel.commentField,
                    !commentsLoadState.isLoading,
                    {
                        viewModel.createComment()
                    },
                    Modifier
                        .constrainAs(commentSend) {
                            bottom.linkTo(parent.bottom)
                        }
                        .fillMaxWidth()
                )
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
            HorizontalDivider(
                color = MaterialTheme.colorScheme.secondary,
                thickness = 1.dp
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun CommentItem(comment: FeedPostCommentUI) {
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

@Composable
private fun CommentSendContainer(
    field: Field<String>,
    isSendEnabled: Boolean,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ConstraintLayout(modifier.padding(horizontal = 4.dp)) {
        val (edit, send) = createRefs()

        val textState =
            field.valueFlow.collectAsState()
        val hintState = field.hintFlow.collectAsState()
        val errorState = field.errorFlow.collectAsState()

        OutlinedTextField(
            value = textState.value,
            label = {
                hintState.value?.get(LocalContext.current)?.toString()?.let { hint ->
                    Text(hint)
                }
            },
            isError = field.hasError,
            supportingText = {
                if (field.hasError) {
                    errorState.value?.get(LocalContext.current)?.toString()?.let { error ->
                        Text(error)
                    }
                }
            },
            onValueChange = {
                field.value = it
            },
            modifier = Modifier
                .constrainAs(edit) {
                    start.linkTo(parent.start)
                    end.linkTo(send.start, margin = 8.dp)
                    width = androidx.constraintlayout.compose.Dimension.fillToConstraints
                }
                .wrapContentHeight(),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .constrainAs(send) {
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    height = androidx.constraintlayout.compose.Dimension.fillToConstraints
                }
        ) {
            IconButton(
                {
                    onSendClick()
                },
                enabled = isSendEnabled,
                modifier = Modifier,
            ) {
                Icon(
                    painterResource(R.drawable.ic_send),
                    contentDescription = null
                )
            }
        }
    }
}