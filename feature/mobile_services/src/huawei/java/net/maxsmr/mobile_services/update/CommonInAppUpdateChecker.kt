package net.maxsmr.mobile_services.update

import androidx.fragment.app.Fragment
import net.maxsmr.mobile_services.IMobileServicesAvailability

class CommonInAppUpdateChecker(
    private val availability: IMobileServicesAvailability,
    private val fragment: Fragment,
    private val updateRequestCode: Int,
    private val callbacks: InAppUpdateChecker.Callbacks,
    private val immediateUpdatePriority: Int = 4,
): InAppUpdateChecker {

    override fun doCheck() {
        // TODO implementation
    }
}