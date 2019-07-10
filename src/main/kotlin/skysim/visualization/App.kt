package skysim.visualization

import javafx.application.Application
import javafx.event.EventHandler
import javafx.scene.control.Button
import javafx.stage.Stage
import javafx.scene.*
import javafx.scene.paint.*
import javafx.scene.shape.*
import javafx.scene.transform.Rotate
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.input.MouseEvent
import javafx.scene.layout.*
import javafx.scene.Scene
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.VBox
import javafx.stage.Modality
import kotlinx.coroutines.runBlocking

class SimulationParameters(
    var cell_length: Double? = null,
    var cloud_size: Double = 100.0,
    var dynamic_plot: Boolean? = null,
    var field_magnitude: Double? = null,
    var free_path: Double? = null,
    var gain: Double? = null,
    var particle_limit: Double? = null,
    var output: String? = null,
    var seed: Double? = null,
    var save_plot: String? = null,
    var seed_photons: String? = null
)

class App: Application() {
    private val width = 1200.0
    private val height = 1000.0
    private val visualizationWidth = 900.0
    private val visualizationHeight = height
    private val uiWidth = width - visualizationWidth
    private val uiHeight = height

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
        private fun prepareControlPanel(visualizer: Visualizer): Node {
            val panel = HBox()
            panel.spacing = 5.0

            val label_counter = Label("0 / 0")
            val back = Button("<-")
            back.onAction = EventHandler {
                visualizer.showPrevGeneration()
                label_counter.text = "${visualizer.getCurrentGen() + 1} / ${visualizer.size()}"
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
                label_counter.text = "${visualizer.getCurrentGen() + 1} / ${visualizer.size()}"
            }
            panel.children.addAll(back, play, forward, label_counter)
            return panel
        }

        private val parametersSpasing = 15.0

        private fun prepareParameterLine(name: String): Node {
            val textfield = TextField()
            val label = Label(name)
            val node = HBox()
            node.spacing = parametersSpasing
            node.children.addAll(label, textfield)
            return node
        }

        private fun prepareUIParametersButton(): Button {
            return Button("Apply")
        }

        private fun prepareUIParameters(): Node {
            val uiParameters = VBox()
            uiParameters.children.add(prepareParameterLine("cell-length"))
            uiParameters.children.add(prepareParameterLine("cloud-size"))
            uiParameters.children.add(Button("dynamic-plot"))
            uiParameters.children.add(prepareParameterLine("field-magnitude"))
            uiParameters.children.add(prepareParameterLine("free-path"))
            uiParameters.children.add(prepareParameterLine("gain"))
            uiParameters.children.add(prepareParameterLine("particle-limit"))
            uiParameters.children.add(prepareParameterLine("output"))
            uiParameters.children.add(prepareParameterLine("seed"))
            uiParameters.children.add(prepareParameterLine("save-plot"))
            uiParameters.children.add(prepareParameterLine("seed-photons"))

            uiParameters.children.add(prepareUIParametersButton())

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
                }
            }
            return startButton
        }

        fun prepareUI(visualizer: Visualizer): Node {
            val root = VBox()
            root.spacing = 10.0
            root.maxHeight = 50.0

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
            return scene
        }
    }
    private val uiPreparator = UIPreparator()

    val simulationParameters = SimulationParameters()

    override fun start(mainStage: Stage) {
        val root = GridPane()
        val scene = Scene(root, width, height, true)
        scene.fill = Color.BLACK

        // Configure layout
        root.isGridLinesVisible = false
        root.hgap = 0.0
        root.vgap = 0.0

        val visualization = visualizerPreparator.prepareVisualization(simulationParameters.cloud_size)
        val ui = uiPreparator.prepareUI(visualization)

        root.add(ui, 1, 0)
        root.add(visualization.scene, 0, 0)

        mainStage.title = "skysim"
        mainStage.scene = scene
        mainStage.show()
    }

    companion object {
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
