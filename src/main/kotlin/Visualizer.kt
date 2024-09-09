package de.uniwuerzburg.omodvisualizer

import de.uniwuerzburg.omod.io.json.OutputActivity
import de.uniwuerzburg.omod.io.json.OutputTrip
import org.joml.Matrix3x2f
import org.locationtech.jts.geom.Coordinate
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL20.*
import us.dustinj.timezonemap.containsInclusive
import java.awt.Color
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.TimeSource

class Visualizer {
    private lateinit var window: Window
    private lateinit var renderer: Renderer
    private var aspect: Float = 1f
    private val vAgents = VisualAgent.fromFile(File(("debugIn/bayreuth_smallTest.json")))
    private val nBalls = vAgents.size
    private val timeSource = TimeSource.Monotonic
    private var lastTime = timeSource.markNow()
    private var totalTime = 0L
    private var positions = List(nBalls) { 0f to 0f }
    private lateinit var bbox: Array<Float>

    fun run() {
        init()
        loop()
        close()
    }

    private fun init() {
        // Init window
        window = Window(1600,900, "")
        aspect = window.getAspect()

        // Init GL
        GL.createCapabilities() // Creates the GLCapabilities instance and makes the OpenGL bindings available for use.

        // GL Features
        // glEnable(GL_BLEND)
        // glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glClearColor(232f / 255f, 190f / 255f, 136f / 255f, 1.0f) // Set the clear color

        // Init renderers
        val mesh = Mesh.basicCircle(10, Color.darkGray)
        renderer = Renderer(mesh, nBalls)

        // Start timer
        lastTime = timeSource.markNow()
        totalTime = 0L
    }

    private fun loop() {
        while (!glfwWindowShouldClose(window.ref)) {
            val delta = getTimeDelta()
            updateState(delta)
            render()
            print("FPS: ${1e9 / delta}, Time: ${totalTime / 1e9  * 100}\r") // Print FPS
        }
    }

    private fun close() {
        renderer.close()
        window.close()
        glfwTerminate()
        glfwSetErrorCallback(null)?.free()
    }

    private fun updateState(delta: Long) {
        totalTime += delta
        val simMinute = totalTime / 1e9 * 100
        for (agent in vAgents) {
            agent.updatePosition(simMinute)
        }
        positions = vAgents.map { it.x to it.y }
    }

    private fun render() {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        val model = Matrix3x2f()
        //    .scale(1f * aspect, 1f)
            .scale(0.01f * aspect, 0.01f)

        renderer.render( model, positions )

        /*for (i in 0 until nBalls) {
            val model = Matrix3x2f()
                .translate(r * cos(angle) + startPositions[i].first, r * sin(angle) + startPositions[i].second)
                .scale(0.01f * aspect, 0.01f)
            renderer.render( model )
        }*/

        glfwSwapBuffers(window.ref) // swap the color buffers
        glfwPollEvents() // Poll for window events, like keystrokes.
    }

    private fun getTimeDelta() : Long {
        val now = timeSource.markNow()
        val delta = (now - lastTime).inWholeNanoseconds
        lastTime = now
        return delta
    }
}
