package net.maxsmr.vk_news_client.ui.presentation.comments

import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPost
import net.maxsmr.core.ui.compose.components.IComposableViewModelsContainer
import net.maxsmr.core.ui.compose.components.IComposableViewModelsContainer.*
import net.maxsmr.vk_news_client.ui.model.FeedPostUI

class CommentsFactoryArgs(val feedPost: FeedPostUI): IFactoryArgs<CommentsViewModel>