package net.maxsmr.feature.compose_sample.ui.presentation.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.maxsmr.core.ui.components.activities.BaseActivity.Companion.REQUEST_CODE_PERMISSION_GPS
import net.maxsmr.core.ui.compose.components.BaseComposeActivity
import net.maxsmr.core.ui.location.LocationViewModel
import net.maxsmr.core.ui.location.LocationViewModel.GpsCheckCallbacks
import net.maxsmr.feature.compose_sample.ui.presentation.main.TextWithCounter

@Composable
fun HomeScreen(
    activity: BaseComposeActivity<*>,
    locationViewModel: LocationViewModel,
    viewModel: HomeViewModel
) {
    val isLocationEnabled = remember { mutableStateOf(false) }
    Column {
        TextWithCounter("Home")
        Spacer(Modifier.height(6.dp))
        if (!isLocationEnabled.value) {
            Button({
                locationViewModel.doOnGpsCheck(
                    activity,
                    REQUEST_CODE_PERMISSION_GPS,
                    isGpsOnly = false,
                    requireFineLocation = true,
                    callbacks = object : GpsCheckCallbacks {
                        override fun onPermissionsGranted() {
                            isLocationEnabled.value = true
                        }
                    }
                )
            }) {
                Text(text = "Ask permission")
            }
        } else {
            Text(text = "Location enabled")
        }
    }
}