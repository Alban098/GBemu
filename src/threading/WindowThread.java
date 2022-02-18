package threading;

import console.Console;
import console.LogLevel;
import gbemu.core.GameBoy;
import gbemu.core.GameBoyState;
import gbemu.settings.Button;
import gbemu.core.input.InputState;
import gbemu.core.ppu.PPU;
import gbemu.extension.debug.DebuggerMode;
import glwrapper.SyncTimer;
import gui.debug.*;
import gui.std.Layer;
import gui.std.CheatsLayer;
import gui.std.SettingsLayer;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.extension.implot.ImPlot;
import imgui.extension.implot.ImPlotContext;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImBoolean;
import glwrapper.Texture;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.util.Objects;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * This class represent the Main rendering thread
 * containing the OpenGl Context
 */
public class WindowThread {

    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();

    private String glslVersion = null;
    private long windowPtr;
    private final DebugLayer cpuLayer;
    private final DebugLayer memoryLayer;
    private final DebugLayer serialOutputLayer;
    private final DebugLayer consoleLayer;
    private final DebugLayer ppuLayer;
    private final DebugLayer apuLayer;
    private final Layer settingsLayer;
    private final Layer cheatsLayer;

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
    private final GameBoyThread gameboyThread;
    private DebuggerThread debuggerThread;
    private ConsoleThread consoleThread;
    private final SyncTimer timer;
    private String currentDirectory = "./";


    /**
     * Create a new OpenGl Window
     * @param gameboy the Game Boy to render
     * @param gameBoyThread the Thread running the Game Boy
     */
    public WindowThread(GameBoy gameboy, GameBoyThread gameBoyThread) {
        cpuLayer = new CPULayer(gameboy.getDebugger(), gameBoyThread);
        memoryLayer = new MemoryLayer(gameboy.getDebugger());
        serialOutputLayer = new SerialOutputLayer(gameboy.getDebugger());
        consoleLayer = new ConsoleLayer(gameboy.getDebugger());
        ppuLayer = new PPULayer(gameboy.getDebugger());
        apuLayer = new APULayer(gameboy.getDebugger());
        settingsLayer = new SettingsLayer(gameboy.getSettingsContainer());
        cheatsLayer = new CheatsLayer(gameboy, gameboy.getCheatManager());
        timer = new SyncTimer();

        this.gameboy = gameboy;
        this.gameboyThread = gameBoyThread;
    }

    /**
     * Initialize the Window
     */
    public void init() {
        initWindow();
        initImGui();
        imGuiGlfw.init(windowPtr, true);
        imGuiGl3.init(glslVersion);
        screen_texture = new Texture(PPU.SCREEN_WIDTH, PPU.SCREEN_HEIGHT);
        ((PPULayer)ppuLayer).initTextures();
    }

    /**
     * Clean the window and kill every Thread attached
     */
    public void destroy() {
        imGuiGl3.dispose();
        imGuiGlfw.dispose();
        screen_texture.cleanUp();
        ((PPULayer)ppuLayer).cleanUp();
        if (consoleThread != null)
            consoleThread.kill();
        if (debuggerThread != null)
            debuggerThread.kill();
        ImPlot.destroyContext(plotCtx);
        ImGui.destroyContext();
        Callbacks.glfwFreeCallbacks(windowPtr);
        glfwDestroyWindow(windowPtr);
        glfwTerminate();
    }

