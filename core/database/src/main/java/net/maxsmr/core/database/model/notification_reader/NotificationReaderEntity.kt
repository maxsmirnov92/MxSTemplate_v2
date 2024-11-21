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
) {

    sealed interface Status: Serializable

    data object New : Status {

        private fun readResolve(): Any = New
    }

    data object Loading: Status {

        private fun readResolve(): Any = Loading
    }

    class Failed(val exception: Exception): Status {

        override fun toString(): String {
            return "Failed(exception=$exception)"
        }
    }
}