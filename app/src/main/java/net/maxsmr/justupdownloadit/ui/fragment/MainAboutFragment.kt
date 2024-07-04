package net.maxsmr.justupdownloadit.ui.fragment

import android.os.Bundle
import android.util.Size
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.di.DI_NAME_VERSION_NAME
import net.maxsmr.core.ui.components.IFragmentDelegate
import net.maxsmr.feature.about.AboutViewModel.AboutAppDescription
import net.maxsmr.feature.about.AboutViewModel.AboutAppDescription.DonateInfo.PaymentAddress
import net.maxsmr.feature.about.BaseAboutFragment
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.justupdownloadit.BuildConfig
import net.maxsmr.justupdownloadit.R
import net.maxsmr.justupdownloadit.mobileBuildType
import net.maxsmr.justupdownloadit.ui.delegate.MainRateAppFragmentDelegate
import net.maxsmr.mobile_services.IMobileServicesAvailability
import net.maxsmr.justupdownloadit.ui.MainAboutViewModel
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class MainAboutFragment : BaseAboutFragment<MainAboutViewModel>() {

    override val description by lazy {
        AboutAppDescription(
            R.mipmap.ic_launcher,
            Size(286, 286),
            getString(R.string.app_name),
            versionName,
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

    override val viewModel: MainAboutViewModel by viewModels()

    override val rateDelegate by lazy {
        MainRateAppFragmentDelegate(
            this,
            viewModel,
            null,
            mobileBuildType,
            cacheRepo
        )
    }

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    @Inject
    lateinit var cacheRepo: CacheDataStoreRepository

    @Inject
    @Named(DI_NAME_VERSION_NAME)
    lateinit var versionName: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: MainAboutViewModel) {
        super.onViewCreated(view, savedInstanceState, viewModel)
        if (viewModel.isForRate) {
            rateDelegate.onRateAppSelected()
        }
    }

    override fun createFragmentDelegates(): List<IFragmentDelegate> {
        return listOf(rateDelegate)
    }
}