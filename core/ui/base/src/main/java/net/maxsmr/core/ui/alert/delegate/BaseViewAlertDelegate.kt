package net.maxsmr.core.ui.alert.delegate

import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.queue.AlertQueue
import net.maxsmr.core.ui.alert.representation.StandardAlertRepresentation

/**
 * Делегат для fragment с функцией отображения алертов
 * с репрезентациями на основе android.app.Dialog и View;
 * 1. Использование "из коробки" при наличии прикреплённой [BaseViewModel]:
 * подразумевается первое использование не ранее onViewCreated!
 * 2. Использование по месту при подстановке [AlertQueue], диалоги с которой он должен обрабатывать;
 */
abstract class BaseViewAlertDelegate<VM : BaseViewModel>: BaseAlertDelegate<VM>() {

    abstract fun handleAlertDialogs()

    open fun handleSnackbarAlerts() {}

    open fun handleToastAlerts() {}

    /**
     * Связывает сообщения с тэгом [tag] из очереди this с конкретным способом отображения,
     * возвращаемым лямбдой [representationFactory].
     */
    fun bindAlertDialog(tag: String, representationFactory: (Alert) -> StandardAlertRepresentation) {
        bindStandardAlert(viewModel.dialogQueue, tag, representationFactory)
    }
}