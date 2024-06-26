package net.maxsmr.justupdownloadit.ui.fragment

import android.util.Size
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.feature.about.BaseAboutFragment
import net.maxsmr.feature.about.BaseAboutViewModel.AboutAppDescription
import net.maxsmr.feature.about.BaseAboutViewModel.AboutAppDescription.DonateInfo.PaymentAddress
import net.maxsmr.justupdownloadit.BuildConfig
import net.maxsmr.justupdownloadit.R
import net.maxsmr.justupdownloadit.ui.AboutViewModel
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

@AndroidEntryPoint
class AboutFragment : BaseAboutFragment<AboutViewModel>() {

    override val description by lazy {
        AboutAppDescription(
            R.mipmap.ic_launcher,
            Size(266, 266),
            getString(R.string.app_name),
            BuildConfig.VERSION_NAME,
            donateInfo = AboutAppDescription.DonateInfo(
                addresses = BuildConfig.DEV_PAYMENT_ADDRESSES.map { PaymentAddress(it.key, it.value) }
            ),
            easterEggInfo = AboutAppDescription.EasterEggInfo(
                animatedLogoResId = R.drawable.ic_logo_animated,
                targetClickCount = 10,
                clicksLeftToShowToast = 4,
            )
        )
    }

    override val viewModel: AboutViewModel by viewModels()

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

}