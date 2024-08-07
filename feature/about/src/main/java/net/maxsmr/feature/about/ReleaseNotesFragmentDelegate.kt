package net.maxsmr.feature.about

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

class ReleaseNotesFragmentDelegate @JvmOverloads constructor(
    override val fragment: BaseVmFragment<*>,
    override val viewModel: BaseViewModel,
    private val versionCode: Int,
    private val versionName: String,
    private val noteAssetsFolderNames: Map<String, String>,
    private val repo: CacheDataStoreRepository,
    private val defaultLocale: String = "en",
    private val shouldShowAll: Boolean = true,
) : IFragmentDelegate {

    init {
        check(versionCode >= 1) {
            "Incorrect versionCode: $versionCode"
        }
    }

    private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger("ReleaseNotesFragmentDelegate")

    override fun onViewCreated(delegate: AlertFragmentDelegate<*>) {
        val scope = viewModel.viewModelScope

        scope.launch(Dispatchers.IO) {
            val seenCodes = repo.getSeenReleaseNotesVersionCodes()
            logger.d("This versionCode: $versionCode, last seen release notes version codes: $seenCodes")

            val currentLocale = Locale.getDefault()

            fun String.equalsCurrentLocale(): Boolean {
                return currentLocale.toString().split("_")[0].equals(this, true)
            }

            val assets = fragment.requireContext().assets

            /**
             * @return номер версии + название соответствующего ассета с заметками в [folderName]
             */
            fun getReleaseNotesAt(folderName: String): Map<Int, String> {
                return assets.list(folderName)?.mapNotNull {
                    val versionCode =
                        it.removeExtension().split("_").lastOrNull()?.toIntOrNull() ?: run {
                            logger.e("Incorrect file \"$it\" in folder \"$folderName\"")
                            return@mapNotNull null
                        }
                    Pair(versionCode, it)
                }?.filter {
                    if (shouldShowAll) {
                        it.first <= versionCode
                    } else {
                        it.first == versionCode
                    } && !seenCodes.contains(it.first)
                }?.toMap().orEmpty()
            }

            val folderName = noteAssetsFolderNames.entries.find { it.key.equalsCurrentLocale() }?.value
            val releaseNotesForCurrentLocale = if (!folderName.isNullOrEmpty()) {
                getReleaseNotesAt(folderName)
            } else {
                logger.w("Asset folder name for current locale is not specified!")
                emptyMap()
            }

            val defaultFolderName: String?
            val releaseNotesForDefaultLocale = if (!defaultLocale.equalsCurrentLocale()) {
                // смотрим по дефолтной, если она отлична от текущей
                defaultFolderName = noteAssetsFolderNames[defaultLocale]
                if (defaultFolderName != null) {
                    // и такой подкаталог есть
                    getReleaseNotesAt(defaultFolderName)
                } else {
                    logger.w("Asset folder name for default locale (\"$defaultLocale\") is not specified!")
                    emptyMap()
                }
            } else {
                defaultFolderName = folderName
                releaseNotesForCurrentLocale
            }

            val resultReleaseNotes = mutableMapOf<Int, Pair<String, String>>()
            val allCodes = releaseNotesForCurrentLocale.keys.plus(releaseNotesForDefaultLocale.keys).toSortedSet()
            allCodes.forEach {
                // собираем упорядоченную мапу с кодами и именами каталога + ассета
                val name = releaseNotesForCurrentLocale[it]
                if (!name.isNullOrEmpty() && !folderName.isNullOrEmpty()) {
                    resultReleaseNotes[it] = Pair(folderName, name)
                } else if (!defaultFolderName.isNullOrEmpty()) {
                    logger.w("No asset for versionCode $it found in folder for current locale (\"$folderName\"), get for default locale")
                    val defaultName = releaseNotesForDefaultLocale[it]
                    if (!defaultName.isNullOrEmpty()) {
                        resultReleaseNotes[it] = Pair(defaultFolderName, defaultName)
                    } else {
                        logger.w("No asset for versionCode $it found in folder for default locale (\"$defaultFolderName\")")
                    }
                } else {
                    logger.w("No asset for versionCode $it found in folder for current locale (\"$folderName\")")
                }
            }

            val iterator = resultReleaseNotes.entries.iterator()

            suspend fun showNextDialog() {

                if (iterator.hasNext()) {
                    val entry = iterator.next()
                    val code = entry.key
                    val message = assets.readStringFromAsset(entry.value.first + File.separator + entry.value.second)

                    if (!message.isNullOrEmpty()) {
                        withContext(Dispatchers.Main.immediate) {
                            viewModel.showCustomDialog(DIALOG_TAG_RELEASE_NOTES) {
                                setTitle(
                                    if (code == versionCode) {
                                        if (versionName.isNotEmpty()) {
                                            TextMessage(
                                                R.string.about_dialog_release_notes_this_title_format,
                                                versionName
                                            )
                                        } else {
                                            TextMessage(R.string.about_dialog_release_notes_this_title)
                                        }
                                    } else {
                                        TextMessage(R.string.about_dialog_release_notes_previous_title)
                                    }
                                )
                                setMessage(message)
                                setAnswers(
                                    Alert.Answer(
                                        if (iterator.hasNext()) {
                                            R.string.about_dialog_release_notes_next_button
                                        } else {
                                            R.string.about_dialog_release_notes_last_button
                                        }
                                    ).onSelect {
                                        scope.launch(Dispatchers.IO) {
                                            repo.setSeenReleaseNotesVersionCode(code)
                                            showNextDialog()
                                        }
                                    }
                                    // не добавляем ответ с тэгом ANSWER_TAG_CLOSE
                                )
                            }
                        }
                    } else {
                        logger.w("No release notes content for versionCode $code")
                    }
                }
            }

            showNextDialog()
        }

        delegate.bindAlertDialog(DIALOG_TAG_RELEASE_NOTES) {
            it.asCommonWrapBottomSheetDialog(fragment.requireContext(), false)
        }
    }

    companion object {

        private const val DIALOG_TAG_RELEASE_NOTES = "release_notes"
    }
}