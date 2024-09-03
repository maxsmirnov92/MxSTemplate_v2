package net.maxsmr.address_sorter.ui.fragment

import android.util.Size
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.address_sorter.BuildConfig
import net.maxsmr.address_sorter.R
import net.maxsmr.core.di.DI_NAME_VERSION_NAME
import net.maxsmr.feature.about.AboutViewModel
import net.maxsmr.feature.about.AboutViewModel.AboutAppDescription
import net.maxsmr.feature.about.AboutViewModel.AboutAppDescription.DonateInfo.PaymentAddress
import net.maxsmr.feature.about.BaseAboutFragment
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class MainAboutFragment : BaseAboutFragment<AboutViewModel>() {

    override val description by lazy {
        AboutAppDescription(
            R.mipmap.ic_launcher,
            Size(286, 286),
            getString(R.string.app_name),
            versionName,
            donateInfo = AboutAppDescription.DonateInfo(
                addresses = BuildConfig.DEV_PAYMENT_ADDRESSES.map { PaymentAddress(it.key, it.value) }
            ),
        )
    }

    override val viewModel: AboutViewModel by viewModels()

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    @Inject
    @Named(DI_NAME_VERSION_NAME)
    lateinit var versionName: String
}