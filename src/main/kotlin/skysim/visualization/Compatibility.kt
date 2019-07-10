package skysim.visualization

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import skysim.sky.skysim

fun composeArgs(param: SimulationParameters): ArrayList<String> {
    val defaultValues = SimulationParameters()

    val args = arrayListOf("visualization")
    //TODO : All these 'if' are not necessary, however now simulation fails
    //parse some arguments, so this helps to automativally include only changed(testing) arguments
    if (param.dynamic_plot.get() != defaultValues.dynamic_plot.get()) {
        args.add("--dynamic-plot")
    }
    if (param.cell_length.get() != defaultValues.cell_length.get()) {
        args.add("--cell-length")
        args.add(param.cell_length.get())
    }

    println("There is a bug! --cloud-size breaks simulator")
    if (param.cloud_size.get() != defaultValues.cloud_size.get()) {
        args.add("--cloud-size")
        args.add(param.cloud_size.get())
    }

    if (param.field_magnitude.get() != defaultValues.field_magnitude.get()) {
        args.add("--field-magnitude")
        args.add(param.field_magnitude.get())
    }
    if (param.free_path.get() != defaultValues.free_path.get()) {
        args.add("--free-path")
        args.add(param.free_path.get())
    }
    if (param.gain.get() != defaultValues.gain.get()) {
        args.add("--gain")
        args.add(param.gain.get())
    }
    if (param.particle_limit.get() != defaultValues.particle_limit.get()) {
        args.add("--particle-limit")
        args.add(param.particle_limit.get())
    }
    if (param.output.get() != defaultValues.output.get()) {
        args.add("--output")
        args.add(param.output.get())
    }
    if (param.seed.get() != defaultValues.seed.get()) {
        args.add("--seed")
        args.add(param.seed.get())
    }
    if (param.save_plot.get() != defaultValues.save_plot.get()) {
        args.add("--save-plot")
        args.add(param.save_plot.get())
    }
    if (param.seed_photons.get() != defaultValues.seed_photons.get()) {
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