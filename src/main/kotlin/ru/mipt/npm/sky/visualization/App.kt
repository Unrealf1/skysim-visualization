package ru.mipt.npm.sky.visualization

import javafx.application.Application
import javafx.beans.binding.DoubleBinding
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.value.ObservableDoubleValue
import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.util.StringConverter
import kotlin.math.max


class App(
    private val windowWidth: SimpleDoubleProperty = SimpleDoubleProperty(1200.0),
    private val windowHeight: SimpleDoubleProperty = SimpleDoubleProperty(900.0)
) : Application() {

    /**
     * Here and hereinafter UI stands for "User interface"
     * and by that I mean buttons/labels etc
     */
    class UIWidth(
        private val windowWidth: ObservableDoubleValue,
        private val visualizationWidth: ObservableDoubleValue
    ) : DoubleBinding() {
        init {
            bind(windowWidth, visualizationWidth)
        }

        override fun computeValue(): Double = windowWidth.doubleValue() - visualizationWidth.doubleValue()
    }

    /**
     * Here and hereinafter VS stands for "Visualization"
     * and by that I mean part of the window with interactive model
     */
    class VSWidth(private val windowWidth: ObservableDoubleValue) : DoubleBinding() {
        init {
            bind(windowWidth)
        }

        private val maxUIWidth = 300.0

        override fun computeValue(): Double {
            return max(windowWidth.get() * 0.75, windowWidth.get() - maxUIWidth)
        }
    }

    private val visualizationWidth = VSWidth(windowWidth)

    private val visualizationHeight = SimpleDoubleProperty(windowHeight.get())

    private val uiWidth = UIWidth(windowWidth, visualizationWidth)

    private val uiHeight = SimpleDoubleProperty(windowHeight.get())

    enum class EButton {
        stop, back, play, forward, pause
    }

    object UnitStringConverter : StringConverter<String>() {
        override fun toString(`object`: String?): String = `object` ?: ""
        override fun fromString(string: String?): String = string ?: ""
    }

    val simulationParameters = SimulationViewModel()

    /**
     * Initial bindings
     */
    private fun bindProperties(mainStage: Stage) {
        windowWidth.bind(mainStage.widthProperty())
        windowHeight.bind(mainStage.heightProperty())

        visualizationHeight.bind(windowHeight)

        uiHeight.bind(windowHeight)
    }

    override fun start(mainStage: Stage) {
        mainStage.minHeight = 530.0
        mainStage.minWidth = 1000.0


        val model = SimulationViewModel()

        val view = View(model, visualizationWidth, visualizationHeight)

        val scene = Scene(view.buildRoot(mainStage), windowWidth.get(), windowHeight.get(), false)
        scene.fill = Color.BLACK
        bindProperties(mainStage)

        mainStage.title = "Skysim"
        mainStage.scene = scene
        mainStage.show()
    }

    companion object {
        private const val version = "1.0.0"

        val VERSION_MESSAGE = """	
            Visualizer version: $version
	        Simulator version: ${ru.mipt.npm.sky.version}
        """.trimIndent()
    }
}

fun main(args: Array<String>) {
    println(App.VERSION_MESSAGE)
    // This init some javafx stuff and then run
    // App.start(mainStage: Stage)
    Application.launch(App::class.java)
}
