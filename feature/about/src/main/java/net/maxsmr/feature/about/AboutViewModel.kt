package net.maxsmr.feature.about

import android.os.Handler
import android.os.Looper
import android.text.style.CharacterStyle
import android.util.Size
import androidx.annotation.DrawableRes
import androidx.lifecycle.SavedStateHandle
import net.maxsmr.commonutils.RangeSpanInfo
import net.maxsmr.commonutils.createSpanText
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.alert.queue.AlertQueueItem
import net.maxsmr.core.android.base.delegates.persistableLiveDataInitial
import net.maxsmr.core.android.base.delegates.persistableValueInitial
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import java.io.Serializable

open class AboutViewModel(state: SavedStateHandle) : BaseHandleableViewModel(state) {

    val animatedLogoState by persistableLiveDataInitial(false)

    private val handler = Handler(Looper.getMainLooper())

    private val logoPressedClearRunnable = Runnable {
        logoPressedCount = 0
    }

    private var logoPressedCount by persistableValueInitial(0)

    private var logoAnimatedOnce by persistableValueInitial(false)

    fun onLogoClick(easterEggInfo: AboutAppDescription.EasterEggInfo) {
        with(easterEggInfo) {
            val isAnimated = animatedLogoState.value ?: false
            if (!isAnimated) {
                if (!logoAnimatedOnce) {
                    if (targetClickCount <= 0) return
                    handler.removeCallbacks(logoPressedClearRunnable)
                    logoPressedCount++
                    if (logoPressedCount >= targetClickCount) {
                        removeToastsFromQueue()
                        animatedLogoState.value = true
                        logoAnimatedOnce = true
                    } else if (clicksLeftToShowToast > 0) {
                        val clicksLeft = targetClickCount - logoPressedCount
                        if (clicksLeft <= clicksLeftToShowToast) {
                            showToast(
                                TextMessage(
                                    R.string.about_toast_easter_egg_logo_message_format,
                                    clicksLeft
                                ),
                                // убираем тост, который не успел скрыться системой
                                uniqueStrategy = AlertQueueItem.UniqueStrategy.Replace
                            )
                        }
                    }
                    handler.postDelayed(logoPressedClearRunnable,
                        resetClickDelay.takeIf { it > 0 } ?: DELAY_RESET_CLICK_LOGO_DEFAULT)
                } else {
                    animatedLogoState.value = true
                }
            } else {
                animatedLogoState.value = false
            }
        }
    }

    data class AboutAppDescription(
        @DrawableRes
        val logoResId: Int,
        val logoSize: Size?,
        val name: String,
        val version: String,
        val description: String? = null,
        val donateInfo: DonateInfo? = null,
        val easterEggInfo: EasterEggInfo? = null,
    ) : Serializable {

        data class DonateInfo(
            val description: String? = null,
            val addresses: List<PaymentAddress>,
        ) : Serializable {

            data class PaymentAddress(
                val name: String,
                val address: String,
            ) : Serializable {

                fun toSpanText(style: CharacterStyle): CharSequence {
                    val text = "$name: $address"
                    return text.createSpanText(RangeSpanInfo(name.length + 2, text.length, style))
                }
            }
        }

        data class EasterEggInfo(
            @DrawableRes
            val animatedLogoResId: Int,
            val targetClickCount: Int = CLICK_COUNT_LOGO_DEFAULT,
            val clicksLeftToShowToast: Int = 3,
            val resetClickDelay: Long = DELAY_RESET_CLICK_LOGO_DEFAULT,
        )
    }

    companion object {

        const val CLICK_COUNT_LOGO_DEFAULT = 5

        const val DELAY_RESET_CLICK_LOGO_DEFAULT = 1500L
    }
}