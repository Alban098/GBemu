package glwrapper;

import console.Console;
import console.LogLevel;
import org.lwjgl.glfw.GLFW;

/**
 * This class represent a Timer in charge of keeping the emulator locked at a consistent maximum framerate
 */
public class SyncTimer {

    private double timeThen;

    /**
     * Create a new instance of SyncTimer
     */
    public SyncTimer() {
        timeThen = GLFW.glfwGetTime();
    }

    /**
     * Wait until the next frame is ready to be computed
     * @param fps the required FPS
     */
    public void sync(double fps) {
        double time = GLFW.glfwGetTime();
        try {
            double gapTo = 1.0 / fps + timeThen;
            while (gapTo < time)
                gapTo = 1.0 / fps + gapTo;
            while (gapTo > time) {
                Thread.sleep(1);
                time = GLFW.glfwGetTime();
            }
            timeThen = gapTo;
        } catch (Exception e) {
            e.printStackTrace();
            Console.getInstance().log(LogLevel.ERROR, "Sync timer crashed : " + e.getMessage());
        }
    }
}