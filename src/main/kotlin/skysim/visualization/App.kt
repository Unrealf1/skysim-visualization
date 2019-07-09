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
import javafx.beans.property.SimpleStringProperty
import javafx.event.ActionEvent
import javafx.geometry.Point3D
import javafx.scene.input.MouseEvent
import javafx.scene.layout.*
import javafx.scene.Scene
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.VBox
import javafx.stage.Modality
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import skysim.sky.skysim
import kotlin.random.Random
import skysim.visualization.launchSkysim
import skysim.sky.version
import java.util.concurrent.atomic.AtomicBoolean

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

class DummyClass(
        val isDouble: Boolean = true,
        var dValue: Double? = null,
        var sValue: String? = null
)

class App: Application() {

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

    private val width = 1200.0
    private val height = 1000.0
    private val visualization_width = 900.0
    private val visualization_height = height
    private val ui_width = width - visualization_width
    private val ui_height = height

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

    private fun initMouseControl(group: Group, scene: SubScene) {
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

    private fun prepareVisualizationCamera(fieldSize: Double): Camera {
        val camera = PerspectiveCamera(true)
        camera.translateXProperty().set(0.0)
        camera.translateYProperty().set(0.0)
        camera.translateZProperty().set(-fieldSize * 3.0)
        camera.farClip = 100000.0
        camera.nearClip = 0.0
        return camera
    }

    private fun prepareVisualization(fieldSize: Double): Visualizer {
        val root = Group()
        val scene = SubScene(
                root,
                visualization_width,
                visualization_height,
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

        initMouseControl(fieldGroup, scene)
        val scroll_handler = { event: ScrollEvent ->
            scene.camera.translateZProperty().set(scene.camera.translateZ +  event.deltaY)
        }
        scene.onScroll = EventHandler(scroll_handler)

        return Visualizer(scene, photonsGroup)
    }

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

    val simulationParameters = SimulationParameters()

    private fun prepareUIButton(name: String, value: DummyClass): Node {
        val button = Button("ok")
        val textfield = TextField()
        if (value.isDouble.not()) {
            button.onAction = EventHandler{
                value.dValue = textfield.text.toDouble()
            }
        } else {
            button.onAction = EventHandler{
                value.sValue = textfield.text
            }
        }

        val label = Label(name)
        val node = HBox()
        node.children.addAll(label, textfield, button)
        return node
    }

    private fun prepareUI(visualizer: Visualizer): Node {
        val root = VBox()
        root.spacing = 10.0
        root.maxHeight = 50.0

        val scene = SubScene(
                root,
                ui_width,
                ui_height,
                true,
                SceneAntialiasing.DISABLED
        )
        scene.fill = Color.AQUAMARINE

        root.children.add(prepareControlPanel(visualizer))
        //root.children.add(Accordion())
        root.children.add(prepareUIButton(
                "cell-length",
                DummyClass(true, simulationParameters.cell_length, null)))
        root.children.add(prepareUIButton(
                "cloud-size",
                DummyClass(true, simulationParameters.cloud_size, null)))
        root.children.add(Button("dynamic-plot"))
        root.children.add(prepareUIButton(
                "field-magnitude",
                DummyClass(true, simulationParameters.field_magnitude, null)))
        root.children.add(prepareUIButton(
                "free-path",
                DummyClass(true, simulationParameters.free_path, null)))
        root.children.add(prepareUIButton(
                "gain",
                DummyClass(true, simulationParameters.gain, null)))
        root.children.add(prepareUIButton(
                "particle-limit",
                DummyClass(true, simulationParameters.particle_limit, null)))
        root.children.add(prepareUIButton(
                "output",
                DummyClass(false, null, simulationParameters.output)))
        root.children.add(prepareUIButton(
                "seed",
                DummyClass(true, simulationParameters.seed, null)))
        root.children.add(prepareUIButton(
                "save-plot",
                DummyClass(false, null, simulationParameters.save_plot)))
        root.children.add(prepareUIButton(
                "seed-photons",
                DummyClass(false, null,  simulationParameters.seed_photons)))
        val help_button = Button("Help&Version")
        help_button.onAction = EventHandler{
            val popupwindow = Stage()
            popupwindow.initModality(Modality.APPLICATION_MODAL)
            popupwindow.title = "Help and Version"

            val label = Label(helpMessage)

            val close_help_button = Button("Close")
            close_help_button.setOnAction { popupwindow.close() }

            val layout = VBox(10.0)
            layout.children.addAll(label, close_help_button)
            layout.alignment = Pos.CENTER

            val popup_scene = Scene(layout, 600.0, 500.0)
            popupwindow.scene = popup_scene
            popupwindow.showAndWait()
        }
        root.children.add(help_button)
        val start_simulation = Button("Start simulation")
        start_simulation.onAction = EventHandler {
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
        root.children.add(start_simulation)
        return scene
    }

    override fun start(mainStage: Stage) {
        val root = GridPane()
        val scene = Scene(root, width, height, true)
        scene.fill = Color.BLACK

        // Configure layout
        root.isGridLinesVisible = false
        root.hgap = 0.0
        root.vgap = 0.0

        val visualization = prepareVisualization(simulationParameters.cloud_size)
        val ui = prepareUI(visualization)

        root.add(ui, 1, 0)
        root.add(visualization.scene, 0, 0)

        mainStage.title = "skysim"
        mainStage.scene = scene
        mainStage.show()
    }

    fun test(vis: Visualizer) {
        val fakeGen = Generation()
        val n = Random.nextInt(2, 10)
        for (i in 0..n) {
            fakeGen.photons.add(Point3D(Random.nextDouble(-25.0, 25.0),
                    Random.nextDouble(-25.0, 25.0),
                    Random.nextDouble(-25.0, 25.0)))
        }
        vis.addGeneration(fakeGen)
        vis.showNextGeneration()
        println("'test' finished")
    }

    companion object {
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
