package net.maxsmr.core.android.base.actions

import android.content.Intent
import androidx.fragment.app.Fragment

class IntentNavigationAction(
    val info: IntentNavigationInfo,
) : BaseViewModelAction<Fragment>() {

    override fun doAction(actor: Fragment) {
        super.doAction(actor)
        info.requestCode?.let {
            actor.startActivityForResult(info.intent, it)
        } ?: actor.startActivity(info.intent)
    }

    data class IntentNavigationInfo(
        val intent: Intent,
        val requestCode: Int? = null,
    )
}