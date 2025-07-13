package net.maxsmr.core.ui.alert.representation

import net.maxsmr.core.android.base.alert.Alert

/**
 * Абстракция любой формы представления сообщения пользователю
 *
 * @see Alert
 */
interface AlertRepresentation

/**
 * Держатель для [AlertRepresentation] по конкретному тэгу
 */
class AlertRepresentationHolder<AR : AlertRepresentation>(
    private val representationFactory: (Alert) -> AR,
) {

    /**
     * [Alert] и актуальный [AR] для него
     */
    private val representationsMap: MutableMap<Alert, AR> = mutableMapOf()

    fun getOrCreate(alert: Alert): AR {
        return representationsMap[alert] ?: run {
            representationFactory(alert).also {
                representationsMap[alert] = it
            }
        }
    }

    fun remove(alert: Alert): AR? {
        return representationsMap.remove(alert)
    }

    fun clear(): List<AR> {
        val representations = representationsMap.values.toList()
        representationsMap.clear()
        return representations
    }
}

sealed interface AlertRepresentationResult {

    class ShowAlert<AR : AlertRepresentation>(
        val representation: AR,
    ) : AlertRepresentationResult

    class HideAlerts<AR : AlertRepresentation>(
        val representations: List<AR>,
    ) : AlertRepresentationResult
}
