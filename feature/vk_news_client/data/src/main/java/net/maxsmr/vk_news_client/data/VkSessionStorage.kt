package net.maxsmr.vk_news_client.data

import com.vk.id.VKID
import net.maxsmr.core.network.session.SessionStorage

class VkSessionStorage: SessionStorage {

    override val session: String?
        get() = VKID.instance.accessToken?.token
}