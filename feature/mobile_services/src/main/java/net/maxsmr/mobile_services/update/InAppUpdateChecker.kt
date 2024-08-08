package net.maxsmr.mobile_services.update

interface InAppUpdateChecker {

    fun doCheck()

    interface Callbacks {

        fun onUpdateCheckSuccess()

        fun onUpdateDownloadNotStarted(isCancelled: Boolean)

        fun onUpdateDownloadStarted()

        fun onUpdateDownloading(currentBytes: Long, totalBytes: Long) {}

        fun onUpdateDownloaded(completeAction: () -> Unit)

        fun onUpdateFailed()

        fun onUpdateCancelled()

        fun onStartUpdateFlowFailed(throwable: Throwable)
    }
}