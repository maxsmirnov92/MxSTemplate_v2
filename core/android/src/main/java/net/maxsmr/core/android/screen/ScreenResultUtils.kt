package net.maxsmr.core.android.screen

import androidx.core.os.BundleCompat.getParcelable
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Ключ, используемый для хранения результата экрана в Bundle.
 * Используется внутри реализации механизма передачи результатов между экранами.
 * */
@PublishedApi
internal const val SCREEN_RESULT_KEY = "result"

/**
 * Устанавливает результат экрана для передачи родительскому экрану.
 * Он будет передан через ScreenResultListener, сразу **после завершения работы** текущего экрана
 *
 * В качестве ключа используется имя класса результата.
 *
 * @param value Результат экрана, который нужно передать. Должен реализовывать интерфейс [ScreenResult].

 * @author shmoilov.ee
 * */
inline fun <reified T : ScreenResult> Fragment.setScreenResult(value: T) {
    if (this is DialogFragment) {
        // DialogFragment не приостанавливает родительский фрагмент,
        // поэтому ScreenResultListener будет прослушивать значения всё время.
        // Чтобы избежать ранней передачи результата, для диалогов устанавливаем результат только при уничтожении
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                setFragmentResult(T::class.java.simpleName, bundleOf(SCREEN_RESULT_KEY to value))
            }
        })
    } else {
        setFragmentResult(T::class.java.simpleName, bundleOf(SCREEN_RESULT_KEY to value))
    }
}

/**
 * Устанавливает слушатель для получения результата дочернего экрана.
 * Он будет получать результаты дочерних экранов сразу после их завершения работы
 *
 * В качестве ключа используется имя класса ожидаемого результата.
 *
 * @param listener Функция обратного вызова, которая будет вызвана при получении результата.
 * Принимает результат типа [T], реализующий интерфейс [ScreenResult].
 * */
inline fun <reified T : ScreenResult> Fragment.setScreenResultListener(crossinline listener: (T) -> Unit) {
    setFragmentResultListener(T::class.java.simpleName) { _, bundle ->
        val result = getParcelable(bundle, SCREEN_RESULT_KEY, T::class.java)
        result?.let(listener)
    }
}
