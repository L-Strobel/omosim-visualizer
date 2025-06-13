package de.uniwuerzburg.omodvisualizer

import de.uniwuerzburg.omod.core.models.ActivityType
import org.joml.Matrix4f
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL30.*
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import java.awt.Color
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.time.TimeSource


class Visualizer {
    private lateinit var window: Window
    private lateinit var agentRenderers: MutableMap<ActivityType?, Renderer>
    private lateinit var bgRenderer: Renderer
    private var aspect: Float = 1f
    private lateinit var vAgents: List<VisualAgent>
    private val timeSource = TimeSource.Monotonic
    private var lastTime = timeSource.markNow()
    private var simTime = 0.0
    private lateinit var positions: Map<ActivityType?, List<Pair<Float, Float>>>
    private lateinit var transformer: CoordTransformer
    private lateinit var bBox: Array<Float>

    private lateinit var textureRenderer: Renderer

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
        // glEnable(GL_BLEND)
        // glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glClearColor(0.15f , 0.15f, 0.15f, 1.0f)

        // Init renderers

        agentRenderers = mutableMapOf<ActivityType?, Renderer>()
        for (activity in ActivityType.entries) {
            val color = when(activity) {
                ActivityType.HOME -> Color.CYAN
                ActivityType.WORK -> Color.RED
                else -> Color.YELLOW
            }
            val mesh = Mesh.basicCircle(20, color)
            agentRenderers[activity] = Renderer(mesh, vAgents.size)
        }
        val mesh = Mesh.basicCircle(20, Color.GREEN)
        agentRenderers[null] = Renderer(mesh, vAgents.size)


        // Read background data
        val bgMesh = BackgroundReader.getOSM(
            File("C:/Users/les29rq/open_data/OSM/bayern-latest.osm.pbf"),
            bBox[0].toDouble() - 0.05,
            bBox[1].toDouble() + 0.05,
            bBox[2].toDouble() - 0.1,
            bBox[3].toDouble() + 0.1,
            transformer
        )
        bgRenderer = Renderer(bgMesh, 1)
        textureRenderer = Renderer(Mesh.textureCanvas(), 1, "debugIn/TestTexture.png")

        Controls.registerControls(window)

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
        textureRenderer.close()
        bgRenderer.close()
        agentRenderers.forEach{ (k, v) -> v.close()}
        window.close()
        glfwTerminate()
        glfwSetErrorCallback(null)?.free()
    }

    private fun updateState(delta: Long) {
        simTime += delta / 1e9 * Controls.speed * Controls.pause
        for (agent in vAgents) {
            agent.updatePosition(simTime)
        }
        positions =  vAgents.groupBy { it.activity }.mapValues { (k, v) -> v.map { it.x to it.y } }
    }

    private fun render() {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        val projection = Matrix4f().ortho2D(
            -Controls.zoom + Controls.right, Controls.zoom + Controls.right,
            -Controls.zoom + Controls.up, Controls.zoom + Controls.up
        )

        // Plot background
        bgRenderer.render(projection, Matrix4f())
        //textureRenderer.renderBasic(projection, Matrix4f().scale(0.1f * aspect, 0.1f, 1f))

        val model = Matrix4f()
            .scale(0.002f * aspect, 0.002f, 1f)

        for ((k, v) in positions.entries) {
            agentRenderers[k]!!.renderInstanced(projection, model, v )
        }


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
