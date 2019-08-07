package ru.mipt.npm.sky.visualization

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty

class SimulationViewModel {
    val cellLength = SimpleDoubleProperty(100.0)
    val cloudSize = SimpleDoubleProperty(1000.0)
    val dynamicPlot = SimpleBooleanProperty(false)
    val fieldMagnitude = SimpleDoubleProperty(0.2)
    val freePath = SimpleDoubleProperty(100.0)
    val gain = SimpleDoubleProperty(0.0)
    val particleLimit = SimpleIntegerProperty(10000)
    val output = SimpleStringProperty("")
    val seed = SimpleIntegerProperty(-1)
    val savePlot = SimpleStringProperty("./last_simulation")
    val seedPhotons = SimpleStringProperty("")


    val showBox = SimpleBooleanProperty(true)
}