package net.maxsmr.feature.about

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import net.maxsmr.commonutils.convertAnyToPx
import net.maxsmr.commonutils.copyToClipboard
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.gui.setTextOrGone
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
            tvName.setTextOrGone(description.name)
            tvVersion.setTextOrGone(description.version)
            tvAppDescription.text = description.description?.takeIf { it.isNotEmpty() }
                ?: getString(R.string.about_app_description_text)
            val donateInfo = description.donateInfo
            containerDonate.isVisible = donateInfo?.addresses?.isNotEmpty() == true
            if (donateInfo != null) {
                tvDonateDescription.text = donateInfo.description?.takeIf { it.isNotEmpty() }
                    ?: getString(R.string.about_donate_description_text)
                adapter.items = donateInfo.addresses.map { DonateAddressAdapterData(it) }
                rvDonation.adapter = adapter
            }

            description.logoSize?.takeIf {
                it.width > 0 && it.height > 0
            }?.let {
                ivLogo.updateLayoutParams<ViewGroup.LayoutParams> {
                    this.width = convertAnyToPx(it.width.toFloat(), context = requireContext()).toInt()
                    this.height = convertAnyToPx(it.height.toFloat(), context = requireContext()).toInt()
                }
            }

            description.easterEggInfo?.let { eggInfo ->
                ivLogo.setOnClickListener {
                    viewModel.onLogoClick(eggInfo)
                }
                viewModel.animatedLogoState.observe {
                    // может быть animated-vector или animation-list
                    val animatedLogo = ContextCompat.getDrawable(requireContext(), eggInfo.animatedLogoResId) as Animatable
                    if (it) {
                        ivLogo.setImageDrawable(animatedLogo as Drawable)
                        animatedLogo.start()
                    } else {
                        animatedLogo.stop()
                        ivLogo.setImageResource(description.logoResId)
                    }
                }
            }?: run {
                ivLogo.setImageResource(description.logoResId)
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
        viewModel.showToast(TextMessage(net.maxsmr.core.ui.R.string.toast_copied_to_clipboard_message))
    }
}