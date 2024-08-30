package net.maxsmr.core.ui.location

import android.Manifest
import android.content.DialogInterface
import android.location.Location
import android.os.HandlerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.getLocationSettingsIntent
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.event.VmEvent
import net.maxsmr.commonutils.live.postValueIfNew
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.coroutines.asDispatcher
import net.maxsmr.core.android.coroutines.collectEventsWithOwner
import net.maxsmr.core.android.location.LocationCallback
import net.maxsmr.core.android.location.receiver.ILocationReceiver
import net.maxsmr.core.android.location.receiver.LocationParams
import net.maxsmr.core.ui.R
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.alert.representation.asOkDialog
import net.maxsmr.core.ui.alert.representation.asYesNoDialog
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import net.maxsmr.core.ui.components.fragments.BaseVmFragment

class LocationViewModel @AssistedInject constructor(
    @Assisted state: SavedStateHandle,
    @Assisted private val mockLocationReceiver: ILocationReceiver?,
    private val locationReceiver: ILocationReceiver,
) : BaseHandleableViewModel(state), LocationCallback {

    private val _currentLocation: MutableLiveData<Location?> = MutableLiveData()
    val currentLocation: LiveData<Location?> = _currentLocation

    private val _navigateToLocationSettings = MutableStateFlow<VmEvent<Unit>?>(null)

    val navigateToLocationSettings = _navigateToLocationSettings.asStateFlow()

    private val locationThread: HandlerThread = HandlerThread("LocationHandlerThread")

    private val locationDispatcher: CoroutineDispatcher by lazy {
        locationThread.asDispatcher()
    }

    var lastGpsDeniedState: GpsDeniedState? = null
        private set

    override fun onLocationChanged(location: Location) {
        _currentLocation.postValue(location)
    }

    override fun onLocationAvailabilityChanged(isAvailable: Boolean) {
        if (!isAvailable) {
            _currentLocation.postValueIfNew(null)
        }
    }

    override fun onGpsNotAvailable() {
        _currentLocation.postValueIfNew(null)
        showOkDialog(DIALOG_TAG_GPS_NOT_AVAILABLE, R.string.dialog_gps_not_available_message)
    }

    override fun onGpsProviderNotEnabled() {
        _currentLocation.postValueIfNew(null)
        showYesNoDialog(
            DIALOG_TAG_GPS_NOT_ENABLED,
            TextMessage(R.string.dialog_gps_enable_message),
            TextMessage(R.string.dialog_gps_enable_title),
            R.string.dialog_gps_enable_answer_settings,
            android.R.string.cancel,
            onSelect = {
                if (it == DialogInterface.BUTTON_POSITIVE) {
                    navigateToLocationSettings()
                }
            }
        )
    }

    override fun onCleared() {
        locationThread.quit()
        unregisterLocationUpdates()
    }

    /**
     * Вызов для показа алертов о проблемах определения местоположения при необходимости
     */
    override fun handleAlerts(delegate: AlertFragmentDelegate<*>) {
        super.handleAlerts(delegate)
        val context = delegate.context
        with(delegate) {
            bindAlertDialog(DIALOG_TAG_GPS_NOT_AVAILABLE) {
                it.asOkDialog(context, true)
            }
            bindAlertDialog(DIALOG_TAG_GPS_NOT_ENABLED) { alert ->
                alert.asYesNoDialog(context, false)
            }
        }
    }

    override fun handleEvents(fragment: BaseVmFragment<*>) {
        super.handleEvents(fragment)
        navigateToLocationSettings.collectEventsWithOwner(fragment.viewLifecycleOwner) {
            fragment.startActivity(getLocationSettingsIntent())
        }
    }

    fun getLastKnownLocation(isGpsOnly: Boolean = false): Location? {
        val location = (mockLocationReceiver ?: locationReceiver).lastKnownPosition
        if (location == null) {
            checkLocationEnabled(baseApplicationContext, isGpsOnly)
        }
        return location
    }

    fun registerLocationUpdates(
        fragment: BaseVmFragment<*>,
        isGpsOnly: Boolean,
        requireFineLocation: Boolean,
    ) {
        if (!hasGpsPermissions(fragment, requireFineLocation)) return
        if (!checkLocationEnabled(fragment.requireContext(), isGpsOnly)) return

        viewModelScope.launch(locationDispatcher) {
            // запрос геолокации на отдельном треде со своим looper
            (mockLocationReceiver ?: locationReceiver).registerLocationUpdates(
                this@LocationViewModel,
                LocationParams(
                    priority = if (isGpsOnly) {
                        LocationParams.Priority.HIGH
                    } else {
                        LocationParams.Priority.BALANCED
                    }
                ),
                locationThread.looper
            )
        }
    }

    fun unregisterLocationUpdates() {
        (mockLocationReceiver ?: locationReceiver).unregisterLocationUpdates()
    }

    private fun navigateToLocationSettings() {
        _navigateToLocationSettings.tryEmit(VmEvent(Unit))
    }

    fun hasGpsPermissions(fragment: BaseVmFragment<*>, requireFineLocation: Boolean): Boolean {
        val perms: List<String> =
            if (requireFineLocation || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
                listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            } else {
                listOf(Manifest.permission.ACCESS_COARSE_LOCATION)
            }

        return fragment.permissionsHelper.hasPermissions(fragment.requireContext(), perms)
    }

    @JvmOverloads
    fun registerLocationUpdatesOnGpsCheck(
        fragment: BaseVmFragment<*>,
        requestCode: Int,
        isGpsOnly: Boolean,
        requireFineLocation: Boolean,
        checkOnly: Boolean = false,
        callbacks: GpsCheckCallbacks? = null,
    ) {
        doOnGpsCheck(
            fragment,
            requestCode,
            isGpsOnly,
            requireFineLocation,
            checkOnly,
            object : GpsCheckCallbacks {

                override fun onGpsDisabledOrNotAvailable() {
                    callbacks?.onGpsDisabledOrNotAvailable()
                }

                override fun onBeforeGpsCheck() {
                    callbacks?.onBeforeGpsCheck()
                }

                override fun onPermissionsDenied() {
                    callbacks?.onPermissionsDenied()
                }

                override fun onPermissionsGranted() {
                    registerLocationUpdates(fragment, isGpsOnly, requireFineLocation)
                    callbacks?.onPermissionsGranted()
                }
            }
        )
    }

    @JvmOverloads
    fun doOnGpsCheck(
        fragment: BaseVmFragment<*>,
        requestCode: Int,
        isGpsOnly: Boolean,
        requireFineLocation: Boolean,
        checkOnly: Boolean = false,
        callbacks: GpsCheckCallbacks,
    ) {
        val perms: List<String> =
            if (requireFineLocation || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
                listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            } else {
                listOf(Manifest.permission.ACCESS_COARSE_LOCATION)
            }

        logger.d("doOnGpsCheck")
        fragment.doOnPermissionsResult(requestCode, perms, onDenied = {
            lastGpsDeniedState = GpsDeniedState.PERMISSIONS
            callbacks.onPermissionsDenied()
        }) {
            callbacks.onBeforeGpsCheck()
            if (checkLocationEnabled(fragment.requireContext(), isGpsOnly, checkOnly)) {
                lastGpsDeniedState = null
                callbacks.onPermissionsGranted()
            } else {
                lastGpsDeniedState = GpsDeniedState.GPS
                callbacks.onGpsDisabledOrNotAvailable()
            }
        }
    }

    interface GpsCheckCallbacks {

        fun onGpsDisabledOrNotAvailable() {}

        fun onBeforeGpsCheck() {}

        fun onPermissionsDenied() {}

        fun onPermissionsGranted() {}
    }

    enum class GpsDeniedState {
        GPS,
        PERMISSIONS
    }

    @AssistedFactory
    interface Factory {

        /**
         * @param mockLocationReceiver если задан, использется вместо [LocationViewModel.locationReceiver]
         */
        fun create(
            state: SavedStateHandle,
            mockLocationReceiver: ILocationReceiver?,
        ): LocationViewModel
    }

    companion object {

        private const val DIALOG_TAG_GPS_NOT_AVAILABLE = "DIALOG_TAG_GPS_NOT_AVAILABLE"
        private const val DIALOG_TAG_GPS_NOT_ENABLED = "DIALOG_TAG_GPS_NOT_ENABLED"
    }
}