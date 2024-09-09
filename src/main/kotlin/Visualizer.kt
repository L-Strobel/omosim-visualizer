package de.uniwuerzburg.omodvisualizer

import de.uniwuerzburg.omod.io.json.OutputActivity
import org.joml.Matrix3x2f
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL20.*
import java.awt.Color
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.TimeSource

class Visualizer {
    private lateinit var window: Window
    private lateinit var renderer: Renderer
    private var aspect: Float = 1f
    private var anglePerc = 0f
    private val rng = Random()
    private val vAgents = load()
    private val nBalls = vAgents.size
    private val startPositions = List(nBalls) { rng.nextFloat()*2 - 1 to rng.nextFloat() * 2 - 1}
    private val timeSource = TimeSource.Monotonic
    private var lastTime = timeSource.markNow()


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
    }

    private fun loop() {
        while (!glfwWindowShouldClose(window.ref)) {
            val delta = getTimeDelta()
            updateState(delta)
            render()
            print("FPS: ${1e9 / delta}\r") // Print FPS
        }
    }

    private fun close() {
        renderer.close()
        window.close()
        glfwTerminate()
        glfwSetErrorCallback(null)?.free()
    }

    private fun updateState(delta: Long) {
        anglePerc += 1f * delta.toFloat() / (1e9f / 60f)
        anglePerc %= 100f
    }

    private fun render() {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        val firstLats = mutableListOf<Double>()
        val firstLons = mutableListOf<Double>()
        for (agent in vAgents) {
            val act = agent.legs.first() as OutputActivity
            firstLats.add(act.lat)
            firstLons.add(act.lon)
        }
        val bbBOX = arrayOf(firstLons.min(), firstLons.max(), firstLats.min(), firstLats.max())

        val model = Matrix3x2f()
            .scale(0.01f * aspect, 0.01f)

        val positions = firstLons.zip(firstLats).map { (x, y) ->
            val xAdj =  ((x - bbBOX[0]) / (bbBOX[1] - bbBOX[0]) *2 - 1).toFloat()
            val yAdj =  ((y - bbBOX[2]) / (bbBOX[3] - bbBOX[2]) *2 - 1).toFloat()
            xAdj to yAdj
        }
        //val positions = List(nBalls) { Pair(rng.nextFloat()*2 - 1, rng.nextFloat()*2 - 1)}

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
