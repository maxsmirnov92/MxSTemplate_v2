package net.maxsmr.mxstemplate.ui.fragment

import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.di.DI_NAME_VERSION_CODE
import net.maxsmr.core.di.DI_NAME_VERSION_NAME
import net.maxsmr.core.ui.components.IComponentDelegate
import net.maxsmr.core.ui.location.LocationViewModel
import net.maxsmr.core.ui.view.alert.delegate.FragmentViewAlertDelegate
import net.maxsmr.feature.about.ReleaseNotesComponentDelegate
import net.maxsmr.feature.about.alert.view.ReleaseNotesFragmentAlertDelegate
import net.maxsmr.feature.address_sorter.ui.AddressSorterFragmentAlertDelegate
import net.maxsmr.feature.address_sorter.ui.AddressSorterViewModel
import net.maxsmr.feature.address_sorter.ui.BaseAddressSorterFragment
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.mxstemplate.BuildConfig
import net.maxsmr.mxstemplate.RELEASE_NOTES_ASSETS_FOLDER_NAME_EN
import net.maxsmr.mxstemplate.RELEASE_NOTES_ASSETS_FOLDER_NAME_RU
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class MainAddressSorterFragment : BaseAddressSorterFragment() {

    private val releaseNotesDelegate by lazy {
        ReleaseNotesComponentDelegate(
            requireContext(),
            viewModel,
            versionCode,
            versionName,
            mapOf(
                "en" to RELEASE_NOTES_ASSETS_FOLDER_NAME_EN,
                "ru" to RELEASE_NOTES_ASSETS_FOLDER_NAME_RU
            ),
            cacheRepo,
        )
    }

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    @Inject
    override lateinit var locationFactory: LocationViewModel.Factory

    @Inject
    override lateinit var factory: AddressSorterViewModel.Factory

    override val routingKeyUrl: String by lazy {
        BuildConfig.URL_DEMO_KEY_DOUBLE_GIS_ROUTING
    }

    @Inject
    lateinit var cacheRepo: CacheDataStoreRepository

    @Inject
    @Named(DI_NAME_VERSION_CODE)
    @JvmField
    var versionCode: Int = 0

    @Inject
    @Named(DI_NAME_VERSION_NAME)
    lateinit var versionName: String

    override fun createAlertDelegate() = FragmentViewAlertDelegate(
        this,
        viewModel,
        AddressSorterFragmentAlertDelegate(this, viewModel),
        ReleaseNotesFragmentAlertDelegate(this, viewModel)
    )

    override fun createFragmentDelegates(): List<IComponentDelegate<*>> {
        return listOf(releaseNotesDelegate)
    }
}