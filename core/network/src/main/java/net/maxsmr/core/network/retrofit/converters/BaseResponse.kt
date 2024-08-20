package net.maxsmr.core.network.retrofit.converters

import net.maxsmr.core.network.NO_ERROR
import net.maxsmr.core.network.SUCCESS

/**
 * Интерфейс с базовыми полями ответа, к которому прибавляются
 * остальные поля в наследуемых классах
 */
interface BaseResponse {

    val errorCode: Int
    val errorMessage: String

    val isOk get() = errorCode in listOf(SUCCESS, NO_ERROR)
}