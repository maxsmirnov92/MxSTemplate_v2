package net.maxsmr.core.ui.view.alert.delegate

import androidx.lifecycle.LifecycleOwner
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.ui.alert.delegate.BaseViewAlertDelegate
import net.maxsmr.core.ui.components.fragments.BaseVmFragment

abstract class BaseFragmentViewAlertDelegate<VM : BaseViewModel>: BaseViewAlertDelegate<VM>() {

    protected abstract val fragment: BaseVmFragment<*>

    final override val lifecycleOwner: LifecycleOwner by lazy { fragment }
}