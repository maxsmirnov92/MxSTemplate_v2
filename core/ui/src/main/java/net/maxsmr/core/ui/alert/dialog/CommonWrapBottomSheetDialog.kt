package net.maxsmr.core.ui.alert.dialog

import android.content.Context
import android.os.Bundle
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseAdapterData
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDelegationAdapter
import net.maxsmr.android.recyclerview.views.decoration.Divider
import net.maxsmr.android.recyclerview.views.decoration.DividerItemDecoration
import net.maxsmr.commonutils.gui.setTextOrGone
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.ui.R
import net.maxsmr.core.ui.databinding.ItemBottomSheetDialogAnswerBinding
import net.maxsmr.core.ui.databinding.LayoutBottomSheetDialogCommonWrapBinding

class CommonWrapBottomSheetDialog(
    context: Context,
    alert: Alert,
    cancelable: Boolean = true,
) : BaseCustomBottomSheetDialog(
    context,
    layoutResId = R.layout.layout_bottom_sheet_dialog_common_wrap,
    alert = alert,
    cancelable = cancelable,
    shouldMatchHeight = false,
    initialState = BottomSheetState.STATE_COLLAPSED
) {

    private val binding by lazy {
        LayoutBottomSheetDialogCommonWrapBinding.bind(wrappedContentView)
    }

    private val adapter = AnswerAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        alert ?: throw IllegalStateException("Alert must not be null")

        with(binding) {
            tvTitle.setTextOrGone(alert.title?.get(context))
            tvMessage.text = alert.message?.get(context)
            rvAnswers.adapter = adapter
            rvAnswers.addItemDecoration(
                DividerItemDecoration.Builder(context)
                    .setDivider(Divider.Space(4), DividerItemDecoration.Mode.ALL_EXCEPT_LAST)
                    .build()
            )
            adapter.items = alert.answers.filter {
                it.title.get(context).isNotEmpty()
            }.map { AnswerAdapterData(it) }
        }
    }

    private fun answerAdapterDelegate() =
        adapterDelegate<AnswerAdapterData, BaseAdapterData>(
            R.layout.item_bottom_sheet_dialog_answer,
        ) {
            bind {
                with(ItemBottomSheetDialogAnswerBinding.bind(itemView).btAnswer) {
                    text = item.answer.title.get(context)
                    setOnAnswerClickListener(item.answer)
                }
            }
        }

    inner class AnswerAdapter : BaseDelegationAdapter<BaseAdapterData>(
        answerAdapterDelegate()
    )

    private data class AnswerAdapterData(
        val answer: Alert.Answer,
    ) : BaseAdapterData {

        override fun isSame(other: BaseAdapterData): Boolean {
            return if (other !is AnswerAdapterData) {
                false
            } else {
                answer != other.answer
            }
        }
    }
}


