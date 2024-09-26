package net.maxsmr.feature.about

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Bundle
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
import net.maxsmr.feature.about.AboutViewModel.AboutAppDescription.DonateInfo.PaymentAddress
import net.maxsmr.feature.about.adapter.DonateAddressAdapter
import net.maxsmr.feature.about.adapter.DonateAddressAdapterData
import net.maxsmr.feature.about.adapter.DonateAddressClickListener
import net.maxsmr.feature.about.databinding.FragmentAboutBinding

abstract class BaseAboutFragment<VM : AboutViewModel> : BaseNavigationFragment<VM>(), DonateAddressClickListener {

    abstract val description: AboutViewModel.AboutAppDescription

    override val layoutId: Int = R.layout.fragment_about

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
                val res = requireContext().resources
                ivLogo.updateLayoutParams<ViewGroup.LayoutParams> {
                    this.width = res.convertAnyToPx(it.width.toFloat()).toInt()
                    this.height = res.convertAnyToPx(it.height.toFloat()).toInt()
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

    override fun onAddressClick(address: PaymentAddress) {
        requireContext().copyToClipboard("payment address", address.address)
        viewModel.showToast(TextMessage(net.maxsmr.core.ui.R.string.toast_copied_to_clipboard_message))
    }

}