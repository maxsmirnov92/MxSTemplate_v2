package net.maxsmr.core.ui.content.pick.chooser

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.commonutils.getAppSettingsIntent
import net.maxsmr.commonutils.live.event.VmEvent
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.android.content.pick.ContentPickerViewModel
import net.maxsmr.core.android.content.pick.IntentWithPermissions
import net.maxsmr.core.android.content.pick.concrete.ConcretePickerParams
import net.maxsmr.core.android.permissions.getPermissionName
import net.maxsmr.core.ui.R
import net.maxsmr.core.ui.content.pick.chooser.adapter.AppIntentChooserAdapter
import net.maxsmr.core.ui.content.pick.chooser.adapter.IntentChooserAdapterData
import net.maxsmr.core.ui.databinding.LayoutIntentChooserBinding
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

/**
 * BottomSheetDialog для выбора конкретного приложения для взятия контента.
 *
 * Также предоставляет возможность перехода в настройки приложения для предоставления необходимых
 * некоторым приложениям из списка разрешений, которые ранее пользователь отклонил с опцией "Больше не спрашивать"
 */
@AndroidEntryPoint
internal class AppIntentChooserDialog : BottomSheetDialogFragment() {

    private val binding by viewBinding(LayoutIntentChooserBinding::bind)

    private val viewModel: ContentPickerViewModel by lazy {
        //Vm шарится между ContentPicker и AppIntentChooser
        ViewModelProvider(requireActivity())[ContentPickerViewModel::class.java]
    }

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        SettingsResultCallback()
    )

    @Inject
    lateinit var permissionsHelper: PermissionsHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_intent_chooser, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        init()
    }

    private fun init() = binding.run {
        val data = arguments?.getParcelable(EXTRA_DATA) as? AppIntentChooserData ?: return@run
        val adapterData = data.intents.toAdapterData()

        val screenWidth = resources.displayMetrics.widthPixels
        val spanCount = screenWidth / resources.getDimensionPixelSize(R.dimen.app_chooser_item_width_min)
        val itemWidth = screenWidth / spanCount

        val adapter = AppIntentChooserAdapter(
            adapterData,
            itemWidth,
            { params, intent ->
                viewModel.appChoices.value =
                    VmEvent(ContentPickerViewModel.AppChoice(data.requestCode, params, intent))
                dismiss()
            },
            {
                settingsLauncher.launch(getAppSettingsIntent(requireContext()))
            }
        )

        rvIntents.layoutManager =
            GridLayoutManager(requireContext(), spanCount, GridLayoutManager.VERTICAL, false).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int = when (adapter.data[position]) {
                        is IntentChooserAdapterData.App -> 1
                        else -> spanCount
                    }
                }
            }
        rvIntents.adapter = adapter
        tvTitle.text = data.title.get(requireContext())
    }

    private fun Map<ConcretePickerParams, IntentWithPermissions>.toAdapterData(): List<IntentChooserAdapterData> {
        val result = mutableListOf<IntentChooserAdapterData>()
        val pm = requireContext().packageManager
        val deniedNotAskAgain = mutableSetOf<String>()
        this.forEach { (params, intentWithPermissions) ->
            val denied = permissionsHelper.filterDeniedNotAskAgain(
                requireContext(),
                intentWithPermissions.permissions.toList()
            )
            if (denied.isEmpty()) {
                pm.queryIntentActivities(intentWithPermissions.intent, 0).forEach {
                    val packageName = it.activityInfo.packageName
                    result.add(
                        IntentChooserAdapterData.App(
                            params = params,
                            intentWithPermissions = IntentWithPermissions(
                                Intent(intentWithPermissions.intent).apply { setPackage(packageName) },
                                intentWithPermissions.permissions
                            ),
                            icon = it.loadIcon(pm),
                            label = it.loadLabel(pm),
                        )
                    )
                }
            } else {
                deniedNotAskAgain.addAll(denied)
            }
        }
        if (deniedNotAskAgain.isNotEmpty()) {
            result.add(IntentChooserAdapterData.Permissions(deniedNotAskAgain.map {
                requireContext().getPermissionName(it)
            }))
        }
        return result
    }

    override fun onStart() {
        super.onStart()
        setHeight()
    }

    private fun setHeight() {
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        view?.doOnLayout {
            val behavior = BottomSheetBehavior.from(bottomSheet)
            val height = binding.rvIntents.height + binding.tvTitle.height

            behavior.peekHeight = height
            bottomSheet.layoutParams.height = height
            it.requestLayout()
        }
    }


    private inner class SettingsResultCallback : ActivityResultCallback<ActivityResult> {

        override fun onActivityResult(result: ActivityResult) {
            init()
            setHeight()
        }
    }

    companion object {

        private const val EXTRA_DATA = "EXTRA_DATA"

        fun show(src: Fragment, data: AppIntentChooserData): AppIntentChooserDialog {
            return AppIntentChooserDialog().apply {
                arguments = bundleOf(EXTRA_DATA to data)
                show(src.childFragmentManager, null)
            }
        }
    }
}