package net.maxsmr.core.database.model.download

import android.net.Uri
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import net.maxsmr.commonutils.text.appendExtension
import net.maxsmr.core.domain.entities.feature.download.HashInfo
import java.io.Serializable

/**
 * Информация о загрузке
 * @param name название загружаемого ресурса, БЕЗ расширения
 * @param mimeType тип загружаемого ресурса, из ответа
 * @param extension расширение, зависит от [mimeType] на момент подстановки
 * @param status статус загрузки
 */
@Entity(tableName = "DownloadInfo")
data class DownloadInfo(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    val name: String,
    val mimeType: String,
    val extension: String,
    val status: Status,
) : Serializable {

    @Ignore
    val nameWithExt = appendExtension(name, extension, false)

    @Ignore
    val localUri = status.localUri

    @Ignore
    val isLoading = status is Status.Loading

    @Ignore
    val isSuccess = status is Status.Success

    @Ignore
    val isError = status is Status.Error

    @Ignore
    val resourceStatus = when {
        isLoading -> net.maxsmr.commonutils.states.Status.LOADING
        isSuccess -> net.maxsmr.commonutils.states.Status.SUCCESS
        else -> net.maxsmr.commonutils.states.Status.ERROR
    }

    @Ignore
    val statusAsSuccess: Status.Success? = status as? Status.Success

    @Ignore
    val statusAsError: Status.Error? = status as? Status.Error

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DownloadInfo) return false

        if (id != other.id) return false
        if (name != other.name) return false
        if (mimeType != other.mimeType) return false
        return extension == other.extension
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + extension.hashCode()
        return result
    }

    override fun toString(): String {
        return "DownloadInfo(id=$id, name='$name', mimeType='$mimeType', extension='$extension', status=$status, nameWithExt='$nameWithExt')"
    }

    sealed class Status : Serializable {

        protected abstract val uriString: String?

        open val localUri
            get() = uriString?.let { Uri.parse(it) }


        data object Loading : Status() {

            override val uriString: String? = null
        }

        /**
         * @param uriString локальная uri в случае возникновения ошибки при записи скачанного файла,
         * либо null в случае ошибки скачивания
         */
        data class Error(
            override val uriString: String? = null,
            // нельзя сделать @kotlinx.serialization.Serializable из-за этого Exception
            val reason: Exception? = null,
        ) : Status()

        /**
         * @param uriString урла успешно загруженного файла
         * @param initialHashInfo посчитанная контрольная сумма на момент успешной загрузки файла,
         * если алгоритм был указан в парамсах
         */
        data class Success(
            override val uriString: String,
            val initialHashInfo: HashInfo?,
        ) : Status() {

            override val localUri: Uri
                get() = super.localUri as Uri
        }
    }
}