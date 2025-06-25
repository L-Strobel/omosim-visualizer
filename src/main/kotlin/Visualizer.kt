package de.uniwuerzburg.omodvisualizer

import de.uniwuerzburg.omod.core.models.ActivityType
import de.uniwuerzburg.omodvisualizer.graphic.Renderer
import de.uniwuerzburg.omodvisualizer.graphic.addCircleMesh
import de.uniwuerzburg.omodvisualizer.input.BackgroundReader
import de.uniwuerzburg.omodvisualizer.input.CoordTransformer
import de.uniwuerzburg.omodvisualizer.input.VisualAgent
import de.uniwuerzburg.omodvisualizer.theme.ThemeColors
import org.joml.Matrix4f
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL30.*
import java.io.File
import kotlin.time.TimeSource


class Visualizer {
    private lateinit var window: Window
    private var aspect: Float = 1f
    private lateinit var vAgents: List<VisualAgent>
    private val timeSource = TimeSource.Monotonic
    private var lastTime = timeSource.markNow()
    private var simTime = 0.0
    private lateinit var positions: Map<ActivityType?, List<Pair<Float, Float>>>
    private lateinit var transformer: CoordTransformer
    private lateinit var bBox: Array<Float>

    // Renderer
    private lateinit var ui: UI
    private lateinit var agentRenderers: MutableMap<ActivityType?, Renderer>
    private lateinit var bgRenderer: Renderer

    fun run() {
        init()
        loop()
        close()
    }

    private fun init() {
        // Init window
        window = Window("")
        aspect = window.getAspect()

        val (agents, t, b) = VisualAgent.fromFile(File(("debugIn/basicTest.json")), 7000, aspect)
        vAgents = agents
        transformer = t
        bBox = b
        positions = mapOf()

        // Init GL
        GL.createCapabilities() // Creates the GLCapabilities instance and makes the OpenGL bindings available for use.

        // GL Features
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glClearColor(0.15f , 0.15f, 0.15f, 1.0f)

        Controls.registerControls(window)

        // Init renderers
        agentRenderers = mutableMapOf<ActivityType?, Renderer>()
        for (activity in ActivityType.entries) {
            val color = ThemeColors.of(activity)
            agentRenderers[activity] = Renderer(instances = vAgents.size).addCircleMesh(color)
        }
        agentRenderers[null] = Renderer(instances = vAgents.size).addCircleMesh(ThemeColors.of(null))

        // Read background data
        bgRenderer = BackgroundReader.getOSM(
            File("C:/Users/les29rq/open_data/OSM/bayern-latest.osm.pbf"),
            bBox[0].toDouble() - 0.05,
            bBox[1].toDouble() + 0.05,
            bBox[2].toDouble() - 0.1,
            bBox[3].toDouble() + 0.1,
            transformer
        )

        ui = UI(window)
        for (button in ui.buttons) {
            Controls.registerButtons(button)
        }

        // Start timer
        lastTime = timeSource.markNow()
        simTime = 0.0
    }

    private fun loop() {
        while (!glfwWindowShouldClose(window.ref)) {
            val delta = getTimeDelta()
            updateState(delta)
            render()
            print("FPS: ${1e9 / delta}, Time: ${simTime}\r") // Print FPS
        }
    }

    private fun close() {
        ui.close()
        bgRenderer.close()
        agentRenderers.forEach{ (_, v) -> v.close()}
        window.close()
        glfwTerminate()
        glfwSetErrorCallback(null)?.free()
    }

    private fun updateState(delta: Long) {
        simTime += delta / 1e9 * Controls.getSpeed() * Controls.pause
        for (agent in vAgents) {
            agent.updatePosition(simTime)
        }
        positions =  vAgents.groupBy { it.activity }.mapValues { (_, v) -> v.map { it.x to it.y } }
    }

    private fun render() {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        val projection = Matrix4f().ortho2D(
            -Controls.zoom + Controls.right, Controls.zoom + Controls.right,
            -Controls.zoom + Controls.up, Controls.zoom + Controls.up
        )

        // vvv------- DRAW CALLS -------vvv

        bgRenderer.render(projection, Matrix4f())

        val model = Matrix4f()
            .scale(0.002f * aspect, 0.002f, 1f)

        for ((k, v) in positions.entries) {
            if (!Controls.disabled[k]!!) {
                agentRenderers[k]!!.renderInstanced(projection, model, v )
            }
        }

        ui.render(simTime)

        // ^^^------- DRAW CALLS -------^^^

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
