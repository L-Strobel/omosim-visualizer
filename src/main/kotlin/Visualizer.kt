package de.uniwuerzburg.omodvisualizer

import org.geotools.referencing.operation.matrix.MatrixFactory
import org.joml.Matrix3x2f
import org.joml.Matrix4f
import org.joml.Vector4f
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL20.*
import java.awt.Color
import java.io.File
import java.util.Vector
import kotlin.math.max
import kotlin.math.min
import kotlin.time.TimeSource

class Visualizer {
    private lateinit var window: Window
    private lateinit var agentRenderer: Renderer
    private lateinit var bgRenderer: Renderer
    private var aspect: Float = 1f
    private val vAgents: List<VisualAgent>
    private val timeSource = TimeSource.Monotonic
    private var lastTime = timeSource.markNow()
    private var simTime = 0.0
    private var positions: List<Pair<Float, Float>>
    private var speed = 10f // Speed-up compared to real time
    private val bBox: Array<Float>
    private var zoom = 1f
    private var up = 0f
    private var right = 0f

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
        val mesh = Mesh.basicCircle(20, Color.CYAN)
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

        // Control
        glfwSetKeyCallback(window.ref) { window: Long, key: Int, scancode: Int, action: Int, mods: Int ->
            val moveAction = (action == GLFW_REPEAT ) || (action == GLFW_PRESS )
            val moveStrength = 0.02f
            if (key == GLFW_KEY_W && moveAction) {
                up += moveStrength
            } else if (key == GLFW_KEY_A && moveAction) {
                right -= moveStrength
            } else if (key == GLFW_KEY_S && moveAction) {
                up -= moveStrength
            } else if (key == GLFW_KEY_D && moveAction) {
                right += moveStrength
            } else if (key == GLFW_KEY_E  && action == GLFW_RELEASE) {
                speed *= 2
            } else if (key == GLFW_KEY_R && action == GLFW_RELEASE)  {
                speed /= 2
            }
        }
        glfwSetScrollCallback(window.ref) {  window: Long, xoffset: Double, yoffset: Double ->
            zoom += - yoffset.toFloat() * 0.05f
            zoom = max(zoom, 0.05f)
            zoom = min(zoom, 3f)
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
        bgRenderer.close()
        agentRenderer.close()
        window.close()
        glfwTerminate()
        glfwSetErrorCallback(null)?.free()
    }

    private fun updateState(delta: Long) {
        simTime += delta / 1e9 * speed
        for (agent in vAgents) {
            agent.updatePosition(simTime)
        }
        positions = vAgents.map { it.x to it.y }
    }

    private fun render() {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        val projection = Matrix4f().ortho2D(-zoom + right, zoom + right, -zoom + up, zoom + up)

        // Plot background
        bgRenderer.render(projection, Matrix4f())

        val model = Matrix4f()
            .scale(0.002f * aspect, 0.002f, 1f)

        agentRenderer.renderInstanced(projection, model, positions )

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
