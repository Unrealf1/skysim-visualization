package ru.mipt.npm.sky.visualization

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.value.ObservableDoubleValue
import javafx.event.EventHandler
import javafx.scene.*
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.paint.Color
import javafx.scene.shape.Box
import javafx.scene.shape.DrawMode
import javafx.scene.transform.Rotate

/**
 * This class produces visualization via function "prepareVisualization"
 * and provides some control over visualization after
 * (camera for example)
 */
class Canvas3D(val model: SimulationViewModel, val width: ObservableDoubleValue, val height: ObservableDoubleValue) {

    val camera = PerspectiveCamera(true)

    private fun buildCamera(): Camera {
        camera.translateXProperty().set(0.0)
        camera.translateYProperty().set(0.0)
        camera.translateZProperty().set(model.cloudSize.value.toDouble() * -3.0)
        camera.farClip = 100000.0
        camera.nearClip = 1.0
        return camera
    }

    val boxField: Box = Box()

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

        val pressedHandler = { event: MouseEvent ->
            anchorX = event.sceneX
            anchorY = event.sceneY
            anchorAngleX = angleX.get()
            anchorAngleY = angleY.get()
        }
        scene.onMousePressed = EventHandler(pressedHandler)

        val draggedHandler = { event: MouseEvent ->
            angleX.set(anchorAngleX - (anchorY - event.sceneY))
            angleY.set(anchorAngleY + anchorX - event.sceneX)

        }
        scene.onMouseDragged = EventHandler(draggedHandler)
    }

    private fun prepareVisualization(): Visualizer {
        val root = Group()
        val scene = SubScene(
            root,
            width.get(),
            height.get(),
            false,
            SceneAntialiasing.DISABLED
        )
        scene.widthProperty().bind(width)
        scene.heightProperty().bind(height)
        scene.fill = Color.BLACK

        val fieldGroup = Group()
        root.children.add(fieldGroup)
        boxField.depthProperty().bind(model.cloudSize)
        boxField.heightProperty().bind(model.cloudSize)
        boxField.widthProperty().bind(model.cloudSize)
        boxField.drawMode = DrawMode.LINE
        boxField.visibleProperty().bind(model.showBox)
        fieldGroup.children.add(boxField)

        val photonsGroup = Group()
        fieldGroup.children.add(photonsGroup)

        scene.camera = buildCamera()

        initMouseControl(fieldGroup, scene)
        val scrollHandler = { event: ScrollEvent ->
            scene.camera.translateZProperty().set(scene.camera.translateZ + event.deltaY * 8.0)
        }
        scene.onScroll = EventHandler(scrollHandler)

        return Visualizer(scene, photonsGroup)
    }

    val visualizer by lazy { prepareVisualization() }
}