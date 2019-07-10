package skysim.visualization

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import skysim.sky.skysim
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun composeArgs(param: SimulationParameters): ArrayList<String> {
    val args = arrayListOf("visualization")
    //parse some arguments, so this helps to automativally include only changed(testing) arguments
    if (param.dynamic_plot.get()) {
        args.add("--dynamic-plot")
    }

    //Don't know default parameter yet
    //args.add("--cell-length")
    //args.add(param.cell_length.get())

    //TODO : "There is a bug! --cloud-size breaks simulator")
    //args.add("--cloud-size")
    //args.add(param.cloud_size.get())

    //args.add("--field-magnitude")
    //args.add(param.field_magnitude.get())

    //Don't know default parameter yet
    //args.add("--free-path")
    //args.add(param.free_path.get())

    //Don't know default parameter yet
    //args.add("--gain")
    //args.add(param.gain.get())


    args.add("--particle-limit")
    args.add(param.particle_limit.get())


    if (param.output.get() != "") {
        args.add("--output")
        args.add(param.output.get())
    }

    if (param.seed.get() != "") {
        args.add("--seed")
        args.add(param.seed.get())
    }

    if (param.save_plot.get() != "") {
        args.add("--save-plot")
        args.add(param.save_plot.get())
    }

    if (param.seed_photons.get() != "") {
        args.add("--seed-photons")
        args.add(param.seed_photons.get())
    }
    println(args)

    return args
}

fun launchSkysim(args: SimulationParameters): Channel<Generation> {
    val channel = Channel<Generation>()
    GlobalScope.launch { // launch a new coroutine in background and continue
        skysim(composeArgs(args).toTypedArray(), channel)
    }
    return channel
}