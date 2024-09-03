package net.maxsmr.mxstemplate.ui.fragment

import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.di.DI_NAME_VERSION_CODE
import net.maxsmr.core.di.DI_NAME_VERSION_NAME
import net.maxsmr.core.ui.components.IFragmentDelegate
import net.maxsmr.core.ui.location.LocationViewModel
import net.maxsmr.feature.about.ReleaseNotesFragmentDelegate
import net.maxsmr.feature.address_sorter.ui.AddressSorterViewModel
import net.maxsmr.feature.address_sorter.ui.BaseAddressSorterFragment
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.mxstemplate.RELEASE_NOTES_ASSETS_FOLDER_NAME_EN
import net.maxsmr.mxstemplate.RELEASE_NOTES_ASSETS_FOLDER_NAME_RU
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class MainAddressSorterFragment: BaseAddressSorterFragment() {

    private val releaseNotesDelegate by lazy {
        ReleaseNotesFragmentDelegate(
            this,
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

    @Inject
    lateinit var cacheRepo: CacheDataStoreRepository

    @Inject
    @Named(DI_NAME_VERSION_CODE)
    @JvmField
    var versionCode: Int = 0

    @Inject
    @Named(DI_NAME_VERSION_NAME)
    lateinit var versionName: String

    override fun createFragmentDelegates(): List<IFragmentDelegate> {
        return listOf(releaseNotesDelegate)
    }
}