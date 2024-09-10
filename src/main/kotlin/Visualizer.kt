package de.uniwuerzburg.omodvisualizer

import org.joml.Matrix3x2f
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL20.*
import java.awt.Color
import java.io.File
import kotlin.time.TimeSource

class Visualizer {
    private lateinit var window: Window
    private lateinit var agentRenderer: Renderer
    private lateinit var bgRenderer: Renderer
    private var aspect: Float = 1f
    private val vAgents: List<VisualAgent>
    private val timeSource = TimeSource.Monotonic
    private var lastTime = timeSource.markNow()
    private var totalTime = 0L
    private var positions: List<Pair<Float, Float>>
    private var speed = 20f // Speed-up compared to real time
    private val bBox: Array<Float>

    init {
        val (agents, bb) = VisualAgent.fromFile(File(("debugIn/bayreuth_smallTest.json")))
        vAgents = agents
        bBox = bb
        positions = List(vAgents.size) { 0f to 0f }
    }

    fun run() {
        init()
        loop()
        close()
    }

    private fun init() {
        // Init window
        window = Window(2560,1440, "")
        aspect = window.getAspect()

        // Init GL
        GL.createCapabilities() // Creates the GLCapabilities instance and makes the OpenGL bindings available for use.

        // GL Features
        // glEnable(GL_BLEND)
        // glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glClearColor(0.15f , 0.15f, 0.15f, 1.0f)

        // Init renderers
        val mesh = Mesh.basicCircle(5, Color.CYAN)
        agentRenderer = Renderer(mesh, vAgents.size)

        // Read background data
        val bgMesh = BackgroundReader.readOSM(
            File("C:/Users/les29rq/Nextcloud/Projekte/08_data/OSM/bayern-latest.osm.pbf"),
            bBox[0].toDouble(),
            bBox[1].toDouble(),
            bBox[2].toDouble(),
            bBox[3].toDouble()
        )
        bgRenderer = Renderer(bgMesh, 1)

        // Start timer
        lastTime = timeSource.markNow()
        totalTime = 0L
    }

    private fun loop() {
        while (!glfwWindowShouldClose(window.ref)) {
            val delta = getTimeDelta()
            updateState(delta)
            render()
            print("FPS: ${1e9 / delta}, Time: ${totalTime / 1e9  * speed}\r") // Print FPS
        }
    }

    private fun close() {
        bgRenderer.close()
        agentRenderer.close()
        window.close()
        glfwTerminate()
        glfwSetErrorCallback(null)?.free()
    }

    private fun updateState(delta: Long) {
        totalTime += delta
        val simMinute = totalTime / 1e9 * speed
        for (agent in vAgents) {
            agent.updatePosition(simMinute)
        }
        positions = vAgents.map { it.x to it.y }
    }

    private fun render() {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        // Plot background
        bgRenderer.render(Matrix3x2f())

        val model = Matrix3x2f()
            .scale(0.002f * aspect, 0.002f)

        agentRenderer.renderInstanced( model, positions )


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
