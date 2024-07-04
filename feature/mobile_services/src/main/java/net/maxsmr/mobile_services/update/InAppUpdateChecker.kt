package net.maxsmr.mobile_services.update

interface InAppUpdateChecker {

    fun onStartChecking()

    fun onStopChecking()
}