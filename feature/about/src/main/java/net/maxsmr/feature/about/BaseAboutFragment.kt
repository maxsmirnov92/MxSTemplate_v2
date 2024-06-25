package net.maxsmr.feature.about

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import net.maxsmr.commonutils.copyToClipboard
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.gui.setTextOrGone
import net.maxsmr.core.android.base.actions.ToastAction
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.feature.about.BaseAboutViewModel.AboutAppDescription.DonateInfo.PaymentAddress
import net.maxsmr.feature.about.adapter.DonateAddressAdapter
import net.maxsmr.feature.about.adapter.DonateAddressAdapterData
import net.maxsmr.feature.about.adapter.DonateAddressClickListener
import net.maxsmr.feature.about.databinding.FragmentAboutBinding

abstract class BaseAboutFragment<VM : BaseAboutViewModel> : BaseNavigationFragment<VM>(), DonateAddressClickListener {

    abstract val description: BaseAboutViewModel.AboutAppDescription

    override val layoutId: Int = R.layout.fragment_about

    override val menuResId: Int = R.menu.menu_about

    private val binding by viewBinding(FragmentAboutBinding::bind)

    private val adapter by lazy { DonateAddressAdapter(this) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: VM) {
        super.onViewCreated(view, savedInstanceState, viewModel)
        val description = description
        with(binding) {
            ivLogo.setImageResource(description.logoResId)
            tvName.setTextOrGone(description.name)
            tvVersion.setTextOrGone(description.version)
            tvAppDescription.text = description.description?.takeIf { it.isNotEmpty() }
                ?: getString(R.string.about_app_description_text)
            val info = description.donateInfo
            containerDonate.isVisible = info?.addresses?.isNotEmpty() == true
            if (info != null) {
                tvDonateDescription.text = info.description?.takeIf { it.isNotEmpty() }
                    ?: getString(R.string.about_donate_description_text)
                adapter.items = info.addresses.map { DonateAddressAdapterData(it) }
                rvDonation.adapter = adapter
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.actionRateApp -> {
                viewModel.onRateAppSelected()
                true
            }

            R.id.actionFeedback -> {
                viewModel.navigateToFeedback()
                true
            }

            else -> {
                super.onMenuItemSelected(menuItem)
            }
        }
    }

    override fun onAddressClick(address: PaymentAddress) {
        copyToClipboard(requireContext(), "payment address", address.address)
        viewModel.showToast(ToastAction(TextMessage(net.maxsmr.core.ui.R.string.toast_copied_to_clipboard_message)))
    }
}