    /**
     * Initialize OpenGl and GLFW
     */
    private void initWindow() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit() ) {
            System.exit(-1);
        }

        glslVersion = "#version 130";
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0);

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        windowPtr = glfwCreateWindow(160*3, 144*3+10, "GBemu", NULL, NULL);

        if (windowPtr == NULL) {
            System.exit(-1);
        }

        glfwMakeContextCurrent(windowPtr);
        GLFWImage logo = GLFWImage.malloc();
        GLFWImage.Buffer logoBuf = GLFWImage.malloc(1);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            logo.set(87, 87, Objects.requireNonNull(STBImage.stbi_load("logo.png", stack.mallocInt(1), stack.mallocInt(1), stack.mallocInt(1), 4)));
            logoBuf.put(0, logo);
        }
        glfwSetWindowIcon(windowPtr, logoBuf);
        glfwSwapInterval(1);
        glfwShowWindow(windowPtr);
        GL.createCapabilities();
    }

    /**
     * Initialize ImGui and ImPlot
     */
    private void initImGui() {
        ImGui.createContext();
        plotCtx = ImPlot.createContext();
        ImGuiIO io = ImGui.getIO();
        io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
    }

    /**
     * Execute the rendering Loop
     */
    public void run() {
        while (!glfwWindowShouldClose(windowPtr)) {
            //Clear the screen
            glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);

            //Start a new ImGui frame
            imGuiGlfw.newFrame();
            ImGui.newFrame();


            //Render Menu, emulation and layers
            handleInput();
            renderMenuBar();
            renderGameScreen();
            renderLayers();

            //Render the ImGui Frame
            ImGui.render();
            imGuiGl3.renderDrawData(ImGui.getDrawData());

            //Notify the Game Boy Thread to render a new Frame
            synchronized (gameboyThread) {
                gameboyThread.notify();
            }

            //Notify the Debugger Thread to compute a gbemu.extension.debug frame if enabled
            if (gameboy.getDebugger().isEnabled()) {
                synchronized (debuggerThread) {
                    debuggerThread.notify();
                }
            }

            //ImGui and GLFW standard call to render the frame
            if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
                final long backupWindowPtr = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
                ImGui.updatePlatformWindows();
                ImGui.renderPlatformWindowsDefault();
                GLFW.glfwMakeContextCurrent(backupWindowPtr);
            }
            GLFW.glfwSwapBuffers(windowPtr);
            GLFW.glfwPollEvents();
            timer.sync(60);
        }
    }

    /**
     * Render the ImGui layers that are visible
     */
    private void renderLayers() {
        if (ppuLayer.isVisible()) {
            ((PPULayer)ppuLayer).setCgbMode(gameboy.mode == GameBoy.Mode.CGB);
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

        if (cheatsLayer.isVisible())
            cheatsLayer.render();
    }

    /**
     * Render the emulation screen
     */
    private void renderGameScreen() {
        //Load the buffer to the texture
        screen_texture.load(gameboy.getPpu().getScreenBuffer());

        //Bind the texture
        glEnable(GL_TEXTURE_2D);
        screen_texture.bind();

        glLoadIdentity();

        //Create the main QUAD
        glBegin(GL_QUADS);
        glTexCoord2f(0, 1); glVertex2f(-1,-1);
        glTexCoord2f(0, 0); glVertex2f(-1,1);
        glTexCoord2f(1, 0); glVertex2f(1,1);
        glTexCoord2f(1, 1); glVertex2f(1,-1);

        //Cleanup the texture
        glDisable(GL_TEXTURE_2D);
        screen_texture.unbind();

        glEnd();
    }

    /**
     * Render the Main Menu
     */
    private void renderMenuBar() {
        ImGui.beginMainMenuBar();
        if (ImGui.beginMenu("File")) {
            if(ImGui.menuItem("Load ROM")) {
                FileChooser chooser = new FileChooser();
                chooser.setInitialDirectory(new File(currentDirectory));
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("GameBoy ROM (.gb, .gbc)", "*.gb", "*.gbc"));
                Platform.runLater(() -> {
                    File file = chooser.showOpenDialog(null);
                    if (file != null) {
                        currentDirectory = file.getAbsolutePath().replace(file.getName(), "");
                        try {
                            gameboy.insertCartridge(file.getAbsolutePath());
                            gameboy.setState(GameBoyState.RUNNING);
                        } catch (Exception e) {
                            Console.getInstance().log(LogLevel.ERROR, "Invalid file : " + e.getMessage());
                        }
                    }
                });
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
            if (ImGui.checkbox("Debug features", debug)) {
                gameboy.getDebugger().setEnabled(debug.get());
                if (debug.get()) {
                    //Create and start the debugger thread
                    debuggerThread = new DebuggerThread(gameboy.getDebugger());
                    debuggerThread.start();
                } else {
                    //Kill active thread
                    if (debuggerThread != null) debuggerThread.kill();
                    if (consoleThread != null) consoleThread.kill();
                }
            }
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
                if (ImGui.checkbox("Console", consoleLayerVisible)) {
                    consoleLayer.setVisible(consoleLayerVisible.get());
                    if (consoleLayer.isVisible()) {
                        //Create and run the console thread
                        consoleThread = new ConsoleThread(gameboy.getDebugger());
                        consoleThread.start();
                        ((ConsoleLayer)consoleLayer).hookThread(consoleThread);
                    } else {
                        //Kill the active thread
                        if (consoleThread != null) consoleThread.kill();
                    }
                }
            }

            //Update the hooked component
            synchronized (gameboy.getDebugger()) {
                gameboy.getDebugger().setHooked(DebuggerMode.CPU, cpuLayerVisible.get());
                gameboy.getDebugger().setHooked(DebuggerMode.MEMORY, memoryLayerVisible.get());
                gameboy.getDebugger().setHooked(DebuggerMode.PPU, ppuLayerVisible.get());
                gameboy.getDebugger().setHooked(DebuggerMode.APU, apuLayerVisible.get());
                gameboy.getDebugger().setHooked(DebuggerMode.CONSOLE, consoleLayerVisible.get());
                gameboy.getDebugger().setHooked(DebuggerMode.SERIAL, serialOutputLayerVisible.get());
            }
            ImGui.endMenu();
        }

        if (ImGui.beginMenu("Settings")) {
            settingsLayer.setVisible(!settingsLayer.isVisible());
            ImGui.endMenu();
        }
        if (ImGui.beginMenu("Cheats")) {
            cheatsLayer.setVisible(!cheatsLayer.isVisible());
            ImGui.endMenu();
        }
        ImGui.endMainMenuBar();
    }

    private void handleInput() {
        for (Button button : Button.values()) {
            if (glfwGetKey(windowPtr, Button.getKeyboardMap().get(button)) == GLFW_PRESS)
                gameboy.setButtonState(button, InputState.PRESSED);
            else
                gameboy.setButtonState(button, InputState.RELEASED);
        }
    }

    public long getId() {
        return windowPtr;
    }
}