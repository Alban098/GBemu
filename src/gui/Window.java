package gui;

import core.GameBoy;
import core.GameBoyState;
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
    private final ConsoleLayer consoleLayer;
    private final PPULayer tileMapLayer;

    private boolean isSpacePressed = false;
    private boolean isFPressed = false;

    private Texture screen_texture;
    private Texture[] tileMaps_textures;
    private Texture[] tileTables_textures;

    private final GameBoy gameBoy;


    public Window(GameBoy gameBoy) {
        cpuLayer = new CPULayer();
        gameRendererLayer = new GameRendererLayer();
        memoryLayer = new MemoryLayer();
        serialOutputLayer = new SerialOutputLayer();
        consoleLayer = new ConsoleLayer();
        tileMapLayer = new PPULayer();

        this.gameBoy = gameBoy;
    }

    public void init() {
        initWindow();
        initImGui();
        imGuiGlfw.init(windowPtr, true);
        imGuiGl3.init(glslVersion);
        screen_texture = new Texture(PPU.SCREEN_WIDTH, PPU.SCREEN_HEIGHT, gameBoy.getPpu().getScreenBuffer());
        tileMaps_textures = new Texture[]{
                new Texture(256, 256, gameBoy.getPpu().getTileMaps()[0]),
                new Texture(256, 256, gameBoy.getPpu().getTileMaps()[1])
        };
        tileTables_textures = new Texture[]{
                new Texture(128, 64, gameBoy.getPpu().getTileTables()[0]),
                new Texture(128, 64, gameBoy.getPpu().getTileTables()[1]),
                new Texture(128, 64, gameBoy.getPpu().getTileTables()[2])
        };
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
            if (GameBoy.DEBUG) {
                if (gameBoy.getState() == GameBoyState.RUNNING)
                    gameBoy.executeFrame();
                if (gameBoy.getState() == GameBoyState.DEBUG) {
                    if (glfwGetKey(windowPtr, GLFW_KEY_SPACE) == GLFW_PRESS && !isSpacePressed) {
                        gameBoy.executeInstructions(1, true);
                        isSpacePressed = true;
                    }
                    if (glfwGetKey(windowPtr, GLFW_KEY_SPACE) == GLFW_RELEASE && isSpacePressed)
                        isSpacePressed = false;
                    if (glfwGetKey(windowPtr, GLFW_KEY_F) == GLFW_PRESS && !isFPressed) {
                        gameBoy.forceFrame();
                        isFPressed = true;
                    }
                    if (glfwGetKey(windowPtr, GLFW_KEY_F) == GLFW_RELEASE && isFPressed)
                        isFPressed = false;
                    if (glfwGetKey(windowPtr, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS)
                        gameBoy.executeInstructions(1000, false);
                }
            } else {
                gameBoy.executeFrame();
            }

            glClearColor(0.1f, 0.09f, 0.1f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);
            screen_texture.load(gameBoy.getPpu().getScreenBuffer());


            imGuiGlfw.newFrame();
            ImGui.newFrame();
            gameRendererLayer.imgui(screen_texture);

            if (GameBoy.DEBUG) {
                tileTables_textures[0].load(gameBoy.getPpu().getTileTables()[0]);
                tileTables_textures[1].load(gameBoy.getPpu().getTileTables()[1]);
                tileTables_textures[2].load(gameBoy.getPpu().getTileTables()[2]);
                tileMaps_textures[0].load(gameBoy.getPpu().getTileMaps()[0]);
                tileMaps_textures[1].load(gameBoy.getPpu().getTileMaps()[1]);
                cpuLayer.imgui(gameBoy);
                memoryLayer.imgui(gameBoy);
                serialOutputLayer.imgui(gameBoy);
                consoleLayer.imgui(gameBoy);
                tileMapLayer.imgui(tileTables_textures, tileMaps_textures);
            }

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