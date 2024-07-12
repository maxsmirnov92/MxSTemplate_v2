package net.maxsmr.mxstemplate.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.di.DI_NAME_VERSION_NAME
import net.maxsmr.core.ui.components.IFragmentDelegate
import net.maxsmr.feature.about.AboutViewModel
import net.maxsmr.feature.about.BaseAboutFragment
import net.maxsmr.feature.about.AboutViewModel.AboutAppDescription
import net.maxsmr.feature.about.AboutViewModel.AboutAppDescription.DonateInfo.PaymentAddress
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.mobile_services.IMobileServicesAvailability
import net.maxsmr.mxstemplate.BuildConfig
import net.maxsmr.mxstemplate.R
import net.maxsmr.mxstemplate.mobileBuildType
import net.maxsmr.mxstemplate.ui.MainAboutViewModel
import net.maxsmr.mxstemplate.ui.delegate.MainRateAppFragmentDelegate
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class MainAboutFragment : BaseAboutFragment<MainAboutViewModel>() {

    override val description by lazy {
        AboutAppDescription(
            R.mipmap.ic_launcher,
            null,
            getString(R.string.app_name),
            versionName,
            donateInfo = AboutAppDescription.DonateInfo(
                addresses = BuildConfig.DEV_PAYMENT_ADDRESSES.map { PaymentAddress(it.key, it.value) }
            ),
            easterEggInfo = AboutAppDescription.EasterEggInfo(
                animatedLogoResId = R.drawable.ic_splashscreen_logo_vector_animated,
                targetClickCount = 10,
                clicksLeftToShowToast = 4,
            )
        )
    }

    override val viewModel: MainAboutViewModel by viewModels()

    override val delegates: List<IFragmentDelegate> by lazy {
        listOf(rateDelegate)
    }

    override val rateDelegate by lazy {
        MainRateAppFragmentDelegate(
            availability,
            mobileBuildType,
            cacheRepo
        )
    }

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    @Inject
    @Named(DI_NAME_VERSION_NAME)
    lateinit var versionName: String

    @Inject
    lateinit var availability: IMobileServicesAvailability

    @Inject
    lateinit var cacheRepo: CacheDataStoreRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: MainAboutViewModel) {
        super.onViewCreated(view, savedInstanceState, viewModel)
        if (viewModel.isForRate) {
            rateDelegate.onRateAppSelected()
        }
    }
}