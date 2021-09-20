package gui;

import core.GameBoy;
import core.ppu.PPU;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import openGL.Texture;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();

    private String glslVersion = null;
    private long windowPtr;
    private final CPULayer cpuLayer;
    private final GameRendererLayer gameRendererLayer;
    private final MemoryLayer memoryLayer;
    private final SerialOutputLayer serialOutputLayer;

    private boolean isSpacePressed = false;

    private Texture screen_texture;

    private final GameBoy gameBoy;


    public Window(GameBoy gameBoy) throws Exception {
        cpuLayer = new CPULayer();
        gameRendererLayer = new GameRendererLayer();
        memoryLayer = new MemoryLayer();
        serialOutputLayer = new SerialOutputLayer();

        this.gameBoy = gameBoy;
    }

    public void init() throws Exception {
        initWindow();
        initImGui();
        imGuiGlfw.init(windowPtr, true);
        imGuiGl3.init(glslVersion);
        screen_texture = new Texture(PPU.SCREEN_WIDTH, PPU.SCREEN_HEIGHT, gameBoy.getPpu().getScreenBuffer());
    }

    public void destroy() {
        imGuiGl3.dispose();
        imGuiGlfw.dispose();
        ImGui.destroyContext();
        Callbacks.glfwFreeCallbacks(windowPtr);
        glfwDestroyWindow(windowPtr);
        glfwTerminate();
    }

    private void initWindow() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() ) {
            System.out.println("Unable to initialize GLFW");
            System.exit(-1);
        }

        glslVersion = "#version 130";
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0);

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        windowPtr = glfwCreateWindow(1920, 1080, "My Window", NULL, NULL);

        if (windowPtr == NULL) {
            System.out.println("Unable to create window");
            System.exit(-1);
        }

        glfwMakeContextCurrent(windowPtr);
        glfwSwapInterval(1);
        glfwShowWindow(windowPtr);

        GL.createCapabilities();
    }

    private void initImGui() {
        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
    }

    public void run() {
        while (!glfwWindowShouldClose(windowPtr)) {

            if (glfwGetKey(windowPtr, GLFW_KEY_SPACE) == GLFW_PRESS && !isSpacePressed) {
                gameBoy.executeInstruction(1);
                isSpacePressed = true;
            }
            if (glfwGetKey(windowPtr, GLFW_KEY_SPACE) == GLFW_RELEASE && isSpacePressed)
                isSpacePressed = false;
            if(glfwGetKey(windowPtr, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS)
                gameBoy.executeInstruction(1000);

            glClearColor(0.1f, 0.09f, 0.1f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);
            screen_texture.load(gameBoy.getPpu().getScreenBuffer());

            imGuiGlfw.newFrame();
            ImGui.newFrame();
            gameRendererLayer.imgui(screen_texture);
            cpuLayer.imgui(gameBoy);
            memoryLayer.imgui(gameBoy);
            serialOutputLayer.imgui(gameBoy);

            ImGui.render();
            imGuiGl3.renderDrawData(ImGui.getDrawData());

            if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
                final long backupWindowPtr = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
                ImGui.updatePlatformWindows();
                ImGui.renderPlatformWindowsDefault();
                GLFW.glfwMakeContextCurrent(backupWindowPtr);
            }

            GLFW.glfwSwapBuffers(windowPtr);
            GLFW.glfwPollEvents();
        }
    }
}