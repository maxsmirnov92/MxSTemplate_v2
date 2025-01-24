package net.maxsmr.vk_news_client.ui.presentation.news

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import net.maxsmr.commonutils.conversion.CountUnit
import net.maxsmr.commonutils.format.formatCountSingle
import net.maxsmr.core.domain.entities.feature.vk_news_client.Statistics.StatsType
import net.maxsmr.designsystem.compose.component.SwipeToDismissContainer
import net.maxsmr.designsystem.compose.theme.AppColors.DarkRed
import net.maxsmr.feature.vk_news_client.ui.R
import net.maxsmr.vk_news_client.ui.model.FeedPostUI
import net.maxsmr.vk_news_client.ui.model.StatisticsUI
import net.maxsmr.vk_news_client.ui.model.StatisticsUI.Companion.getItemByType

@Composable
fun FeedPostCardLoading(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            text = stringResource(net.maxsmr.core.android.R.string.loading),
            fontSize = 16.sp,
            fontFamily = FontFamily.Default,
        )
    }
}

@Composable
fun FeedPostCardList(
    viewModel: NewsFeedViewModel,
    posts: List<FeedPostUI>,
    nextPageIsLoading: Boolean,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onCommentClickListener: (FeedPostUI) -> Unit,
) {
    if (posts.isEmpty()) return

//    LaunchedEffect(listState) {
//        snapshotFlow {
//            listState.firstVisibleItemIndex
//        }.collect { index ->
//            if (index >= posts.size - 1 && !nextPageIsLoading) {
//                viewModel.loadNextRecommendations()
//            }
//        }
//    }

    LazyColumn(
        contentPadding = PaddingValues(
            top = 16.dp,
            start = 8.dp,
            end = 8.dp,
            bottom = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        state = listState,
        modifier = modifier
    ) {
        items(posts, {
            it.id
        }) { item ->
            SwipeToDismissContainer(
                dismissDirections = setOf(
                    SwipeToDismissBoxValue.EndToStart,
                    SwipeToDismissBoxValue.StartToEnd
                ),
                onDelete = {
                    viewModel.delete(item)
                },
                content = {
                    FeedPostCard(item) { statsItem ->
                        when (statsItem.type) {
                            StatsType.COMMENTS -> {
                                onCommentClickListener(item)
                            }

                            StatsType.LIKES -> {
                                viewModel.changeLikeStatus(item)
                            }

                            StatsType.SHARES -> {
                                viewModel.updateCount(item.id, statsItem.type)
                            }

                            else -> {

                            }
                        }
                    }
                },
                iconContent = {
                    Icon(
                        Icons.Default.Delete,
                        modifier = Modifier.size(40.dp),
                        tint = Color.Red,
                        contentDescription = "delete"
                    )
                },
                backgroundColor = Color.Unspecified,
                // внутри LazyItemScope Modifier для анимации при изменении списка
                modifier = Modifier.animateItem(),
            )
        }
        // при рекомпозиции позиция скролла будет сбиваться,
        // если не указан ключ
        item(key = "loadingIndicator") {
            if (nextPageIsLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // стейт экрана будет меняться не из контекста композиции,
                // т.е. изменение является сайд-эффектом
                SideEffect {
                    viewModel.loadNextRecommendations()
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun FeedPostCard(
    post: FeedPostUI,
    modifier: Modifier = Modifier,
    onStatsItemClickListener: (StatisticsUI) -> Unit,
) {
    Card(
        modifier = modifier,
//        border = BorderStroke(1.dp, MaterialTheme.colors.onBackground),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            ConstraintLayout(
                modifier = Modifier
                    .fillMaxWidth(),
//                verticalAlignment = Alignment.CenterVertically
            ) {
                val (icon, title, moreButton) = createRefs()

                GlideImage(
                    model = post.communityImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .constrainAs(icon) {
                            start.linkTo(parent.start) // Прикреплен к левому краю родителя
                            top.linkTo(parent.top)    // Указано примерное положение
                        }
                )

                Column(
                    modifier = Modifier.constrainAs(title) {
                        start.linkTo(icon.end, margin = 6.dp)  // Начинается от конца левого элемента
                        end.linkTo(moreButton.start, margin = 6.dp) // Заканчивается у начала правого элемента
                        top.linkTo(parent.top)         // Указано примерное положение
                        bottom.linkTo(parent.bottom) // Для привязки по вертикали
                        width =
                            androidx.constraintlayout.compose.Dimension.fillToConstraints // Растягивается между границами
                    }

                ) {
                    Text(
                        text = post.communityName,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Default,
                    )
                    Spacer(Modifier.padding(top = 4.dp))
                    Text(

                        text = post.publicationTime,
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Default,
                    )
                }

                // в Icon по дефолту размеры по гайдлайнам
                Icon(
                    modifier = Modifier.constrainAs(moreButton) {
                        bottom.linkTo(parent.bottom)
                        end.linkTo(parent.end)    // Прикреплен к правому краю родителя
                        top.linkTo(parent.top)    // Указано примерное положение
                    },
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                    // при растровом использовать
                    // tint = Color.Unspecified
                )
            }
            if (post.contentText.isNotEmpty()) {
                Spacer(Modifier.padding(top = 8.dp))
                Text(
                    text = post.contentText,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Default,
                )
            }
            Spacer(Modifier.padding(top = 8.dp))
            GlideImage(
                model = post.contentImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentScale = ContentScale.FillHeight,
            )
            Spacer(Modifier.padding(top = 8.dp))
            Statistics(
                post.statisticsItems,
                post.isLiked,
                onStatsItemClickListener,
            )
        }
    }
}

@Composable
private fun Statistics(
    items: List<StatisticsUI>,
    isLiked: Boolean,
    onItemClickListener: (StatisticsUI) -> Unit,
) {

    Row(modifier = Modifier.fillMaxWidth()) {
        val viewsItem = items.getItemByType(StatsType.VIEWS)
        Row(Modifier.weight(1f)) {
            IconWithText(
                R.drawable.ic_views_count,
                formatStatsCount(viewsItem.count)?.get(LocalContext.current) ?: ""
            )
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val sharesItem = items.getItemByType(StatsType.SHARES)
            IconWithText(
                R.drawable.ic_share,
                formatStatsCount(sharesItem.count)?.get(LocalContext.current) ?: ""
            ) {
                onItemClickListener(sharesItem)
            }
            val commentsItem = items.getItemByType(StatsType.COMMENTS)
            IconWithText(
                R.drawable.ic_comment,
                formatStatsCount(commentsItem.count)?.get(LocalContext.current) ?: ""
            ) { onItemClickListener(commentsItem) }
            val likesItem = items.getItemByType(StatsType.LIKES)
            IconWithText(
                if (isLiked) {
                    R.drawable.ic_like_set
                } else {
                    R.drawable.ic_like
                },
                formatStatsCount(items.getItemByType(StatsType.LIKES).count)?.get(LocalContext.current) ?: "",
                if (isLiked) {
                    DarkRed
                } else {
                    MaterialTheme.colorScheme.onSecondary
                }
            ) { onItemClickListener(likesItem) }
        }
    }
}

@Composable
private fun IconWithText(
    @DrawableRes iconResId: Int,
    countFormatted: CharSequence,
    imageTintColor: Color = MaterialTheme.colorScheme.onSecondary,
    modifier: Modifier = Modifier,
    onItemClickListener: (() -> Unit)? = null,
) {
    Row(
        modifier = onItemClickListener?.let {
            modifier.clickable {
                onItemClickListener()
            }
        } ?: modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = imageTintColor
        )
        Spacer(Modifier.padding(start = 4.dp))
        Text(
            text = countFormatted.toString(),
            color = MaterialTheme.colorScheme.onSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Default,
        )
    }
}

private fun formatStatsCount(count: Int) = formatCountSingle(
    count,
    CountUnit.UNITS,
    precision = 2,
)