package net.maxsmr.core.domain

inline fun <reified T: Enum<T>> enumValueOfOrDefault(name: String?, default: T): T {
    return name?.let { enumValues<T>().find { it.name.equals(name, ignoreCase = true) } } ?: default
}

inline fun <reified T: Enum<T>> enumValueOfOrNull(name: String?): T? {
    return name?.let { enumValues<T>().find { it.name.equals(name, ignoreCase = true) } }
}

inline fun <reified T> enumValueOfOrDefault(value: Int?, default: T): T  where T : Enum<T>, T : IntId {
    return value?.let { enumValues<T>().find { it.id == value } } ?: default
}

inline fun <reified T> enumValueOfOrNull(value: Int?): T?  where T : Enum<T>, T : IntId {
    return value?.let { enumValues<T>().find { it.id == value } }
}

inline fun <reified T: Enum<T>> enumValueOfOrNull(value: Int?, enumValue: (v: T) -> Int): T? {
    return value?.let { enumValues<T>().find { enumValue(it) == value } }
}

interface IntId {

    val id: Int
}

interface StringId {

    val id: String
}