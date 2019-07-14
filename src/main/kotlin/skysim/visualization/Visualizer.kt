package skysim.visualization

import javafx.geometry.Point3D
import javafx.scene.Group
import javafx.scene.SubScene
import javafx.scene.shape.Sphere

class Generation(
        val photons: ArrayList<Point3D> = arrayListOf<Point3D>(),
        var averageHeight: Double = 0.0)

class Visualizer(val scene: SubScene, val  photonGroup: Group) {
    private val generations = arrayListOf<Generation>(Generation())
    private val photonRadius = 1.5
    private var current_gen = -1

    fun getCurrentGen(): Int {
        return current_gen
    }

    fun size(): Int {
        return generations.size - 1
    }

    fun clear() {
        current_gen = -1
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
        current_gen = value
        showGeneration()
        return true
    }

    fun showNextGeneration(): Boolean{
        current_gen++
        if (current_gen >= size()) {
            current_gen--
            return false
        }
        showGeneration()
        return true
    }

    fun showPrevGeneration(): Boolean {
        current_gen--
        if (current_gen < 0) {
            current_gen++
            return false
        }
        showGeneration()
        return true
    }

    private fun showGeneration() {
        clearPhotonGroup()
        val gen = generations[current_gen]
        for (photon in gen.photons) {
            val visual_photon = Sphere(photonRadius)
            visual_photon.translateXProperty().set(photon.x)
            visual_photon.translateYProperty().set(photon.y)
            visual_photon.translateZProperty().set(photon.z)
            photonGroup.children.add(visual_photon)
        }
    }
}
