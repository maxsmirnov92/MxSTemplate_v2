package net.maxsmr.feature.download.ui

import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.android.recyclerview.views.decoration.Divider
import net.maxsmr.android.recyclerview.views.decoration.DividerItemDecoration
import net.maxsmr.commonutils.gui.bindToTextNotNull
import net.maxsmr.commonutils.gui.setTextOrGone
import net.maxsmr.commonutils.live.field.observeFromText
import net.maxsmr.core.android.base.alert.AlertHandler
import net.maxsmr.core.android.base.delegates.AbstractSavedStateViewModelFactory
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.android.content.pick.ContentPicker
import net.maxsmr.core.android.content.pick.PickRequest
import net.maxsmr.core.android.content.pick.concrete.saf.SafPickerParams
import net.maxsmr.core.ui.components.activities.BaseActivity
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.feature.download.data.DownloadsViewModel
import net.maxsmr.feature.download.ui.adapter.HeaderListener
import net.maxsmr.feature.download.ui.adapter.HeadersAdapter
import net.maxsmr.feature.download.ui.databinding.FragmentDownloadsParamsBinding
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

@AndroidEntryPoint
class DownloadsParamsFragment: BaseVmFragment<DownloadsParamsViewModel>(), HeaderListener {

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    @Inject
    lateinit var factory: DownloadsParamsViewModel.Factory

    override val layoutId: Int = R.layout.fragment_downloads_params

    override val viewModel: DownloadsParamsViewModel by viewModels {
        AbstractSavedStateViewModelFactory(this) {
            factory.create(it, downloadsViewModel)
        }
    }

    private val downloadsViewModel: DownloadsViewModel by viewModels()

    private val binding by viewBinding(FragmentDownloadsParamsBinding::bind)

    private val headersAdapter by lazy { HeadersAdapter(this) }

    private val contentPicker: ContentPicker = FragmentContentPickerBuilder()
        .addRequest(
            PickRequest.BuilderDocument(REQUEST_CODE_CHOOSE_REQUEST_BODY_RESOURCE)
                .addSafParams(SafPickerParams.any())
                .needPersistableUriAccess(true)
                .onSuccess {
                    viewModel.onRequestBodyUriSelected(it.uri)
                }
                .onError {
                    viewModel.onPickerResultError(it)
                }
                .build()

        ).build()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
        viewModel: DownloadsParamsViewModel,
        alertHandler: AlertHandler,
    ) {
        binding.etUrl.bindToTextNotNull(viewModel.urlField)
        viewModel.urlField.observeFromText(binding.etUrl, viewLifecycleOwner)
        viewModel.urlField.hintLive.observe {
            binding.tilUrl.hint = it?.get(requireContext())
        }
        viewModel.urlField.errorLive.observe {
            binding.tilUrl.error = it?.get(requireContext())
        }

        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.download_method_names,
            android.R.layout.simple_list_item_1
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMethod.adapter = adapter
        binding.spinnerMethod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.methodField.value = DownloadsParamsViewModel.Method.entries[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        viewModel.methodField.valueLive.observe {
            binding.spinnerMethod.setSelection(it.ordinal)
        }

        viewModel.requestBodyField.valueLive.observe {
            binding.containerSelectRequestBody.isEnabled = it.isEnabled
            binding.ibSelectRequestBody.isEnabled = it.isEnabled
            binding.ibClearRequestBody.isVisible = !it.isEmpty
            binding.tvRequestBodyName.text = it.getName(requireContext()).takeIf { name -> name.isNotEmpty() }
                ?: getString(
                    if (it.isEnabled) {
                        R.string.download_request_body_empty_text
                    } else {
                        R.string.download_request_body_non_required_text
                    })
        }
        viewModel.requestBodyField.errorLive.observe {
            binding.tvRequestBodyError.setTextOrGone(it?.get(requireContext()))
        }

        binding.etFileName.bindToTextNotNull(viewModel.fileNameField)
        viewModel.fileNameField.observeFromText(binding.etFileName, viewLifecycleOwner)
        viewModel.fileNameField.hintLive.observe {
            binding.tilFileName.hint = it?.get(requireContext())
        }

        binding.cbFileNameFix.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onFileNameFixChanged(isChecked)
        }
        viewModel.fileNameFlagsField.valueLive.observe {
            binding.cbFileNameFix.isEnabled = it.isEnabled
            binding.cbFileNameFix.isChecked = it.shouldFix
        }
        viewModel.fileNameField.hintLive.observe {
            binding.tilFileName.hint = it?.get(requireContext())
        }

        binding.etSubDirName.bindToTextNotNull(viewModel.subDirNameField)
        viewModel.subDirNameField.observeFromText(binding.etSubDirName, viewLifecycleOwner)
        viewModel.subDirNameField.hintLive.observe {
            binding.tilSubDirName.hint = it?.get(requireContext())
        }

        binding.rvHeaders.adapter = headersAdapter
        binding.rvHeaders.addItemDecoration(
            DividerItemDecoration.Builder(requireContext())
            .setDivider(Divider.Space(10), DividerItemDecoration.Mode.ALL_EXCEPT_LAST)
            .build())
        viewModel.headerItems.observe {
            headersAdapter.items = it
        }

        binding.ibSelectRequestBody.setOnClickListener {
            contentPicker.pick(REQUEST_CODE_CHOOSE_REQUEST_BODY_RESOURCE, requireContext())
        }
        binding.ibClearRequestBody.setOnClickListener {
            viewModel.onClearRequestBodyUri()
        }

        binding.ibAdd.setOnClickListener {
            viewModel.onAddHeader()
        }

        binding.btStart.setOnClickListener {
            doOnPermissionsResult(
                BaseActivity.REQUEST_CODE_DOWNLOAD_PERMISSION,
                PermissionsHelper.addPostNotificationsByApiVersion(listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            ) {
                viewModel.onDownloadStartClick()
            }
        }
    }

    override fun onHeaderNameChanged(id: Int, value: String) {
        viewModel.onHeaderValueChanged(id, value, true)
    }

    override fun onHeaderValueChanged(id: Int, value: String) {
        viewModel.onHeaderValueChanged(id, value, false)
    }

    override fun onRemoveHeader(id: Int) {
        viewModel.onRemoveHeader(id)
    }

    companion object {

        private const val REQUEST_CODE_CHOOSE_REQUEST_BODY_RESOURCE = 1
    }
}