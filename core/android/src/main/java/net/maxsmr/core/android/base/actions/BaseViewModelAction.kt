package net.maxsmr.core.android.base.actions

abstract class BaseViewModelAction<Actor> {

    abstract fun doAction(actor: Actor)
}