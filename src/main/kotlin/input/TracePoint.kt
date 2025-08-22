package de.uniwuerzburg.omosimvisualizer.input

import de.uniwuerzburg.omosim.core.models.ActivityType

class TracePoint (
    val start: Double,
    val stop: Double,
    val x: Float,
    val y: Float,
    val activity: ActivityType?
)
