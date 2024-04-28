package net.maxsmr.core.database.model

import androidx.room.TypeConverter

class PrimitiveTypeConverter {

    @TypeConverter
    fun intArrayToString(array: IntArray?): String? {
        if (array == null || array.isEmpty()) return null
        val sb = StringBuilder()
        for (i in 0 until array.size - 1) {
            sb.append(array[i]).append(",")
        }
        sb.append(array[array.size - 1])
        return sb.toString()
    }

    @TypeConverter
    fun stringToIntArray(value: String?): IntArray {
        val split = value?.let {  value.split(",").toTypedArray() } ?: return intArrayOf()
        val result = IntArray(split.size)
        for (i in split.indices) {
            split[i].toIntOrNull()?.let {
                result[i] = it
            }
        }
        return result
    }

    @TypeConverter
    fun longArrayToString(array: LongArray?): String? {
        if (array == null || array.isEmpty()) return null
        val sb = StringBuilder()
        for (i in 0 until array.size - 1) {
            sb.append(array[i]).append(",")
        }
        sb.append(array[array.size - 1])
        return sb.toString()
    }

    @TypeConverter
    fun stringToLongArray(value: String?): LongArray {
        val split = value?.let {  value.split(",").toTypedArray() } ?: return longArrayOf()
        val result = LongArray(split.size)
        for (i in split.indices) {
            split[i].toLongOrNull()?.let {
                result[i] = it
            }
        }
        return result
    }

    @TypeConverter
    fun intListToString(list: List<Int>) = stringListToString(list.map(Int::toString))

    @TypeConverter
    fun stringToIntList(value: String?): List<Int> {
        with(value) {
            if (isNullOrBlank() || isEmpty()) return emptyList()
            return split(separator).mapNotNull { it.toIntOrNull() }
        }
    }

    @TypeConverter
    fun longListToString(list: List<Long>) = stringListToString(list.map(Long::toString))

    @TypeConverter
    fun stringToLongList(value: String?): List<Long> {
        with(value) {
            if (isNullOrBlank() || isEmpty()) return emptyList()
            return split(separator).mapNotNull { it.toLongOrNull() }
        }
    }

    @TypeConverter
    fun stringListToString(list: List<String>): String = list.joinToString(separator)

    @TypeConverter
    fun stringToStringList(value: String?): List<String> = value?.split(separator).orEmpty()

    companion object {

        private const val separator = "^"
    }
}