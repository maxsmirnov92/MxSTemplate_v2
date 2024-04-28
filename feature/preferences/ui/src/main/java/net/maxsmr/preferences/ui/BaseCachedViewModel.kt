package net.maxsmr.preferences.ui

import android.Manifest
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.isAtLeastTiramisu
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.preferences.repository.CacheDataStoreRepository

abstract class BaseCachedViewModel(
    private val cacheRepo: CacheDataStoreRepository,
    state: SavedStateHandle,
) : BaseViewModel(state) {

    private val postNotificationAskedLiveData: LiveData<Boolean> = cacheRepo.postNotificationAsked.asLiveData()

    /**
     * после получения разрешения или отказа пользователя получать уведомления - не показывать этот запрос снова
     */
    private fun onPostNotificationPermissionAsked() {
        viewModelScope.launch {
            cacheRepo.setPostNotificationAsked()
        }
    }

    fun observePostNotificationPermissionAsked(fragment: BaseVmFragment<*>, requestCode: Int) {
        if (isAtLeastTiramisu()) {
            // на запомненный флаг обращаем внимание только на главном экране,
            // по месту спрашиваем POST_NOTIFICATIONS, если нужно
            postNotificationAskedLiveData.observe {asked ->
                if (!asked) {
                    fragment.doOnPermissionsResult(requestCode, setOf(Manifest.permission.POST_NOTIFICATIONS),
                        onDenied =  {
                            onPostNotificationPermissionAsked()
                        }
                    ) {
                        onPostNotificationPermissionAsked()
                    }
                }
            }
        }
    }
}