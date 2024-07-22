package net.maxsmr.mobile_services.update

interface InAppUpdateChecker {

    fun doCheck()

    fun dispose()
}