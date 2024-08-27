package net.maxsmr.core.network.retrofit.converters

import net.maxsmr.core.network.NO_ERROR_API

/**
 * Интерфейс с базовыми полями ответа, к которому прибавляются
 * остальные поля в наследуемых классах
 */
interface BaseResponse {

    val errorCode: Int

    val errorMessage: String

    val isOk get() = errorCode == NO_ERROR_API
}