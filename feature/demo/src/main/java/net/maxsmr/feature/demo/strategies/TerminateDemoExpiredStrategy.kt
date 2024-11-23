package net.maxsmr.feature.demo.strategies

import kotlin.system.exitProcess

class TerminateDemoExpiredStrategy: IDemoExpiredStrategy {

    override fun doAction() {
        exitProcess(0)
    }
}