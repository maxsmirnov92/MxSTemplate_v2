package net.maxsmr.core.android.content.pick.concrete

/**
 * Тип конкретного пикера. Используется, чтобы определить, какой [ConcretePicker] должен обработать
 * результат. Также может использоваться клиентским кодом для определения источника, откуда взят контент
 */
enum class ConcretePickerType {

    PHOTO,
    VIDEO,
    MEDIA,
    SAF;
}