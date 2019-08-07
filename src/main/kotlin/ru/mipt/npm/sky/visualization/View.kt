package ru.mipt.npm.sky.visualization

import javafx.animation.Animation
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.beans.property.Property
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableDoubleValue
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Point2D
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Polygon
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.util.Duration
import javafx.util.StringConverter
import javafx.util.converter.BooleanStringConverter
import javafx.util.converter.NumberStringConverter
import kotlinx.coroutines.runBlocking

/**
 * This class produces UI via function "prepareUI"
 * and provides some control over UI after
 * ('updateControlCounter' for example)
 */
class View(val model: SimulationViewModel, val width: ObservableDoubleValue, val height: ObservableDoubleValue) {

    private val playButton = Button()

    /**
     * This timeline is in charge of playing back generations,
     * when 'play' button is pressed
     */
    private var playTimeline = Timeline()

    /**
     * Here and hereinafter controlCounter is refering to
     * label at the top of the UI which display the number
     * of generation on the screen
     */
    val controlCounter = SimpleStringProperty("0 / 0")


    fun updateControlCounter(visualizer: Visualizer) {
        controlCounter.set("${visualizer.getCurrentGen() + 1} / ${visualizer.size()}")
    }

    /**
     * This function creates a symmetrical arrow
     */
    private fun makeArrow(handleUp: Point2D, middleUp: Point2D, point: Point2D): Polygon {
        return Polygon(
            handleUp.x, handleUp.y,
            middleUp.x, handleUp.y,
            middleUp.x, middleUp.y,
            point.x, point.y,
            middleUp.x, -middleUp.y,
            middleUp.x, -handleUp.y,
            handleUp.x, -handleUp.y
        )
    }

    private fun prepareButtonVisual(button: App.EButton): Node {
        return when (button) {
            App.EButton.back -> makeArrow(
                Point2D(5.0, 3.0),
                Point2D(0.0, 5.0),
                Point2D(-5.0, 0.0)
            )
                .also { it.fill = Color.BLACK }
            App.EButton.play -> Polygon(0.0, 5.0, 0.0, -5.0, 8.66, 0.0)
                .also { it.fill = Color.RED }
            App.EButton.forward -> makeArrow(
                Point2D(-5.0, 3.0),
                Point2D(0.0, 5.0),
                Point2D(5.0, 0.0)
            )
                .also { it.fill = Color.BLACK }
            App.EButton.stop -> Rectangle(10.0, 10.0, Color.BLACK)
            App.EButton.pause -> HBox(
                Rectangle(4.0, 10.0, Color.GREY),
                Rectangle(4.0, 10.0, Color.GREY)
            ).also {
                it.spacing = 2.0
                it.alignment = Pos.CENTER
            }
        }
    }

    private fun doPause() {
        playTimeline.pause()
        playButton.graphic = prepareButtonVisual(App.EButton.play)
    }

    private fun doPlay() {
        playTimeline.play()
        playButton.graphic = prepareButtonVisual(App.EButton.pause)
    }

    private fun doStop() {
        playTimeline.stop()
        playButton.graphic = prepareButtonVisual(App.EButton.play)
    }

    /**
     * Control panel is set of buttons on top of the UI
     * (play, stop, next generation, previous generation, etc)
     */
    private fun VBox.controlPanel(canvas: Canvas3D): Node {
        //FIXME calling lazy property in UI visualization
        val visualizer = canvas.visualizer

        val panel = HBox()
        panel.spacing = 5.0
        panel.alignment = Pos.CENTER

        val play = playButton
        play.graphic = prepareButtonVisual(App.EButton.play)

        playTimeline.onFinished = EventHandler {
            println("playTimeline finished!")
            doPause()
        }
        play.onAction = EventHandler {
            when {
                playTimeline.status == Animation.Status.STOPPED -> {
                    playTimeline = Timeline(
                        KeyFrame(
                            Duration.millis(500.0),
                            EventHandler {
                                // This if is here because I don't understand
                                // how timeline.onFinished works
                                // that '+1' some lines later is there for same reason. To be fixed...
                                if (!visualizer.showNextGeneration()) {
                                    doPause()
                                }
                                updateControlCounter(visualizer)
                            })
                    )
                    playTimeline.cycleCount = visualizer.size() - visualizer.getCurrentGen() + 1
                    doPlay()
                }
                playTimeline.status == Animation.Status.PAUSED -> doPlay()
                playTimeline.status == Animation.Status.RUNNING -> doPause()
            }
        }

        val stop = Button()
        stop.graphic = prepareButtonVisual(App.EButton.stop)
        stop.onAction = EventHandler {
            doStop()
            visualizer.setCurrentGen(0)
            updateControlCounter(visualizer)
        }

        val label_counter = Label()
        label_counter.textProperty().bind(controlCounter)

        val back = Button()
        back.graphic = prepareButtonVisual(App.EButton.back)
        back.onAction = EventHandler {
            doPause()
            visualizer.showPrevGeneration()
            updateControlCounter(visualizer)
        }

        val forward = Button()
        forward.graphic = prepareButtonVisual(App.EButton.forward)
        forward.onAction = EventHandler {
            doPause()
            visualizer.showNextGeneration()
            updateControlCounter(visualizer)
        }

        panel.children.addAll(stop, back, play, forward, label_counter)
        return panel.also { children.add(it) }
    }

