package net.maxsmr.justupdownloadit.logger

import net.maxsmr.commonutils.logger.BaseLogger
import timber.log.Timber

class TimberLogger(tag: String): BaseLogger(tag) {

    override fun _d(message: String) {
        Timber.tag(tag)
        Timber.d(message)
    }

    override fun _d(message: String, exception: Throwable?) {
        Timber.tag(tag)
        Timber.d(exception, message)
    }

    override fun _d(exception: Throwable) {
        Timber.tag(tag)
        Timber.d(exception)
    }

    override fun _e(message: String) {
        Timber.tag(tag)
        Timber.e(message)
    }

    override fun _e(message: String, exception: Throwable?) {
        Timber.tag(tag)
        Timber.e(exception, message)
    }

    override fun _e(exception: Throwable) {
        Timber.tag(tag)
        Timber.d(exception)
    }

    override fun _i(message: String) {
        Timber.tag(tag)
        Timber.i(message)
    }

    override fun _i(message: String, exception: Throwable?) {
        Timber.tag(tag)
        Timber.i(exception, message)
    }

    override fun _i(exception: Throwable) {
        Timber.tag(tag)
        Timber.i(exception)
    }

    override fun _v(message: String) {
        Timber.tag(tag)
        Timber.v(message)
    }

    override fun _v(message: String, exception: Throwable?) {
        Timber.tag(tag)
        Timber.v(exception, message)
    }

    override fun _v(exception: Throwable) {
        Timber.tag(tag)
        Timber.v(exception)
    }

    override fun _w(message: String) {
        Timber.tag(tag)
        Timber.w(message)
    }

    override fun _w(message: String, exception: Throwable?) {
        Timber.tag(tag)
        Timber.w(exception, message)
    }

    override fun _w(exception: Throwable) {
        Timber.tag(tag)
        Timber.w(exception)
    }

    override fun _wtf(message: String) {
        Timber.tag(tag)
        Timber.wtf(message)
    }

    override fun _wtf(message: String, exception: Throwable?) {
        Timber.tag(tag)
        Timber.wtf(exception, message)
    }

    override fun _wtf(exception: Throwable) {
        Timber.tag(tag)
        Timber.wtf(exception)
    }
}