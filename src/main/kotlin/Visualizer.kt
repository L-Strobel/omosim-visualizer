package de.uniwuerzburg.omodvisualizer

import de.uniwuerzburg.omod.io.json.OutputActivity
import de.uniwuerzburg.omod.io.json.OutputTrip
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
import kotlin.time.Duration.Companion.microseconds
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
    private var totalTime = 0L
    private var positions = MutableList(nBalls) { 0f to 0f }
    private lateinit var bbBox: Array<Float>

    fun run() {
        initPos()
        init()
        loop()
        close()
    }

    fun initPos(){
        val firstLats = mutableListOf<Float>()
        val firstLons = mutableListOf<Float>()
        for (agent in vAgents) {
            val act = agent.legs.first() as OutputActivity
            firstLats.add(act.lat.toFloat())
            firstLons.add(act.lon.toFloat())
        }
        bbBox = arrayOf(firstLons.min(), firstLons.max(), firstLats.min(), firstLats.max())

        val lonsScaled = firstLons.map { scaleXToBB(it) }
        val latsScaled = firstLats.map { scaleYToBB(it) }

        positions = lonsScaled.zip(latsScaled).toMutableList()
    }

    private fun scaleXToBB(x: Float) : Float {
        return (x - bbBox[0]) / (bbBox[1] - bbBox[0]) *2 - 1
    }

    private fun scaleYToBB(y: Float) : Float {
        return (y -  bbBox[2]) / (bbBox[3] - bbBox[2]) *2 - 1
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
        for ((i, agent) in vAgents.withIndex()) {
            agent.timeInCurrentLeg += delta / 1e9 * 100
            val currentLeg = agent.legs.first() as OutputActivity
            var diff = agent.timeInCurrentLeg - currentLeg.stayTimeMinute!!
            if (diff>0) {
                agent.legs.removeFirst()
                agent.timeInCurrentLeg = diff

                while (agent.legs.first() is OutputTrip) {
                    agent.legs.removeFirst()
                }
                val nextActivity = agent.legs.first() as OutputActivity
                val x = scaleXToBB(nextActivity.lon.toFloat())
                val y = scaleYToBB(nextActivity.lat.toFloat())
                positions[i] = x to y
                //TODO while
            }

        }
    }

    private fun render() {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        val model = Matrix3x2f()
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
