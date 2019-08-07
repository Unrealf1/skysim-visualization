package ru.mipt.npm.sky.visualization

import javafx.geometry.Point3D
import javafx.scene.Group
import javafx.scene.SubScene
import javafx.scene.shape.Sphere

class Generation(
    val photons: List<Point3D>,
    var averageHeight: Double = 0.0
)

/**
 * This class contains all generated generations, and is able
 * to switch between them.
 * This class is in charge of photons
 * appearing on the screen
 */
class Visualizer(val scene: SubScene, val photonGroup: Group) {
    private val generations = ArrayList<Generation>()
    private val photonRadius = 1.5
    private var currentGen = -1

    fun getCurrentGen(): Int {
        return currentGen
    }

    fun size(): Int {
        return generations.size - 1
    }

    fun clear() {
        currentGen = -1
        generations.clear()
        photonGroup.children.clear()
    }

    fun addGeneration(gen: Generation) {
        generations.add(gen)
    }

    private fun clearPhotonGroup() {
        photonGroup.children.clear()
    }

    fun setCurrentGen(value: Int): Boolean {
        if (value < 0 || value >= size()) {
            return false
        }
        currentGen = value
        showGeneration()
        return true
    }

    fun showNextGeneration(): Boolean {
        currentGen++
        if (currentGen >= size()) {
            currentGen--
            return false
        }
        showGeneration()
        return true
    }

    fun showPrevGeneration(): Boolean {
        currentGen--
        if (currentGen < 0) {
            currentGen++
            return false
        }
        showGeneration()
        return true
    }

    private fun showGeneration() {
        clearPhotonGroup()
        val gen = generations[currentGen]
        for (photon in gen.photons) {
            val visualPhoton = Sphere(photonRadius)
            visualPhoton.translateXProperty().set(photon.x)
            visualPhoton.translateYProperty().set(photon.y)
            visualPhoton.translateZProperty().set(photon.z)
            photonGroup.children.add(visualPhoton)
        }
    }
}