    private val parametersHorizontalSpacing = 15.0
    private val parametersVerticalSpacing = 5.0

    private fun <T : Any> VBox.field(
        labelText: String,
        parameter: Property<T>,
        converter: StringConverter<T>
    ): Node {
        val textfield = TextField().apply {
            textProperty().bindBidirectional(parameter, converter)
        }
        val pane = Pane()
        HBox.setHgrow(pane, Priority.ALWAYS)
        val label = Label(labelText).apply {
            alignment = Pos.CENTER_LEFT
        }


        val node = HBox(label, pane, textfield)
        node.alignment = Pos.CENTER
        node.spacing = parametersHorizontalSpacing
        return node.also { children.add(it) }
    }


    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Any> VBox.field(
        labelText: String,
        parameter: Property<T>
    ) {
        when (T::class) {
            String::class -> field(labelText, parameter, App.UnitStringConverter as StringConverter<T>)
            Number::class -> field(labelText, parameter, NumberStringConverter() as StringConverter<T>)
            Boolean::class -> field(labelText, parameter, BooleanStringConverter() as StringConverter<T>)
            else -> error("Can't convert from string to ${T::class}")
        }
    }

    /**
     * 'UIParameters' is part of the UI, containing simulation parameters
     * (gain, field-size, etc)
     */
    private fun form(): Node {
        val form = VBox().apply {
            spacing = parametersVerticalSpacing
            alignment = Pos.TOP_CENTER

            field(cellLength, model.cellLength)
            field(cloudSize, model.cloudSize)
            field(fieldMagnitude, model.fieldMagnitude)
            field(freePath, model.freePath)
            field(gain, model.gain)
            field(particleLimit, model.particleLimit)
            field(output, model.output)
            field(seed, model.seed)
            field(savePlot, model.savePlot)
            field(seedPhotons, model.seedPhotons)

        }
        val checkBox = CheckBox(dynamicPlot)
        form.children.add(checkBox)
        // Is this right property?
        model.dynamicPlot.bind(checkBox.selectedProperty())

        return form
    }

    private val helpFontSize = 17.0
    private val helpParametersFontSize = 12.0

    private fun prepareHelpParameter(name: String, type: String, description: String): Node {
        return Text("$name(this is $type):\n$description\n").also {
            it.font = Font.font(helpParametersFontSize)
        }
    }

    private fun prepareGeneralHelp(): Node {
        return Text(generalHelpText).also {
            it.font = Font.font(helpFontSize)
        }
    }

    private fun prepareHelp(): Node {
        val root = VBox()

        root.alignment = Pos.TOP_LEFT

        val generalHelp = prepareGeneralHelp()
        root.children.add(generalHelp)

        root.children.add(
            prepareHelpParameter(
                "cell-length",
                "NUMBER",
                "set the length of acceleration cell"
            )
        )

        root.children.add(
            prepareHelpParameter(
                "cloud-size",
                "NUMBER",
                "set the cloud size"
            )
        )

        root.children.add(
            prepareHelpParameter(
                "dynamic-plot",
                "BOOL",
                "start server with dynamic plot"
            )
        )

        root.children.add(
            prepareHelpParameter(
                "field-magnitude",
                "NUMBER",
                "set the cloud size"
            )
        )

        root.children.add(
            prepareHelpParameter(
                "free-path",
                "NUMBER",
                "set the photon free mean path"
            )
        )

        root.children.add(
            prepareHelpParameter(
                "gain",
                "NUMBER",
                "set the local coefficient of gamma"
            )
        )

        root.children.add(
            prepareHelpParameter(
                "particle-limit",
                "NUMBER",
                "set the upper limit of number of particle"
            )
        )

        root.children.add(
            prepareHelpParameter(
                "output",
                "FILENAME",
                "print simulation result in the file with"
            )
        )

        root.children.add(
            prepareHelpParameter(
                "seed",
                "NUMBER",
                "set the random generator seed"
            )
        )

        root.children.add(
            prepareHelpParameter(
                "save-plot",
                "FILENAME",
                "save graph of simulation result in the\n" +
                        "html-file with given name"
            )
        )

        root.children.add(
            prepareHelpParameter(
                "seed-photons",
                "FILENAME",
                "set the path to file contains list of\n" +
                        "seed photons in next format:\n" +
                        "POSITION_X POSITION_Y POSITION_Z\n" +
                        "DIRECTION_X DIRECTION_Y DIRECTION_Z\n" +
                        "ENERGY NUMBER\n" +
                        "POSITION_X POSITION_Y POSITION_Z\n" +
                        "DIRECTION_X DIRECTION_Y DIRECTION_Z\n" +
                        "ENERGY NUMBER\n" +
                        "...\n" +
                        "by default using:\n" +
                        "0.0 0.0 cloud-size/2 0.0 0.0 -1.0 1.0 1\n"
            )
        )

        return root
    }

