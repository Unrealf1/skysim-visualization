package skysim.visualization

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import skysim.sky.skysim

fun composeArgs(param: SimulationParameters): ArrayList<String> {
    val args = arrayListOf("visualization")
    if (param.dynamic_plot != null && param.dynamic_plot!!) {
        args.add("--dynamic-plot")
    }
    if (param.cell_length != null) {
        args.add("--cell-length")
        args.add(param.cell_length.toString())
    }

    println("There is a bug! --cloud-size breaks simulator")
    //args.add("--cloud-size")
    //args.add(param.cloud_size.toString())

    if (param.field_magnitude != null) {
        args.add("--field-magnitude")
        args.add(param.field_magnitude.toString())
    }
    if (param.free_path != null) {
        args.add("--free-path")
        args.add(param.free_path.toString())
    }
    if (param.gain != null) {
        args.add("--gain")
        args.add(param.gain.toString())
    }
    if (param.particle_limit != null) {
        args.add("--particle-limit")
        args.add(param.particle_limit.toString())
    }
    if (param.output != null) {
        args.add("--output")
        args.add(param.output!!)
    }
    if (param.seed != null) {
        args.add("--seed")
        args.add(param.seed.toString())
    }
    if (param.save_plot != null) {
        args.add("--save-plot")
        args.add(param.save_plot!!)
    }
    if (param.seed_photons != null) {
        args.add("--seed-photons")
        args.add(param.seed_photons!!)
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