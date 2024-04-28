package net.maxsmr.core.android.base.actions

import androidx.annotation.CallSuper

/**
 * **N.B.** предназначение этого класса потерялось, оставлено в целях экономии времени,
 * что-бы не переписывать уже существующий код.
 * Базовый тип для взаимодействия VM -> View
 */
abstract class BaseViewModelAction<Actor> {

    @CallSuper
    open fun doAction(actor: Actor) {
    }
}