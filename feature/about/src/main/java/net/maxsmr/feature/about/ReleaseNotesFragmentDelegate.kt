package net.maxsmr.feature.about

import android.content.res.AssetManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.readStringFromAsset
import net.maxsmr.commonutils.text.removeExtension
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.alert.representation.asCommonWrapBottomSheetDialog
import net.maxsmr.core.ui.components.IFragmentDelegate
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import java.io.File
import java.util.Locale
import kotlin.math.log

class ReleaseNotesFragmentDelegate @JvmOverloads constructor(
    override val fragment: BaseVmFragment<*>,
    override val viewModel: BaseViewModel,
    private val versionCode: Int,
    private val versionName: String,
    private val noteAssetsFolderNames: Map<String, String>,
    private val repo: CacheDataStoreRepository,
    private val defaultLocale: String = "en",
) : IFragmentDelegate {

    private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger("ReleaseNotesFragmentDelegate")

    override fun onViewCreated(delegate: AlertFragmentDelegate<*>) {
        val assets = fragment.requireContext().assets
        viewModel.viewModelScope.launch {
            val lastCode: Int? = repo.getLastReleaseNotesVersionCode()
            logger.d("This versionCode: $versionCode, last release notes versionCode: $lastCode")

            if (lastCode == null || lastCode < versionCode) {

                val currentLocale = Locale.getDefault()

                fun String.equalsCurrentLocale(): Boolean {
                    return currentLocale.toString().split("_")[0].equals(this, true)
                }

                fun AssetManager.findAssetName(folderName: String): String? {
                    return list(folderName)?.find {
                        it.removeExtension().split("_").lastOrNull()?.toIntOrNull() == versionCode
                    }
                }

                val folderName = noteAssetsFolderNames.entries.find { it.key.equalsCurrentLocale() }?.value
                val message = if (!folderName.isNullOrEmpty()) {
                    val assetName = assets.findAssetName(folderName)
                    if (!assetName.isNullOrEmpty()) {
                        // подкаталог для текущей локали и имя ассета в нём есть
                        assets.readStringFromAsset(folderName + File.separator + assetName)
                    } else {
                        logger.w("No asset for versionCode $versionCode found in folder for current locale (\"$folderName\")")
                        null
                    }
                } else {
                    null
                }.takeIf { it != null } ?: let {
                    // содержимого ассета по версии для текущей локали нет или каталог не был указан,
                    var defaultMessage: String? = null
                    if (!defaultLocale.equalsCurrentLocale()) {
                        val defaultFolderName = noteAssetsFolderNames[defaultLocale]
                        if (defaultFolderName != null) {
                            // смотрим по дефолтной, если она отлична от текущей
                            val assetName = assets.findAssetName(defaultFolderName)
                            if (!assetName.isNullOrEmpty()) {
                                // подкаталог для дефолтной локали и имя ассета в нём есть
                                defaultMessage =
                                    assets.readStringFromAsset(defaultFolderName + File.separator + assetName)
                            } else {
                                logger.w("No asset for versionCode $versionCode found in folder for default locale (\"$defaultFolderName\")")
                            }
                        } else {
                            logger.w("Asset folder name for defaultLocale (\"$defaultLocale\") is not specified!")
                        }

                    }
                    defaultMessage
                }
                if (!message.isNullOrEmpty()) {
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
                                repo.setLastReleaseNotesVersionCode(versionCode)
                            }
                        }
                            // не добавляем ответ с тэгом ANSWER_TAG_CLOSE
                        )
                    }
                } else {
                    logger.w("No release notes for versionCode $versionCode specified")
                }

            }
        }
        delegate.bindAlertDialog(DIALOG_TAG_RELEASE_NOTES) {
            it.asCommonWrapBottomSheetDialog(fragment.requireContext(), false)
        }
    }

    companion object {

        private const val DIALOG_TAG_RELEASE_NOTES = "release_notes"
    }
}