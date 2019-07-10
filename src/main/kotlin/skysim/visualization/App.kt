package skysim.visualization

import javafx.application.Application
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.event.EventHandler
import javafx.stage.Stage
import javafx.scene.*
import javafx.scene.paint.*
import javafx.scene.shape.*
import javafx.scene.transform.Rotate
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.input.MouseEvent
import javafx.scene.layout.*
import javafx.scene.Scene
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.VBox
import javafx.stage.Modality
import kotlinx.coroutines.runBlocking

class SimulationParameters(
        var cell_length: SimpleStringProperty = SimpleStringProperty("0.0"),
        var cloud_size: SimpleStringProperty = SimpleStringProperty("1000.0"),
        var dynamic_plot: SimpleBooleanProperty = SimpleBooleanProperty(false),
        var field_magnitude: SimpleStringProperty = SimpleStringProperty("0.0"),
        var free_path: SimpleStringProperty = SimpleStringProperty("0.0"),
        var gain: SimpleStringProperty = SimpleStringProperty("0.0"),
        var particle_limit: SimpleStringProperty = SimpleStringProperty("0"),
        var output: SimpleStringProperty = SimpleStringProperty(""),
        var seed: SimpleStringProperty = SimpleStringProperty("0.0"),
        var save_plot: SimpleStringProperty = SimpleStringProperty(""),
        var seed_photons: SimpleStringProperty = SimpleStringProperty("")
)
//TODO : add minimum window size
class App(
        private val windowWidth: Double = 900.0,
        private val windowHeight: Double = 600.0): Application() {
    private val visualizationWidth = windowWidth * 0.75
    private val visualizationHeight = windowHeight
    private val uiWidth = windowWidth - visualizationWidth
    private val uiHeight = windowHeight

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

        fun prepareVisualization(fieldSize: Double): Visualizer {
            val root = Group()
            val scene = SubScene(
                    root,
                    visualizationWidth,
                    visualizationHeight,
                    true,
                    SceneAntialiasing.DISABLED)
            scene.fill = Color.BLACK

            val fieldGroup = Group()
            root.children.add(fieldGroup)
            val boxField = Box(fieldSize, fieldSize, fieldSize)
            boxField.drawMode = DrawMode.LINE
            fieldGroup.children.add(boxField)

            val photonsGroup = Group()
            fieldGroup.children.add(photonsGroup)

            scene.camera = prepareVisualizationCamera(fieldSize)

            mouseController.initMouseControl(fieldGroup, scene)
            val scroll_handler = { event: ScrollEvent ->
                scene.camera.translateZProperty().set(scene.camera.translateZ +  event.deltaY)
            }
            scene.onScroll = EventHandler(scroll_handler)

            return Visualizer(scene, photonsGroup)
        }
    }
    private val visualizerPreparator = VisualizerPreparator()

    inner class UIPreparator {
        private val minInfoScreenWidth = 10.0
        private val minInfoScreenHeight = 10.0

        private fun updateInfo(){

        }

        private fun prepareInfoScreen(): Node {
            return Group()
            val infoWidth = uiWidth * 0.8
            if (infoWidth < minInfoScreenWidth) {
                return Group()
            }
            val infoHeight = uiHeight * 0.2
            if (infoHeight < minInfoScreenHeight) {
                return Group()
            }

            return Rectangle(infoWidth, infoHeight, Color.YELLOW)
        }

        val controlCounter = SimpleStringProperty("0 / 0")

        fun updateControlCounter(visualizer: Visualizer) {
            controlCounter.set("${visualizer.getCurrentGen() + 1} / ${visualizer.size()}")
        }

        private fun prepareControlPanel(visualizer: Visualizer): Node {
            val panel = HBox()
            panel.spacing = 5.0
            panel.alignment = Pos.TOP_CENTER

            val label_counter = Label()
            label_counter.textProperty().bind(controlCounter)

            val back = Button("<-")
            back.onAction = EventHandler {
                visualizer.showPrevGeneration()
                updateControlCounter(visualizer)
            }
            val play = Button("|>")
            //var playing_flag: AtomicBoolean = AtomicBoolean(false)
            //To do: asynch run
            play.onAction = EventHandler {
                /*play.text = "[]"
                while (visualizer.showNextGeneration()) {
                    Thread.sleep(1000)
                }

                play.text = "|>"*/
                println("Not ready!")
            }
            val forward = Button("->")
            forward.onAction = EventHandler {
                visualizer.showNextGeneration()
                updateControlCounter(visualizer)
            }
            panel.children.addAll(back, play, forward, label_counter)
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

        private fun prepareHelpButton(): Button {
            val helpButton = Button("Help&Version")
            helpButton.onAction = EventHandler{
                val popupWindow = Stage()
                popupWindow.initModality(Modality.APPLICATION_MODAL)
                popupWindow.title = "Help and Version"

                val label = Label(helpMessage)

                val closeHelpButton = Button("Close")
                closeHelpButton.setOnAction { popupWindow.close() }

                val layout = VBox(10.0)
                layout.children.addAll(label, closeHelpButton)
                layout.alignment = Pos.CENTER

                val popupScene = Scene(layout, 600.0, 500.0)
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

        fun prepareUI(visualizer: Visualizer): Node {
            val root = VBox()
            root.spacing = 10.0
            root.maxHeight = 50.0
            root.alignment = Pos.TOP_CENTER

            val scene = SubScene(
                    root,
                    uiWidth,
                    uiHeight,
                    true,
                    SceneAntialiasing.DISABLED
            )
            scene.fill = Color.AQUAMARINE

            root.children.add(prepareControlPanel(visualizer))

            val uiParameters = prepareUIParameters()
            root.children.add(uiParameters)

            root.children.add(prepareHelpButton())

            root.children.add(prepareStartButton(visualizer))

            root.children.add(prepareInfoScreen())

            return scene
        }
    }
    private val uiPreparator = UIPreparator()

    val simulationParameters = SimulationParameters()

    override fun start(mainStage: Stage) {
        val root = GridPane()
        val scene = Scene(root, windowWidth, windowHeight, true)
        scene.fill = Color.BLACK

        // Configure layout
        root.isGridLinesVisible = false
        root.hgap = 0.0
        root.vgap = 0.0

        val visualization = visualizerPreparator.prepareVisualization(
                simulationParameters.cloud_size.get().toDouble())

        val ui = uiPreparator.prepareUI(visualization)

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
        private val version = "0.1.1-Alpha"

        private val helpMessage = "\tVisualizer version: " + version + "\n" +
                "\tSimulator version: " + skysim.sky.version + "\n" +
                "usage: skysim\n" +
                "    --cell-length <NUMBER>       set the length of acceleration cell\n" +
                "                                 (influence on birth point of new photon)\n" +
                "    --cloud-size <NUMBER>        set the cloud size\n" +
                "    --dynamic-plot               start server with dynamic plot\n" +
                "    --field-magnitude <NUMBER>   set the field magnitude\n" +
                "    --free-path <NUMBER>         set the photon free mean path\n" +
                " -g,--gain <NUMBER>              set the local coefficient of gamma\n" +
                "                                 multiplication\n" +
                " -h,--help                       print this message and exit\n" +
                " -l,--particle-limit <NUMBER>    set the upper limit of number of particle\n" +
                " -o,--output <FILENAME>          print simulation result in the file with\n" +
                "                                 given name\n" +
                " -s,--seed <NUMBER>              set the random generator seed\n" +
                "    --save-plot <FILENAME>       save graph of simulation result in the\n" +
                "                                 html-file with given name\n" +
                "    --seed-photons <FILENAME>    set the path to file contains list of\n" +
                "                                 seed photons in next format:\n" +
                "                                 POSITION_X POSITION_Y POSITION_Z\n" +
                "                                 DIRECTION_X DIRECTION_Y DIRECTION_Z\n" +
                "                                 ENERGY NUMBER\n" +
                "                                 POSITION_X POSITION_Y POSITION_Z\n" +
                "                                 DIRECTION_X DIRECTION_Y DIRECTION_Z\n" +
                "                                 ENERGY NUMBER\n" +
                "                                 ...\n" +
                "                                 by default using:\n" +
                "                                 0.0 0.0 cloud-size/2 0.0 0.0 -1.0 1.0 1\n" +
                " -v,--version                    print information about version and exit\n"

        val greeting: String
            get() {
                return "Hello world."
            }
    }
}

fun main(args: Array<String>) {
    println(App.greeting)
    Application.launch(App::class.java)

}
