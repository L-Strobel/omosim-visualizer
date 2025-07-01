package de.uniwuerzburg.omodvisualizer

import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import java.nio.IntBuffer


class Window (title: String) {
    val ref: Long

    init {
        GLFWErrorCallback.createPrint(System.err).set() // Setup an error callback.

        // Initialize GLFW
        check(glfwInit()) { "Unable to initialize GLFW" }

        // Configure GLFW
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)
        glfwWindowHint(GLFW_SAMPLES, 8)

        // Get the resolution of the primary monitor
        val vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor())!!

        // Create the window
        ref = glfwCreateWindow(vidMode.width(), vidMode.height(), title, glfwGetPrimaryMonitor(), NULL)
        if (ref == NULL) throw RuntimeException("Failed to create the GLFW window")

        // Make the OpenGL context current
        glfwMakeContextCurrent(ref)

        // Enable v-sync, 1 -> Enabled
        glfwSwapInterval(0)

        // Aspect ratio and size
        glfwSetWindowAspectRatio(ref, vidMode.width(), vidMode.height())
        glfwSetWindowSizeLimits(
            ref,
            vidMode.width(), vidMode.height(),
            GLFW_DONT_CARE, GLFW_DONT_CARE
        )

        // Make the window visible
        glfwShowWindow(ref)
    }

    fun getCurrentWindowSize() : Pair<Int, Int> {
        val width: Int
        val height: Int

        stackPush().use { stack ->
            val pWidth: IntBuffer = stack.mallocInt(1) // int*
            val pHeight: IntBuffer = stack.mallocInt(1) // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(ref, pWidth, pHeight)

            width = pWidth[0]
            height = pHeight[0]
        }
        return Pair(width, height)
    }

    fun getAspect() : Float{
        val resolution = getCurrentWindowSize()
        return resolution.second.toFloat() / resolution.first.toFloat()
    }

    fun close() {
        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(ref)
        glfwDestroyWindow(ref)
    }
}

