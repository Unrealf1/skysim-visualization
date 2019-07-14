package skysim.visualization

import javafx.animation.Animation
import javafx.application.Application
import javafx.event.EventHandler
import javafx.stage.Stage
import javafx.scene.*
import javafx.scene.paint.*
import javafx.scene.shape.*
import javafx.scene.transform.Rotate
import javafx.geometry.Point2D
import javafx.scene.input.MouseEvent
import javafx.scene.layout.*
import javafx.scene.Scene
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.VBox
import javafx.stage.Modality
import kotlinx.coroutines.*
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.beans.binding.DoubleBinding
import javafx.beans.property.*
import javafx.beans.value.ObservableDoubleValue
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.util.Duration
import kotlin.math.max


class SimulationParameters(
        var cell_length: SimpleStringProperty = SimpleStringProperty("0.0"),
        var cloud_size: SimpleStringProperty = SimpleStringProperty("1000.0"),
        var dynamic_plot: SimpleBooleanProperty = SimpleBooleanProperty(false),
        var field_magnitude: SimpleStringProperty = SimpleStringProperty("0.0"),
        var free_path: SimpleStringProperty = SimpleStringProperty("0.0"),
        var gain: SimpleStringProperty = SimpleStringProperty("0.0"),
        var particle_limit: SimpleStringProperty = SimpleStringProperty("15000"),
        var output: SimpleStringProperty = SimpleStringProperty(""),
        var seed: SimpleStringProperty = SimpleStringProperty(""),
        var save_plot: SimpleStringProperty = SimpleStringProperty("./last_simulation"),
        var seed_photons: SimpleStringProperty = SimpleStringProperty("")
)
class App(
        private val windowWidth: SimpleDoubleProperty = SimpleDoubleProperty(1200.0),
        private val windowHeight: SimpleDoubleProperty = SimpleDoubleProperty(900.0)): Application() {

    class UIWidth(
            private val windowWidth: ObservableDoubleValue,
            private val visualizationWidth: ObservableDoubleValue): DoubleBinding() {
        init{
            bind(windowWidth, visualizationWidth)
        }

        override fun computeValue(): Double  = windowWidth.doubleValue() - visualizationWidth.doubleValue()
    }

    class VSWidth(private val windowWidth: ObservableDoubleValue): DoubleBinding() {
        init{
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

    class MouseController {

        // Rotation with mouse from
        // https://www.genuinecoder.com/javafx-3d-tutorial-object-transform-rotation-with-mouse/
        //
        // Tracks drag starting point for x and y
        private var anchorX = 0.0
        private var anchorY = 0.0
        // Keep track of current angle for x and y
        private var anchorAngleX = 0.0
        private var anchorAngleY = 0.0
        // Update these after drag. Using JavaFX property to bind with object
        private val angleX = SimpleDoubleProperty(0.0)
        private val angleY = SimpleDoubleProperty(0.0)

        fun initMouseControl(group: Group, scene: SubScene) {
            val xRotate = Rotate(0.0, Rotate.X_AXIS)
            val yRotate = Rotate(0.0, Rotate.Y_AXIS)
            xRotate.angleProperty().bind(angleX)
            yRotate.angleProperty().bind(angleY)

            group.transforms.addAll(xRotate, yRotate)

            val pressed_handler = {event: MouseEvent ->
                anchorX = event.sceneX
                anchorY = event.sceneY
                anchorAngleX = angleX.get()
                anchorAngleY = angleY.get()
            }
            scene.onMousePressed = EventHandler(pressed_handler)

            val dragged_handler = { event: MouseEvent ->
                angleX.set(anchorAngleX - (anchorY - event.sceneY))
                angleY.set(anchorAngleY + anchorX - event.sceneX)

            }
            scene.onMouseDragged = EventHandler(dragged_handler)
        }
    }
    private val mouseController = MouseController()

    inner class VisualizerPreparator {
        private fun prepareVisualizationCamera(fieldSize: Double): Camera {
            val camera = PerspectiveCamera(true)
            camera.translateXProperty().set(0.0)
            camera.translateYProperty().set(0.0)
            camera.translateZProperty().set(-fieldSize * 3.0)
            camera.farClip = 100000.0
            camera.nearClip = 0.0
            return camera
        }

        var boxField: Box = Box()

        fun prepareVisualization(fieldSize: Double): Visualizer {
            val root = Group()
            val scene = SubScene(
                    root,
                    visualizationWidth.get(),
                    visualizationHeight.get(),
                    false,
                    SceneAntialiasing.DISABLED)
            scene.widthProperty().bind(visualizationWidth)
            scene.heightProperty().bind(visualizationHeight)
            scene.fill = Color.BLACK

            val fieldGroup = Group()
            root.children.add(fieldGroup)
            boxField = Box(fieldSize, fieldSize, fieldSize)
            boxField.drawMode = DrawMode.LINE
            fieldGroup.children.add(boxField)

            val photonsGroup = Group()
            fieldGroup.children.add(photonsGroup)

            scene.camera = prepareVisualizationCamera(fieldSize)

            mouseController.initMouseControl(fieldGroup, scene)
            val scroll_handler = { event: ScrollEvent ->
                scene.camera.translateZProperty().set(scene.camera.translateZ +  event.deltaY * 8.0)
            }
            scene.onScroll = EventHandler(scroll_handler)

            return Visualizer(scene, photonsGroup)
        }
    }
    private val visualizerPreparator = VisualizerPreparator()

    enum class EButton {
        stop, back, play, forward, pause
    }

    inner class UIPreparator {
        val controlCounter = SimpleStringProperty("0 / 0")

        fun updateControlCounter(visualizer: Visualizer) {
            controlCounter.set("${visualizer.getCurrentGen() + 1} / ${visualizer.size()}")
        }

        private fun makeArrow(handleUp: Point2D, middleUp: Point2D, point: Point2D): Polygon {
            return Polygon(
                    handleUp.x, handleUp.y,
                    middleUp.x, handleUp.y,
                    middleUp.x, middleUp.y,
                    point.x, point.y,
                    middleUp.x, -middleUp.y,
                    middleUp.x, -handleUp.y,
                    handleUp.x, -handleUp.y)
        }

        private fun prepareButtonVisual(button: EButton): Node {
            return when (button) {
                EButton.back -> makeArrow(
                        Point2D(5.0, 3.0),
                        Point2D(0.0, 5.0),
                        Point2D(-5.0, 0.0))
                        .also { it.fill = Color.BLACK }
                EButton.play -> Polygon(0.0, 5.0, 0.0, -5.0, 8.66, 0.0)
                        .also { it.fill = Color.RED }
                EButton.forward -> makeArrow(
                        Point2D(-5.0, 3.0),
                        Point2D(0.0, 5.0),
                        Point2D(5.0, 0.0))
                        .also { it.fill = Color.BLACK }
                EButton.stop -> Rectangle(10.0, 10.0, Color.BLACK)
                EButton.pause -> HBox(
                        Rectangle(4.0, 10.0, Color.GREY),
                        Rectangle(4.0, 10.0, Color.GREY)
                ).also {
                    it.spacing = 2.0
                    it.alignment = Pos.CENTER
                }
            }
        }

        private fun doPause(playButton: Button, timeline: Timeline) {
            timeline.pause()
            playButton.graphic = prepareButtonVisual(EButton.play)
        }

        private fun doPlay(playButton: Button, timeline: Timeline) {
            timeline.play()
            playButton.graphic = prepareButtonVisual(EButton.pause)
        }

        private fun doStop(playButton: Button, timeline: Timeline) {
            timeline.stop()
            playButton.graphic = prepareButtonVisual(EButton.play)
        }

        private fun prepareControlPanel(visualizer: Visualizer): Node {
            val panel = HBox()
            panel.spacing = 5.0
            panel.alignment = Pos.CENTER

            val play = Button()
            play.graphic = prepareButtonVisual(EButton.play)
            var timeline = Timeline()

            timeline.onFinished = EventHandler{
                println("finished!")
                doPause(play, timeline)
            }
            play.onAction = EventHandler {
                if (timeline.status == Animation.Status.STOPPED) {
                    timeline = Timeline(KeyFrame(
                            Duration.millis(500.0),
                            EventHandler {
                                // This if is here because I don't understand
                                // how timeline.onFinished works
                                // that '+1' some lines later is there for same reason. To be fixed...
                                if (!visualizer.showNextGeneration()) {
                                    doPause(play, timeline)
                                }
                                updateControlCounter(visualizer)
                            }))
                    timeline.cycleCount = visualizer.size() - visualizer.getCurrentGen() + 1
                    doPlay(play, timeline)
                } else if (timeline.status == Animation.Status.PAUSED) {
                    doPlay(play, timeline)
                } else if (timeline.status == Animation.Status.RUNNING) {
                    doPause(play, timeline)
                }
            }

            val stop = Button()
            stop.graphic = prepareButtonVisual(EButton.stop)
            stop.onAction = EventHandler {
                doStop(play, timeline)
                visualizer.setCurrentGen(0)
                updateControlCounter(visualizer)
            }

            val label_counter = Label()
            label_counter.textProperty().bind(controlCounter)

            val back = Button()
            back.graphic = prepareButtonVisual(EButton.back)
            back.onAction = EventHandler {
                doPause(play, timeline)
                visualizer.showPrevGeneration()
                updateControlCounter(visualizer)
            }

            val forward = Button()
            forward.graphic = prepareButtonVisual(EButton.forward)
            forward.onAction = EventHandler {
                doPause(play, timeline)
                visualizer.showNextGeneration()
                updateControlCounter(visualizer)
            }

            panel.children.addAll(stop, back, play, forward, label_counter)
            return panel
        }
        private val parametersHorizontalSpacing = 15.0
        private val parametersVerticalSpacing = 5.0

        private fun prepareParameterLine(name: String, parameter: SimpleStringProperty): Node {
            val textfield = TextField(parameter.get())
            parameter.bind(textfield.textProperty())
            val label = Label(name)
            val node = HBox()
            node.alignment = Pos.TOP_CENTER
            node.spacing = parametersHorizontalSpacing
            node.children.addAll(label, textfield)
            return node
        }

        private fun prepareUIParameters(): Node {
            val uiParameters = VBox()
            uiParameters.spacing = parametersVerticalSpacing
            uiParameters.alignment = Pos.TOP_CENTER

            uiParameters.children.add(prepareParameterLine(Names.cellLength,
                    simulationParameters.cell_length))
            uiParameters.children.add(prepareParameterLine(Names.cloudSize,
                    simulationParameters.cloud_size))
            uiParameters.children.add(prepareParameterLine(Names.fieldMagnitude,
                    simulationParameters.field_magnitude))
            uiParameters.children.add(prepareParameterLine(Names.freePath,
                    simulationParameters.free_path))
            uiParameters.children.add(prepareParameterLine(Names.gain,
                    simulationParameters.gain))
            uiParameters.children.add(prepareParameterLine(Names.particleLimit,
                    simulationParameters.particle_limit))
            uiParameters.children.add(prepareParameterLine(Names.output,
                    simulationParameters.output))
            uiParameters.children.add(prepareParameterLine(Names.seed,
                    simulationParameters.seed))
            uiParameters.children.add(prepareParameterLine(Names.savePlot,
                    simulationParameters.save_plot))
            uiParameters.children.add(prepareParameterLine(Names.seedPhotons,
                    simulationParameters.seed_photons))
            val checkBox = CheckBox(Names.dynamicPlot)
            uiParameters.children.add(checkBox)
            // Is this right property?
            simulationParameters.dynamic_plot.bind(checkBox.selectedProperty())

            return uiParameters
        }

        private fun prepareHelpParameter(name: String, type: String, description: String): Node {
            return Label("$name(this is $type):\n$description")
        }

        private fun prepareHelp(): Node{
            val root = VBox()

            root.alignment = Pos.TOP_CENTER

            root.children.add(prepareHelpParameter(
                    "cell-length",
                    "NUMBER",
                    "set the length of acceleration cell"
            ))

            root.children.add(prepareHelpParameter(
                    "cloud-size",
                    "NUMBER",
                    "set the cloud size"
            ))

            root.children.add(prepareHelpParameter(
                    "dynamic-plot",
                    "BOOL",
                    "start server with dynamic plot"
            ))

            root.children.add(prepareHelpParameter(
                    "field-magnitude",
                    "NUMBER",
                    "set the cloud size"
            ))

            root.children.add(prepareHelpParameter(
                    "free-path",
                    "NUMBER",
                    "set the photon free mean path"
            ))

            root.children.add(prepareHelpParameter(
                    "gain",
                    "NUMBER",
                    "set the local coefficient of gamma"
            ))

            root.children.add(prepareHelpParameter(
                    "particle-limit",
                    "NUMBER",
                    "set the upper limit of number of particle"
            ))

            root.children.add(prepareHelpParameter(
                    "output",
                    "FILENAME",
                    "print simulation result in the file with"
            ))

            root.children.add(prepareHelpParameter(
                    "seed",
                    "NUMBER",
                    "set the random generator seed"
            ))

            root.children.add(prepareHelpParameter(
                    "save-plot",
                    "FILENAME",
                    "save graph of simulation result in the\n" +
                            "html-file with given name"
            ))

            root.children.add(prepareHelpParameter(
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
            ))

            return root
        }

        private fun prepareHelpButton(): Button {
            val helpButton = Button("Help&Version")
            helpButton.onAction = EventHandler{
                val popupWindow = Stage()
                popupWindow.initModality(Modality.APPLICATION_MODAL)
                popupWindow.title = "Help and Version"
                popupWindow.isResizable = false

                val content = prepareHelp()

                val closeHelpButton = Button("Close")
                closeHelpButton.setOnAction { popupWindow.close() }

                val versionMessage = Label(versionMessage)

                val layout = VBox(10.0)
                layout.alignment = Pos.TOP_CENTER
                layout.children.addAll(versionMessage, content, closeHelpButton)
                layout.alignment = Pos.CENTER

                val popupScene = Scene(layout, 400.0, 700.0)

                popupWindow.scene = popupScene
                popupWindow.showAndWait()
            }
            return helpButton
        }

        private fun prepareStartButton(visualizer: Visualizer): Button {
            val startButton = Button("Start simulation")
            startButton.onAction = EventHandler {
                visualizer.clear()
                runBlocking {
                    val channel = launchSkysim(simulationParameters)
                    var current_generation: Generation
                    var i = 0
                    do {
                        current_generation = channel.receive()
                        visualizer.addGeneration(current_generation)
                        i += current_generation.photons.size
                    } while (current_generation.photons.size != 0)
                    updateControlCounter(visualizer)
                }
            }
            return startButton
        }

        private fun prepareFullScreenButton(mainStage: Stage): Button {
            val button = Button("Fullscreen")
            button.onAction = EventHandler {
                mainStage.isFullScreen = !mainStage.isFullScreen
                if (mainStage.isFullScreen) {
                    button.text = "Window"
                } else {
                    button.text = "Fullscreen"
                }
            }
            return button
        }

        private fun prepareCubeToggleBox(): CheckBox{
            val checkBox = CheckBox("Show Cube")
            checkBox.isSelected = true
            visualizerPreparator.boxField.visibleProperty().bind(checkBox.selectedProperty())
            return checkBox
        }

        fun prepareUI(visualizer: Visualizer, mainStage: Stage): Node {
            val root = VBox()
            root.spacing = 10.0
            root.maxHeight = 50.0
            root.alignment = Pos.TOP_CENTER
            root.background = Background(
                    BackgroundFill(
                            Color.TRANSPARENT,
                            CornerRadii(0.0),
                            Insets(0.0)))


            val scene = SubScene(
                    root,
                    uiWidth.get(),
                    uiHeight.get(),
                    false,
                    SceneAntialiasing.DISABLED
            )
            scene.widthProperty().bind(uiWidth)
            scene.heightProperty().bind(uiHeight)

            scene.fill = Color.WHEAT

            root.children.add(prepareControlPanel(visualizer))

            val uiParameters = prepareUIParameters()
            root.children.add(uiParameters)

            root.children.add(prepareCubeToggleBox())

            root.children.add(prepareHelpButton())

            root.children.add(prepareStartButton(visualizer))

            root.children.add(prepareFullScreenButton(mainStage))

            return scene
        }
    }
    private val uiPreparator = UIPreparator()

    val simulationParameters = SimulationParameters()

    private fun bindProperties(mainStage: Stage) {
        windowWidth.bind(mainStage.widthProperty())
        windowHeight.bind(mainStage.heightProperty())

        visualizationHeight.bind(windowHeight)

        uiHeight.bind(windowHeight)
    }

    override fun start(mainStage: Stage) {
        mainStage.minHeight = 530.0
        mainStage.minWidth = 1000.0

        val root = GridPane()
        val scene = Scene(root, windowWidth.get(), windowHeight.get(), false)
        scene.fill = Color.BLACK
        bindProperties(mainStage)

        // Configure layout
        root.isGridLinesVisible = false
        root.hgap = 0.0
        root.vgap = 0.0

        val visualization = visualizerPreparator.prepareVisualization(
                simulationParameters.cloud_size.get().toDouble())

        val ui = uiPreparator.prepareUI(visualization, mainStage)

        root.add(ui, 1, 0)
        root.add(visualization.scene, 0, 0)

        mainStage.title = "skysim"
        mainStage.scene = scene
        mainStage.show()
    }

    companion object {
        object Names {
            val cellLength = "cell-length"
            val cloudSize = "cloud-size"
            val fieldMagnitude = "field-magnitude"
            val freePath = "free-path"
            val gain = "gain"
            val particleLimit = "particle-limit"
            val output = "output"
            val seed = "seed"
            val savePlot = "save-plot"
            val seedPhotons = "seed-photons"
            val dynamicPlot = "dynamic-plot"
        }
        private val version = "0.3.0"

        private val versionMessage = "\tVisualizer version: " + version + "\n" +
                "\tSimulator version: " + skysim.sky.version + "\n"

        val greeting: String
            get() {
                return versionMessage
            }
    }
}

fun main(args: Array<String>) {
    println(App.greeting)
    Application.launch(App::class.java)

}
