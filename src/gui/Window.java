package gui;

import core.GameBoy;
import core.GameBoyState;
import core.input.Button;
import core.input.State;
import core.ppu.PPU;
import debug.DebuggerMode;
import debug.Logger;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.extension.implot.ImPlot;
import imgui.extension.implot.ImPlotContext;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImBoolean;
import openGL.Texture;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();

    private String glslVersion = null;
    private long windowPtr;
    private final CPULayer cpuLayer;
    private final MemoryLayer memoryLayer;
    private final SerialOutputLayer serialOutputLayer;
    private final ConsoleLayer consoleLayer;
    private final PPULayer ppuLayer;
    private final APULayer apuLayer;
    private final SettingsLayer settingsLayer;

    private boolean isSpacePressed = false;
    private boolean isFPressed = false;

    private Texture screen_texture;

    private final GameBoy gameboy;
    private ImPlotContext plotCtx;

    private final ImBoolean cpuLayerVisible = new ImBoolean(false);
    private final ImBoolean memoryLayerVisible = new ImBoolean(false);
    private final ImBoolean ppuLayerVisible = new ImBoolean(false);
    private final ImBoolean apuLayerVisible = new ImBoolean(false);
    private final ImBoolean serialOutputLayerVisible = new ImBoolean(false);
    private final ImBoolean consoleLayerVisible = new ImBoolean(false);

    private final ImBoolean debug = new ImBoolean(false);

    public Window(GameBoy gameboy) {
        cpuLayer = new CPULayer(gameboy.getDebugger());
        memoryLayer = new MemoryLayer(gameboy.getDebugger());
        serialOutputLayer = new SerialOutputLayer(gameboy.getDebugger());
        consoleLayer = new ConsoleLayer(gameboy.getDebugger());
        ppuLayer = new PPULayer(gameboy.getDebugger());
        apuLayer = new APULayer(gameboy.getDebugger());
        settingsLayer = new SettingsLayer(gameboy.getDebugger());

        this.gameboy = gameboy;
    }

    public void init() {
        initWindow();
        initImGui();
        imGuiGlfw.init(windowPtr, true);
        imGuiGl3.init(glslVersion);
        screen_texture = new Texture(PPU.SCREEN_WIDTH, PPU.SCREEN_HEIGHT);
        ppuLayer.initTextures();
    }

    public void destroy() {
        imGuiGl3.dispose();
        imGuiGlfw.dispose();
        screen_texture.cleanUp();
        ppuLayer.cleanUp();
        ImPlot.destroyContext(plotCtx);
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
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        windowPtr = glfwCreateWindow(160*3, 144*3+10, "GBemu", NULL, NULL);

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
        plotCtx = ImPlot.createContext();
        ImGuiIO io = ImGui.getIO();
        io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
    }

    public void run() {
        while (!glfwWindowShouldClose(windowPtr)) {
            glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);

            imGuiGlfw.newFrame();
            ImGui.newFrame();

            handleInput();
            tickEmulator();
            renderMenuBar();
            renderGameScreen();
            renderDebugLayers();

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

    private void tickEmulator() {
        if (gameboy.hasCartridge()) {
            if (gameboy.isDebuggerHooked(DebuggerMode.CPU)) {
                if (gameboy.getState() == GameBoyState.RUNNING)
                    gameboy.executeFrames(1);
                if (gameboy.getState() == GameBoyState.DEBUG) {
                    if (glfwGetKey(windowPtr, GLFW_KEY_SPACE) == GLFW_PRESS && !isSpacePressed) {
                        gameboy.executeInstructions(1, true);
                        isSpacePressed = true;
                    }
                    if (glfwGetKey(windowPtr, GLFW_KEY_SPACE) == GLFW_RELEASE && isSpacePressed)
                        isSpacePressed = false;
                    if (glfwGetKey(windowPtr, GLFW_KEY_F) == GLFW_PRESS && !isFPressed) {
                        gameboy.forceFrame();
                        isFPressed = true;
                    }
                    if (glfwGetKey(windowPtr, GLFW_KEY_F) == GLFW_RELEASE && isFPressed)
                        isFPressed = false;
                    if (glfwGetKey(windowPtr, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS)
                        gameboy.executeInstructions(1000, false);
                }
            } else {
                gameboy.executeFrames(1);
            }
        }
    }

    private void renderDebugLayers() {
        if (ppuLayer.isVisible()) {
            ppuLayer.setCgbMode(gameboy.mode == GameBoy.Mode.CGB);
            ppuLayer.render();
        }

        if (apuLayer.isVisible())
            apuLayer.render();

        if (cpuLayer.isVisible())
            cpuLayer.render();

        if (memoryLayer.isVisible())
            memoryLayer.render();

        if (serialOutputLayer.isVisible())
            serialOutputLayer.render();

        if (consoleLayer.isVisible())
            consoleLayer.render();

        if (settingsLayer.isVisible())
            settingsLayer.render();
    }

    private void renderGameScreen() {
        screen_texture.load(gameboy.getPpu().getScreenBuffer());

        glEnable(GL_TEXTURE_2D);
        screen_texture.bind();

        glLoadIdentity();

        glBegin(GL_QUADS);
        glTexCoord2f(0, 1); glVertex2f(-1,-1);
        glTexCoord2f(0, 0); glVertex2f(-1,1);
        glTexCoord2f(1, 0); glVertex2f(1,1);
        glTexCoord2f(1, 1); glVertex2f(1,-1);

        glDisable(GL_TEXTURE_2D);
        screen_texture.unbind();

        glEnd();
    }

    private void renderMenuBar() {
        ImGui.beginMainMenuBar();
        if (ImGui.beginMenu("File")) {
            if(ImGui.menuItem("Load ROM")) {
                JFileChooser chooser = new JFileChooser();
                FileNameExtensionFilter filter = new FileNameExtensionFilter(
                        "GameBoy ROM (.gb, .gbc)", "gb", ".gb", "gbc", ".gbc");
                chooser.setFileFilter(filter);
                int returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    try {
                        gameboy.insertCartridge(chooser.getSelectedFile().getAbsolutePath());
                        gameboy.setState(GameBoyState.RUNNING);
                    } catch (Exception e) {
                        Logger.log(Logger.Type.ERROR, "Invalid file : " + e.getMessage());
                    }

                }
            }
            ImGui.separator();
            if (gameboy.hasCartridge()) {
                switch (gameboy.getState()) {
                    case RUNNING -> {
                        if (ImGui.menuItem("Pause"))
                            gameboy.setState(GameBoyState.PAUSED);
                    }
                    case PAUSED, DEBUG -> {
                        if (ImGui.menuItem("Run"))
                            gameboy.setState(GameBoyState.RUNNING);
                    }
                }
            }
            if (ImGui.menuItem("Reset"))
                gameboy.reset();
            ImGui.endMenu();
        }

        if (ImGui.beginMenu("Debug")) {
            if (ImGui.checkbox("Debug features", debug))
                gameboy.getDebugger().setEnabled(debug.get());
            if (debug.get()) {
                ImGui.separator();
                if (ImGui.checkbox("CPU Inspector", cpuLayerVisible))
                    cpuLayer.setVisible(cpuLayerVisible.get());
                if (ImGui.checkbox("Memory Inspector", memoryLayerVisible))
                    memoryLayer.setVisible(memoryLayerVisible.get());
                if (ImGui.checkbox("PPU Inspector", ppuLayerVisible))
                    ppuLayer.setVisible(ppuLayerVisible.get());
                if (ImGui.checkbox("APU Inspector", apuLayerVisible))
                    apuLayer.setVisible(apuLayerVisible.get());
                ImGui.separator();
                if (ImGui.checkbox("Serial Output", serialOutputLayerVisible))
                    serialOutputLayer.setVisible(serialOutputLayerVisible.get());
                if (ImGui.checkbox("Console", consoleLayerVisible))
                    consoleLayer.setVisible(consoleLayerVisible.get());
            }

            gameboy.getDebugger().setHooked(DebuggerMode.CPU, cpuLayerVisible.get());
            gameboy.getDebugger().setHooked(DebuggerMode.MEMORY, memoryLayerVisible.get());
            gameboy.getDebugger().setHooked(DebuggerMode.PPU, ppuLayerVisible.get());
            gameboy.getDebugger().setHooked(DebuggerMode.APU, apuLayerVisible.get());
            gameboy.getDebugger().setHooked(DebuggerMode.CONSOLE, consoleLayerVisible.get());
            gameboy.getDebugger().setHooked(DebuggerMode.SERIAL, serialOutputLayerVisible.get());
            ImGui.endMenu();
        }

        if (ImGui.beginMenu("Settings")) {
            settingsLayer.setVisible(!settingsLayer.isVisible());
            ImGui.endMenu();
        }

        ImGui.endMainMenuBar();
    }


    private void handleInput() {
        if (glfwGetKey(windowPtr, GLFW_KEY_W) == GLFW_PRESS)
            gameboy.setButtonState(Button.UP, State.PRESSED);
        else
            gameboy.setButtonState(Button.UP, State.RELEASED);

        if (glfwGetKey(windowPtr, GLFW_KEY_S) == GLFW_PRESS)
            gameboy.setButtonState(Button.DOWN, State.PRESSED);
        else
            gameboy.setButtonState(Button.DOWN, State.RELEASED);

        if (glfwGetKey(windowPtr, GLFW_KEY_A) == GLFW_PRESS)
            gameboy.setButtonState(Button.LEFT, State.PRESSED);
        else
            gameboy.setButtonState(Button.LEFT, State.RELEASED);

        if (glfwGetKey(windowPtr, GLFW_KEY_D) == GLFW_PRESS)
            gameboy.setButtonState(Button.RIGHT, State.PRESSED);
        else
            gameboy.setButtonState(Button.RIGHT, State.RELEASED);

        if (glfwGetKey(windowPtr, GLFW_KEY_I) == GLFW_PRESS)
            gameboy.setButtonState(Button.A, State.PRESSED);
        else
            gameboy.setButtonState(Button.A, State.RELEASED);

        if (glfwGetKey(windowPtr, GLFW_KEY_O) == GLFW_PRESS)
            gameboy.setButtonState(Button.B, State.PRESSED);
        else
            gameboy.setButtonState(Button.B, State.RELEASED);

        if (glfwGetKey(windowPtr, GLFW_KEY_K) == GLFW_PRESS)
            gameboy.setButtonState(Button.START, State.PRESSED);
        else
            gameboy.setButtonState(Button.START, State.RELEASED);

        if (glfwGetKey(windowPtr, GLFW_KEY_L) == GLFW_PRESS)
            gameboy.setButtonState(Button.SELECT, State.PRESSED);
        else
            gameboy.setButtonState(Button.SELECT, State.RELEASED);
    }
}