    private fun VBox.helpButton(): Button {
        val helpButton = Button("Help&Version")
        helpButton.onAction = EventHandler {
            val popupWindow = Stage()
            popupWindow.initModality(Modality.APPLICATION_MODAL)
            popupWindow.title = "Help and version"
            //popupWindow.isResizable = false

            val content = prepareHelp()

            val closeHelpButton = Button("Close")
            closeHelpButton.setOnAction { popupWindow.close() }

            val versionMessage = Label(App.VERSION_MESSAGE)

            val layout = VBox(10.0)
            layout.alignment = Pos.TOP_CENTER
            layout.children.addAll(versionMessage, content, closeHelpButton)
            layout.alignment = Pos.CENTER

            val popupScene = Scene(layout)

            popupWindow.scene = popupScene
            popupWindow.showAndWait()
        }
        return helpButton.also { children.add(it) }
    }

    private fun VBox.startButton(canvas: Canvas3D): Button {
        val startButton = Button("Start simulation")
        startButton.onAction = EventHandler {
            doStop()
            canvas.visualizer.clear()

            // Move camera to position near possibly changed cube
            canvas.camera
                .translateZProperty()
                .set(model.cloudSize.value.toDouble() * -3.0)

            runBlocking {
                val channel = launchSkysim(model)
                var currentGeneration: Generation
                var i = 0
                do {
                    currentGeneration = channel.receive()
                    canvas.visualizer.addGeneration(currentGeneration)
                    i += currentGeneration.photons.size
                } while (currentGeneration.photons.isNotEmpty())
                updateControlCounter(canvas.visualizer)
            }
        }
        return startButton.also { children.add(it) }
    }

    private fun VBox.fullScreenButton(mainStage: Stage): Button {
        val button = Button("Fullscreen")
        button.onAction = EventHandler {
            mainStage.isFullScreen = !mainStage.isFullScreen
            if (mainStage.isFullScreen) {
                button.text = "Window"
            } else {
                button.text = "Fullscreen"
            }
        }
        return button.also { children.add(it) }
    }

    private fun VBox.cubeToggleBox(): CheckBox {
        val checkBox = CheckBox("Show Cube")
        checkBox.selectedProperty().bindBidirectional(model.showBox)
        return checkBox.also { children.add(it) }
    }

    fun buildRoot(mainStage: Stage): Parent{
        val canvas = Canvas3D(model, width, height)
        val controls = controls(mainStage,canvas)

        val root = BorderPane()
        root.center = canvas.visualizer.scene
        root.right = controls
        return root
    }

    private fun controls(mainStage: Stage, canvas: Canvas3D): VBox {
        val root = VBox()

        root.spacing = 10.0
        root.maxHeight = 50.0
        root.alignment = Pos.TOP_CENTER
        root.background = Background(
            BackgroundFill(
                Color.TRANSPARENT,
                CornerRadii(0.0),
                Insets(0.0)
            )
        )

        root.controlPanel(canvas)

        val uiParameters = form()
        root.children.add(uiParameters)

        root.cubeToggleBox()

        root.helpButton()

        root.startButton(canvas)

        root.fullScreenButton(mainStage)

        return root
    }

    companion object {
        const val cellLength = "cell-length"
        const val cloudSize = "cloud-size"
        const val fieldMagnitude = "field-magnitude"
        const val freePath = "free-path"
        const val gain = "gain"
        const val particleLimit = "particle-limit"
        const val output = "output"
        const val seed = "seed"
        const val savePlot = "save-plot"
        const val seedPhotons = "seed-photons"
        const val dynamicPlot = "dynamic-plot"

        private val generalHelpText = """
        To run the simulation - adjust parameters on the left and then push
        'Start simulation' button. After this all data generated by simulation
        is stored in visualization and you may see generations of photons one after
        another with help of the control panel located on top left of the window.
        There are several buttons:
            Stop - stop current playback and rewind to the first generation
            Previous(←) - show previous generation (and pause playback)
            Play - start showing generations sequentially. After pressed this button turns into 'Pause'
            Pause - pause current playback
            Next(→) - show next generation (and pause playback)
        Also there is a counter showing number of generation displayed on the screen
        You may restart simulation at any time. All changes to parameters are applied automatically.
        If simulation is restarted, all current data is deleted and playback is stopped
        
        These are parameters of simulation and their brief description:
    """.trimIndent()
    }
}
