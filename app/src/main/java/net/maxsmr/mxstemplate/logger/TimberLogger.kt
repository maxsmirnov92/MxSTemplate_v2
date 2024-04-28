package net.maxsmr.mxstemplate.logger

import net.maxsmr.commonutils.logger.BaseLogger
import timber.log.Timber

class TimberLogger(tag: String): BaseLogger(tag) {

    override fun d(message: String) {
        Timber.tag(tag)
        Timber.d(message)
    }

    override fun d(message: String, exception: Throwable?) {
        Timber.tag(tag)
        Timber.d(exception, message)
    }

    override fun d(exception: Throwable) {
        Timber.tag(tag)
        Timber.d(exception)
    }

    override fun e(message: String) {
        Timber.tag(tag)
        Timber.e(message)
    }

    override fun e(message: String, exception: Throwable?) {
        Timber.tag(tag)
        Timber.e(exception, message)
    }

    override fun e(exception: Throwable) {
        Timber.tag(tag)
        Timber.d(exception)
    }

    override fun i(message: String) {
        Timber.tag(tag)
        Timber.i(message)
    }

    override fun i(message: String, exception: Throwable?) {
        Timber.tag(tag)
        Timber.i(exception, message)
    }

    override fun i(exception: Throwable) {
        Timber.tag(tag)
        Timber.i(exception)
    }

    override fun v(message: String) {
        Timber.tag(tag)
        Timber.v(message)
    }

    override fun v(message: String, exception: Throwable?) {
        Timber.tag(tag)
        Timber.v(exception, message)
    }

    override fun v(exception: Throwable) {
        Timber.tag(tag)
        Timber.v(exception)
    }

    override fun w(message: String) {
        Timber.tag(tag)
        Timber.w(message)
    }

    override fun w(message: String, exception: Throwable?) {
        Timber.tag(tag)
        Timber.w(exception, message)
    }

    override fun w(exception: Throwable) {
        Timber.tag(tag)
        Timber.w(exception)
    }

    override fun wtf(message: String) {
        Timber.tag(tag)
        Timber.wtf(message)
    }

    override fun wtf(message: String, exception: Throwable?) {
        Timber.tag(tag)
        Timber.wtf(exception, message)
    }

    override fun wtf(exception: Throwable) {
        Timber.tag(tag)
        Timber.wtf(exception)
    }
}