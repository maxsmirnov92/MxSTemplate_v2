package net.maxsmr.mobile_services;

enum class MobileBuildType(val value: String) {

    COMMON("common"),
    RUSTORE("ruStore");

    companion object {

        fun resolve(value: String) = entries.find { it.value.equals(value, true) }
            ?: throw IllegalArgumentException("Unknown type value: $value")
    }
}