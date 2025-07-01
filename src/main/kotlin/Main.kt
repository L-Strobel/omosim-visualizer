package de.uniwuerzburg.omodvisualizer

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file

class Run : CliktCommand() {
    // Arguments
    private val omod_file by argument(
        help = "Path to omod output. Must be a JSON file."
    ).file(mustExist = true, mustBeReadable = true)
    private val osm_file by argument(
        help = "Path to an osm.pbf file that covers the area completely. " +
               "Recommended download platform: https://download.geofabrik.de/"
    ).file(mustExist = true, mustBeReadable = true)

    override fun run() {
        Visualizer(omod_file, osm_file).run()
    }
}

fun main(args: Array<String>) = Run().main(args)