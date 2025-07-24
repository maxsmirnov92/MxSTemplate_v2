package net.maxsmr.core.ui.location

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.location.Location
import android.os.HandlerThread
import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.event.VmEvent
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.location.LocationCallback
import net.maxsmr.core.android.location.receiver.LocationParams
import net.maxsmr.core.android.location.receiver.LocationReceiver
import net.maxsmr.core.android.permissions.PermissionsRequester
import net.maxsmr.core.ui.R

class LocationViewModel @AssistedInject constructor(
    @Assisted state: SavedStateHandle,
    @Assisted private val mockLocationReceiver: LocationReceiver?,
    private val locationReceiver: LocationReceiver,
    @ApplicationContext private val context: Context,
) : BaseViewModel(state), LocationCallback {

    val currentLocation: StateFlow<Location?> by lazy { _currentLocation.asStateFlow() }

    val navigateToLocationSettingsEvent by lazy { _navigateToLocationSettingsEvent.asStateFlow() }

    private val _currentLocation = MutableStateFlow<Location?>(null)

    private val _navigateToLocationSettingsEvent = MutableStateFlow<VmEvent<Unit>?>(null)

    private val locationThread: HandlerThread = HandlerThread("LocationHandlerThread").apply {
        start()
    }

    var lastLocationDeniedReason: LocationDeniedReason? = null
        private set

    override fun onLocationChanged(location: Location) {
        _currentLocation.value = location
    }

    override fun onLocationAvailabilityChanged(isAvailable: Boolean) {
        if (!isAvailable) {
            _currentLocation.value = null
        }
    }

    override fun onLocationNotSupported() {
        _currentLocation.value = null
        showOkDialog(DIALOG_TAG_LOCATION_NOT_AVAILABLE, R.string.dialog_location_not_available_message)
    }

    override fun onLocationProviderNotEnabled() {
        _currentLocation.value = null
        showYesNoDialog(
            DIALOG_TAG_LOCATION_NOT_ENABLED,
            TextMessage(R.string.dialog_location_enable_message),
            TextMessage(R.string.dialog_location_enable_title),
            R.string.dialog_location_enable_answer_settings,
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

    fun getLastKnownLocation(withGpsOnly: Boolean = false): Location? {
        val location = (mockLocationReceiver ?: locationReceiver).lastKnownPosition
        if (location == null) {
            checkLocationEnabled(context, withGpsOnly)
        }
        return location
    }

    fun unregisterLocationUpdates() {
        (mockLocationReceiver ?: locationReceiver).unregisterLocationUpdates()
    }

    @JvmOverloads
    fun registerLocationUpdates(
        host: PermissionsRequester,
        requestCode: Int,
        requireFineLocation: Boolean,
        callbacks: LocationCheckCallbacks? = null,
        withGpsOnly: Boolean = false,
    ) {
        doOnLocationCheck(
            host,
            requestCode,
            requireFineLocation,
            object : LocationCheckCallbacks {

                override fun onLocationDisabledOrNotAvailable() {
                    callbacks?.onLocationDisabledOrNotAvailable()
                }

                override fun onBeforeLocationCheck() {
                    callbacks?.onBeforeLocationCheck()
                }

                override fun onLocationPermissionsDenied() {
                    callbacks?.onLocationPermissionsDenied()
                }

                override fun onLocationPermissionsGranted() {
                    callbacks?.onLocationPermissionsGranted()
                    registerLocationUpdates(host, withGpsOnly)
                }
            },
            withGpsOnly = withGpsOnly
        )
    }

    @JvmOverloads
    fun doOnLocationCheck(
        host: PermissionsRequester,
        requestCode: Int,
        requireFineLocation: Boolean,
        callbacks: LocationCheckCallbacks,
        withGpsOnly: Boolean = false,
        checkOnly: Boolean = false,
    ) {
        val perms: List<String> =
            if (requireFineLocation || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
                listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            } else {
                listOf(Manifest.permission.ACCESS_COARSE_LOCATION)
            }

        host.doOnPermissionsResult(requestCode, perms, onDenied = {
            lastLocationDeniedReason = LocationDeniedReason.PERMISSIONS
            callbacks.onLocationPermissionsDenied()
        }) {
            callbacks.onBeforeLocationCheck()
            if (checkLocationEnabled(host.requireContext, withGpsOnly, !checkOnly)) {
                lastLocationDeniedReason = null
                callbacks.onLocationPermissionsGranted()
            } else {
                lastLocationDeniedReason = LocationDeniedReason.AVAILABILITY
                callbacks.onLocationDisabledOrNotAvailable()
            }
        }
    }

    private fun navigateToLocationSettings() {
        _navigateToLocationSettingsEvent.tryEmit(VmEvent(Unit))
    }

    private fun registerLocationUpdates(
        host: PermissionsRequester,
        withGpsOnly: Boolean,
    ) {
//        if (!hasGpsPermissions(host, requireFineLocation)) return
        if (!checkLocationEnabled(host.requireContext, withGpsOnly)) return

        (mockLocationReceiver ?: locationReceiver).registerLocationUpdates(
            this@LocationViewModel,
            LocationParams(
                priority = if (withGpsOnly) {
                    LocationParams.Priority.HIGH
                } else {
                    LocationParams.Priority.BALANCED
                }
            ),
            locationThread.looper
        )
    }

    interface LocationCheckCallbacks {

        fun onLocationDisabledOrNotAvailable() {}

        fun onBeforeLocationCheck() {}

        fun onLocationPermissionsDenied() {}

        fun onLocationPermissionsGranted() {}
    }

    enum class LocationDeniedReason {
        AVAILABILITY,
        PERMISSIONS
    }

    @AssistedFactory
    interface Factory {

        /**
         * @param mockLocationReceiver если задан, использется вместо [LocationViewModel.locationReceiver]
         */
        fun create(
            state: SavedStateHandle,
            mockLocationReceiver: LocationReceiver?,
        ): LocationViewModel
    }

    companion object {

        const val DIALOG_TAG_LOCATION_NOT_AVAILABLE = "DIALOG_TAG_LOCATION_NOT_AVAILABLE"
        const val DIALOG_TAG_LOCATION_NOT_ENABLED = "DIALOG_TAG_LOCATION_NOT_ENABLED"
    }
}