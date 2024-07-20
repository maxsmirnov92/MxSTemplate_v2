package net.maxsmr.feature.about

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.readStringsFromAsset
import net.maxsmr.commonutils.text.removeExtension
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.alert.representation.asCommonWrapBottomSheetDialog
import net.maxsmr.core.ui.components.IFragmentDelegate
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import java.io.File

class ReleaseNotesFragmentDelegate(
    private val versionCode: Int,
    private val versionName: String,
    private val noteAssetsFolderName: String,
    private val noteAssetsNames: Set<String>,
    private val repo: CacheDataStoreRepository,
) : IFragmentDelegate {

    override fun onViewCreated(
        fragment: BaseVmFragment<*>,
        viewModel: BaseViewModel,
        delegate: AlertFragmentDelegate<*>,
    ) {
        val context = fragment.requireContext()
        viewModel.viewModelScope.launch {
            val lastCode: Int? = repo.getLastReleaseNoteVersionCode()
            if (lastCode == null || lastCode < versionCode) {
                noteAssetsNames.find {
                    it.removeExtension().split("_").lastOrNull()?.toIntOrNull() == versionCode
                }?.let { assetName ->
                    val message = context.assets.readStringsFromAsset(noteAssetsFolderName + File.separator + assetName)
                        .filter { it.isNotEmpty() }.joinToString(System.lineSeparator())
                    if (message.isNotEmpty()) {
                        viewModel.showCustomDialog(DIALOG_TAG_RELEASE_NOTES) {
                            setTitle(
                                if (versionName.isNotEmpty()) {
                                    TextMessage(R.string.about_dialog_release_notes_title_format, versionName)
                                } else {
                                    TextMessage(R.string.about_dialog_release_notes_title)
                                }
                            )
                            setMessage(message)
                            setAnswers(Alert.Answer(R.string.about_dialog_release_notes_button).onSelect {
                                viewModel.viewModelScope.launch {
                                    repo.setLastReleaseNoteVersionCode(versionCode)
                                }
                            }
                                // не добавляем ответ с тэгом ANSWER_TAG_CLOSE
                            )
                        }
                    }
                }
            }
        }
        delegate.bindAlertDialog(DIALOG_TAG_RELEASE_NOTES) {
            it.asCommonWrapBottomSheetDialog(context, false)
        }
    }

    companion object {

        private const val DIALOG_TAG_RELEASE_NOTES = "release_notes"
    }
}