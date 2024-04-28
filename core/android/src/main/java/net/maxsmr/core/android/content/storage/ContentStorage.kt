package net.maxsmr.core.android.content.storage

import android.content.Context
import android.net.Uri
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.flatMapError
import com.github.kittinunf.result.onSuccess
import java.io.InputStream
import java.io.OutputStream

/**
 * Абстракция хранилища контента (ресурса), доступ к которым осуществляется через прокси типа [T].
 * Предоставляет базовые методы доступа и управления ресурсом
 *
 * @param T прокси для доступа к ресурсу
 */
interface ContentStorage<T> {

    /**
     * Проверка **физического** существования ресурса
     *
     * @param name полное имя файла, с расришением
     * @param path относительный путь к директории файла
     * @return
     * 1. Result.Success(true) - если ресурс с именем [name] существует
     * 1. Result.Success(false) - если не существует
     * 1. Result.Failure - если при проверке возникло исключение
     */
    fun exists(name: String, path: String? = null, context: Context): Result<Boolean, Exception>

    /**
     * **Физически** создает ресурс с именем [name] и возвращает объект для доступа к нему.
     * Если ресурс с именем [name] уже существует, будет перезаписан.
     *
     * @param name полное имя файла, с расришением
     * @param path относительный путь к директории файла
     * @return
     * 1. Result.Success - если ресурс с именем [name] успешно создан
     * 1. Result.Failure - если при создании возникло исключение
     */
    fun create(name: String, path: String? = null, context: Context): Result<T, Exception>

    /**
     * Возвращает объект для доступа к ресурсу с именем [name]. Сам ресурс может не существовать.
     *
     * @param name полное имя файла, с расришением
     * @param path относительный путь к директории файла
     * @return
     * 1. Result.Success - если удалось получить объект для доступа к ресурсу с именем [name]
     * 1. Result.Failure - если возникло исключение
     */
    fun get(name: String, path: String? = null, context: Context): Result<T, Exception>

    /**
     * Возвращает объект для доступа к ресурсу с именем [name]. Если ресурс не существует,
     * **физически** создает его.
     *
     * @param name полное имя файла, с расришением
     * @param path относительный путь к директории файла
     * @return
     * 1. Result.Success - если удалось получить объект для доступа к ресурсу с именем [name], ресурс физически существует
     * 1. Result.Failure - если возникло исключение
     */
    fun getOrCreate(name: String, path: String? = null, context: Context): Result<T, Exception> =
        exists(name, path, context).flatMap { get(name, path, context) }.flatMapError { create(name, path, context) }

    /**
     * Записывает данные [content] в ресурс с именем [name]. Если ресурс физически не существует,
     * будет произведена попытка его создания.
     *
     * @param name полное имя файла, с расришением
     * @param path относительный путь к директории файла
     * @return
     * 1. Result.Success - если запись выполнена успешно
     * 1. Result.Failure - если возникло исключение
     */
    fun write(content: String, name: String, path: String? = null, context: Context): Result<Unit, Exception> =
        create(name, path, context)
            .flatMap { write(it, content) }

    /**
     * Записывает данные [content] в ресурс [resource]. Ресурс должен физически существовать, иначе возможно исключение
     *
     * @return
     * 1. Result.Success - если запись выполнена успешно
     * 1. Result.Failure - если возникло исключение
     */
    fun write(resource: T, content: String): Result<Unit, Exception>

    /**
     * Читает все данные из ресурса с именем [name].
     *
     * @param name полное имя файла, с расришением
     * @param path относительный путь к директории файла
     * @return
     * 1. Result.Success - если чтение прошло успешно
     * 1. Result.Failure - если возникло исключение
     */
    fun read(name: String, path: String? = null, context: Context): Result<String, Exception> =
        get(name, path, context)
            .flatMap { read(it) }

    /**
     * Читает все данные из ресурса [resource].
     *
     * @return
     * 1. Result.Success - если чтение прошло успешно
     * 1. Result.Failure - если возникло исключение
     */
    fun read(resource: T): Result<String, Exception>

    /**
     * Удаляет ресурс с именем [name]
     *
     * @param name полное имя файла, с расришением
     * @param path относительный путь к директории файла
     * @return
     * 1. Result.Success(true) - если ресурс удален
     * 1. Result.Success(false) - если не был удален (не существовал)
     * 1. Result.Failure - если возникло исключение
     */
    fun delete(name: String, path: String? = null, context: Context): Result<Boolean, Exception> =
        get(name, path, context)
            .flatMap { delete(it, context) }

    /**
     * Удаляет ресурс [resource]
     *
     * @return
     * 1. Result.Success(true) - если ресурс удален
     * 1. Result.Success(false) - если не был удален (не существовал)
     * 1. Result.Failure - если возникло исключение
     */
    fun delete(resource: T, context: Context): Result<Boolean, Exception>

    /**
     * Открывает [InputStream] для ресурса с именем [name]
     *
     * @param name полное имя файла, с расришением
     * @param path относительный путь к директории файла
     * @return
     * 1. Result.Success - если [InputStream] успешно открыт
     * 1. Result.Failure - если возникло исключение
     */
    fun openInputStream(name: String, path: String? = null, context: Context): Result<InputStream, Exception> =
        get(name, path, context)
            .flatMap { openInputStream(it) }

