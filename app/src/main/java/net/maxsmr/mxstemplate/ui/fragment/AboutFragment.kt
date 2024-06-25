package net.maxsmr.mxstemplate.ui.fragment

import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.feature.about.BaseAboutFragment
import net.maxsmr.feature.about.BaseAboutViewModel.AboutAppDescription
import net.maxsmr.feature.about.BaseAboutViewModel.AboutAppDescription.DonateInfo.PaymentAddress
import net.maxsmr.mxstemplate.BuildConfig
import net.maxsmr.mxstemplate.R
import net.maxsmr.mxstemplate.ui.AboutViewModel
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

@AndroidEntryPoint
class AboutFragment : BaseAboutFragment<AboutViewModel>() {

    override val description by lazy {
        AboutAppDescription(
            R.mipmap.ic_launcher,
            getString(R.string.app_name),
            BuildConfig.VERSION_NAME,
            donateInfo = AboutAppDescription.DonateInfo(
                addresses = BuildConfig.DEV_PAYMENT_ADDRESSES.map { PaymentAddress(it.key, it.value) }
            )
        )
    }

    override val viewModel: AboutViewModel by viewModels()

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

}