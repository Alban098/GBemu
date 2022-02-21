package glwrapper;

import console.Console;
import console.LogLevel;
import org.lwjgl.glfw.GLFW;

/**
 * This class represent a Timer in charge of keeping the emulator locked at a consistent maximum framerate
 */
public class SyncTimer {

    private double last_time;

    /**
     * Create a new instance of SyncTimer
     */
    public SyncTimer() {
        last_time = GLFW.glfwGetTime();
    }

    /**
     * Wait until the next frame is ready to be computed
     * @param fps the required FPS
     */
    public void sync(double fps) {
        double time = GLFW.glfwGetTime();
        try {
            double gap = 1.0 / fps + last_time;
            while (gap < time)
                gap = 1.0 / fps + gap;
            while (gap > time) {
                Thread.sleep(1);
                time = GLFW.glfwGetTime();
            }
            last_time = gap;
        } catch (Exception e) {
            e.printStackTrace();
            Console.getInstance().log(LogLevel.ERROR, "Sync timer crashed : " + e.getMessage());
        }
    }
}