    /**
     * Открывает [InputStream] для ресурса [resource]
     *
     * @return
     * 1. Result.Success - если [InputStream] успешно открыт
     * 1. Result.Failure - если возникло исключение
     */
    fun openInputStream(resource: T): Result<InputStream, Exception>

    /**
     * Открывает [OutputStream] для ресурса с именем [name]
     *
     * @param name полное имя файла, с расришением
     * @param path относительный путь к директории файла
     * @return
     * 1. Result.Success - если [OutputStream] успешно открыт
     * 1. Result.Failure - если возникло исключение
     */
    fun openOutputStream(name: String, path: String? = null, context: Context): Result<OutputStream, Exception> =
        getOrCreate(name, path, context)
            .flatMap { openOutputStream(it) }

    /**
     * Открывает [OutputStream] для ресурса [resource]
     *
     * @return
     * 1. Result.Success - если [OutputStream] успешно открыт
     * 1. Result.Failure - если возникло исключение
     */
    fun openOutputStream(resource: T): Result<OutputStream, Exception>

    /**
     * Перемещает содержимое ресурса с именем [srcName] в хранилище [dstStorage] в ресурс с именем [dstName]
     *
     * @param srcName полное имя исходного файла, с расришением
     * @param srcPath относительный путь к директории исходного файла
     * @param dstStorage хранилище целевого файла, куда производится копирование
     * @param dstName полное имя целевого файла, с расришением
     * @param dstPath относительный путь к директории целевого файла
     * @return
     * 1. Result.Success - если копирование успешно произведено
     * 1. Result.Failure - если возникло исключение
     */
    fun move(
        srcName: String,
        srcPath: String? = null,
        dstStorage: ContentStorage<*> = this,
        dstName: String,
        dstPath: String? = null,
        context: Context,
    ): Result<Unit, Exception> =
        get(srcName, srcPath, context).flatMap { move(it, dstStorage, dstName, dstPath, context) }

    /**
     * Перемещает ресурс [src] в хранилище [dstStorage] в ресурс с именем [dstName]
     *
     * @param src исходный файл
     * @param dstStorage хранилище целевого файла, куда производится копирование
     * @param dstName полное имя целевого файла, с расришением
     * @param dstPath относительный путь к директории целевого файла
     * @return
     * 1. Result.Success - если копирование успешно произведено
     * 1. Result.Failure - если возникло исключение
     */
    fun move(
        src: T,
        dstStorage: ContentStorage<*> = this,
        dstName: String,
        dstPath: String? = null,
        context: Context,
    ): Result<Unit, Exception> =
        copy(src, dstStorage, dstName, dstPath, context).onSuccess { delete(src, context) }

    /**
     * Копирует содержимое ресурса с именем [srcName] в хранилище [dstStorage] в ресурс с именем [dstName]
     *
     * @param srcName полное имя исходного файла, с расришением
     * @param srcPath относительный путь к директории исходного файла
     * @param dstStorage хранилище целевого файла, куда производится копирование
     * @param dstName полное имя целевого файла, с расришением
     * @param dstPath относительный путь к директории целевого файла
     * @return
     * 1. Result.Success - если копирование успешно произведено
     * 1. Result.Failure - если возникло исключение
     */
    fun copy(
        srcName: String,
        srcPath: String? = null,
        dstStorage: ContentStorage<*> = this,
        dstName: String,
        dstPath: String? = null,
        context: Context,
    ): Result<Unit, Exception> =
        get(srcName, srcPath, context).flatMap { copy(it, dstStorage, dstName, dstPath, context) }

    /**
     * Копирует ресурс [src] в хранилище [dstStorage] в ресурс с именем [dstName]
     *
     * @param src исходный файл
     * @param dstStorage хранилище целевого файла, куда производится копирование
     * @param dstName полное имя целевого файла, с расришением
     * @param dstPath относительный путь к директории целевого файла
     * @return
     * 1. Result.Success - если копирование успешно произведено
     * 1. Result.Failure - если возникло исключение
     */
    fun copy(
        src: T,
        dstStorage: ContentStorage<*> = this,
        dstName: String,
        dstPath: String? = null, context: Context,
    ): Result<Unit, Exception> = Result.of {
        val streamIn = openInputStream(src).get()
        val streamOut = dstStorage.openOutputStream(dstName, dstPath, context).get()
        streamIn.copyTo(streamOut)
        streamIn.close()
        streamOut.close()
    }

    /**
     * Возвразает uri, подходящую для отправки в сторонние приложения
     *
     * @param name полное имя файла, с расришением
     * @param path относительный путь к директории файла
     * @return
     * 1. Result.Success - если uri сформирован
     * 1. Result.Failure - если возникло исключение
     */
    fun shareUri(name: String, path: String? = null, context: Context): Result<Uri?, Exception>

    fun requiredPermissions(read: Boolean, write: Boolean): Array<String>
}