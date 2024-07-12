package net.maxsmr.feature.rate.fragment

import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import net.maxsmr.commonutils.gui.bindToTextNotNull
import net.maxsmr.commonutils.live.field.observeFromText
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.core.ui.fields.bindHintError
import net.maxsmr.feature.rate.R
import net.maxsmr.feature.rate.databinding.FragmentFeedbackBinding

abstract class BaseFeedbackFragment<VM : BaseFeedbackViewModel>: BaseVmFragment<VM>() {

    override val layoutId: Int = R.layout.fragment_feedback

    private val binding by viewBinding(FragmentFeedbackBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: VM) {
        binding.etSubject.bindToTextNotNull(viewModel.subjectField)
        viewModel.subjectField.observeFromText(binding.etSubject, viewLifecycleOwner)
        viewModel.subjectField.bindHintError(viewLifecycleOwner, binding.tilSubject)

        binding.etText.bindToTextNotNull(viewModel.textField)
        viewModel.textField.observeFromText(binding.etText, viewLifecycleOwner)
        viewModel.textField.bindHintError(viewLifecycleOwner, binding.etText)

        viewModel.isSendEnabled.observe {
            binding.btSend.isEnabled = it
        }

        binding.btSend.setOnClickListener {
            onSendClick()
        }
    }

    @CallSuper
    protected open fun onSendClick() {
        viewModel.openEmailIntent(requireContext())
    }
}