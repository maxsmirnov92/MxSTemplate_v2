package net.maxsmr.feature.download.ui.adapter

import android.view.View
import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout
import com.hannesdorfmann.adapterdelegates4.dsl.v2.AdapterDelegateViewHolder
import com.hannesdorfmann.adapterdelegates4.dsl.v2.adapterDelegate
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseAdapterData
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.gui.setInputError
import net.maxsmr.commonutils.gui.setTextWithSelectionToEnd
import net.maxsmr.commonutils.live.field.Field
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.ui.views.edit.TextChangeListener
import net.maxsmr.feature.download.ui.R
import net.maxsmr.feature.download.ui.databinding.ItemHeaderBinding

val logger: BaseLogger = BaseLoggerHolder.instance.getLogger("HeadersDelegateAdapter")

fun headersDelegateAdapter(listener: HeaderListener) =
    adapterDelegate<HeaderInfoAdapterData, HeaderInfoAdapterData, HeadersViewHolder>(
        R.layout.item_header,
        createViewHolder = { HeadersViewHolder(it) }
    ) {

        val binding = ItemHeaderBinding.bind(itemView)

        with(binding) {
            ibRemove.setOnClickListener { listener.onRemoveHeader(item.id) }

            bind {

                addNameTextList(etName) { s, _, _, _ ->
                    lastInputEdit = etName
                    listener.onHeaderNameChanged(item.id, s.toString())
                }
                addValueTextList(etValue) { s, _, _, _ ->
                    lastInputEdit = etValue
                    listener.onHeaderValueChanged(item.id, s.toString())
                }

                fun TextInputLayout.apply(info: HeaderInfoAdapterData.Info) {
                    val editText = editText ?: return

                    editText.setTextWithSelectionToEnd(info.value)
                    hint = info.hint?.get(context) {
                        "$it ${item.id+1}"
                    }
                    setInputError(info.error?.get(context))

                    lastInputEdit?.let {
                        it.requestFocus()
                        lastInputEdit = null
                    }
                }

                tilName.apply(item.header.first)
                tilValue.apply(item.header.second)
            }
        }
    }


data class HeaderInfoAdapterData(
    val id: Int,
    val header: Pair<Info, Info>,
) : BaseAdapterData/*, Serializable*/ {

    override fun isSame(other: BaseAdapterData): Boolean = id == (other as? HeaderInfoAdapterData)?.id

    // TODO Field.Hint Serializable
    data class Info(
        val value: String,
        val hint: Field.Hint?,
        val error: TextMessage?,
    ) /*: Serializable*/
}

interface HeaderListener {

    fun onHeaderNameChanged(id: Int, value: String)

    fun onHeaderValueChanged(id: Int, value: String)

    fun onRemoveHeader(id: Int)
}

class HeadersViewHolder(view: View) : AdapterDelegateViewHolder<HeaderInfoAdapterData>(view) {

    internal var lastInputEdit: EditText? = null

    private var nameTextListener: TextChangeListener? = null
    private var valueTextListener: TextChangeListener? = null

    private var nameEdit: EditText? = null
    private var valueEdit: EditText? = null

    init {
        onViewRecycled {
            logger.d("onViewRecycled")
            removeNameTextListener()
            removeValueTextListener()
        }
    }

    fun addNameTextList(nameEdit: EditText, listener: TextChangeListener) {
        logger.d("addNameTextList: nameEdit=$nameEdit")
        removeNameTextListener()
        nameTextListener = listener
        nameEdit.addTextChangedListener(listener)
        this.nameEdit = nameEdit
    }

    fun addValueTextList(valueEdit: EditText, listener: TextChangeListener) {
        logger.d("addValueTextList: valueEdit=$valueEdit")
        removeValueTextListener()
        valueTextListener = listener
        valueEdit.addTextChangedListener(listener)
        this.valueEdit = valueEdit
    }

    private fun removeNameTextListener() {
        logger.d("removeNameTextListener: nameEdit=$nameEdit")
        nameTextListener?.let {
            nameEdit?.removeTextChangedListener(it)
            nameTextListener = null
        }
    }

    private fun removeValueTextListener() {
        logger.d("removeValueTextListener: valueEdit=$valueEdit")
        valueTextListener?.let {
            valueEdit?.removeTextChangedListener(it)
            valueTextListener = null
        }
    }
}