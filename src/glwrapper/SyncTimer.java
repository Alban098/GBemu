package glwrapper;

import console.Console;
import console.LogLevel;
import org.lwjgl.glfw.GLFW;

public class SyncTimer {

    private double timeThen;
    private boolean enabled = true;

    public SyncTimer() {
        timeThen = GLFW.glfwGetTime();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enable) {
        enabled = enable;
    }

    public void sync(double fps) {
        double time = GLFW.glfwGetTime();
        try {
            if (enabled) {
                double gapTo = 1.0 / fps + timeThen;
                while (gapTo < time)
                    gapTo = 1.0 / fps + gapTo;
                while (gapTo > time) {
                    Thread.sleep(1);
                    time = GLFW.glfwGetTime();
                }
                timeThen = gapTo;
            } else {
                while (timeThen < time)
                    timeThen = 1.0 / fps + timeThen;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Console.getInstance().log(LogLevel.ERROR, "Sync timer crashed : " + e.getMessage());
        }

    }
}