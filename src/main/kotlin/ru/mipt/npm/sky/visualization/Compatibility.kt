package ru.mipt.npm.sky.visualization

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import ru.mipt.npm.sky.skysim

fun composeArgs(param: SimulationViewModel): ArrayList<String> {
    val args = arrayListOf("visualization")
    //parse some arguments, so this helps to automatically include only changed arguments
    if (param.dynamicPlot.get()) {
        args.add("--dynamic-plot")
    }

    args.add("--cell-length")
    args.add(param.cellLength.get().toString())

    args.add("--cloud-size")
    args.add(param.cloudSize.get().toString())

    args.add("--field-magnitude")
    args.add(param.fieldMagnitude.get().toString())

    args.add("--free-path")
    args.add(param.freePath.get().toString())

    //Don't know default parameter yet
    //args.add("--gain")
    //args.add(param.gain.get())

    args.add("--particle-limit")
    args.add(param.particleLimit.get().toString())


    if (param.output.get() != "") {
        args.add("--output")
        args.add(param.output.get())
    }

    if (param.seed.get() != -1) {
        args.add("--seed")
        args.add(param.seed.get().toString())
    }

    if (param.savePlot.get() != "") {
        args.add("--save-plot")
        args.add(param.savePlot.get())
    }

    if (param.seedPhotons.get() != "") {
        args.add("--seed-photons")
        args.add(param.seedPhotons.get())
    }
    println(args)

    return args
}

fun launchSkysim(args: SimulationViewModel): Channel<Generation> {
    val channel = Channel<Generation>()
    GlobalScope.launch {
        // launch a new coroutine in background and continue
        skysim(composeArgs(args).toTypedArray(), channel)
    }
    return channel
}