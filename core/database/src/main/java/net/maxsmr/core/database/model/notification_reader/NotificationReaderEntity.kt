package net.maxsmr.core.database.model.notification_reader

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import java.lang.Exception

@Entity(tableName = "NotificationData")
data class NotificationReaderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contentText: String,
    val packageName: String,
    val timestamp: Long,
    val status: Status
): Serializable {

    sealed interface Status: Serializable

    sealed class BaseTimeStatus : Status {
        abstract val timestamp: Long
    }

    // время добавления нотификации в базу
    // почти не будет отличаться от времени показа
    data object New : Status {

        private fun readResolve(): Any = New
    }

    data class Loading(override val timestamp: Long): BaseTimeStatus() {

        override fun toString(): String {
            return "Loading(timestamp=$timestamp)"
        }
    }

    data class Cancelled(override val timestamp: Long): BaseTimeStatus() {

        override fun toString(): String {
            return "Cancelled(timestamp=$timestamp)"
        }
    }

    data class Success(override val timestamp: Long): BaseTimeStatus() {

        override fun toString(): String {
            return "Success(timestamp=$timestamp)"
        }
    }

    class Failed(
        override val timestamp: Long,
        val exception: Exception
    ): BaseTimeStatus() {

        override fun toString(): String {
            return "Failed(timestamp=$timestamp, exception=$exception)"
        }
    }
}