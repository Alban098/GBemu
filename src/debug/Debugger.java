package debug;

import console.Console;
import core.Flags;
import core.GameBoy;
import core.GameBoyState;
import core.apu.Sample;
import core.cpu.Instruction;
import core.cpu.State;
import core.memory.MMU;
import openGL.SwappingByteBuffer;

import java.awt.*;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Debugger {

    private static final int DECOMPILE_SIZE = 0x11;

    private final GameBoy gameboy;

    private boolean enabled = false;

    private core.cpu.State cpuState;
    private core.ppu.State ppuState;
    private Queue<Sample> sampleQueue;

    private final Map<DebuggerMode, Boolean> hookedModes;

    private final Map<Integer, BreakPoint> breakpoints;
    private final Queue<Instruction> instructionQueue;

    private final Point hoveredTileMap;
    private final Point hoveredTileTables;
    private final Point hoveredSprite;
    private final Tile tileMapHoveredTile;
    private final Tile tileTableHoveredTile;
    private int selectedTileMap = 0;
    private boolean tileMapGrid = false;
    private boolean tileTablesGrid = false;
    private boolean showViewport = true;

    public Debugger(GameBoy gb) {
        breakpoints = new ConcurrentHashMap<>();
        instructionQueue = new ConcurrentLinkedQueue<>();
        this.gameboy = gb;

        hoveredTileMap = new Point(-1, -1);
        hoveredTileTables = new Point(-1, -1);
        hoveredSprite = new Point(-1, -1);
        tileMapHoveredTile = new Tile();
        tileTableHoveredTile = new Tile();

        hookedModes = new ConcurrentHashMap<>();
        for (DebuggerMode mode : DebuggerMode.values())
            hookedModes.put(mode, false);
    }

    public String getSector(int addr) {
        return gameboy.getMemory().getSector(addr);
    }

    public void link(core.cpu.State state) {
        this.cpuState = state;
    }

    public void link(core.ppu.State state) {
        this.ppuState = state;
    }

    public void link(Queue<Sample> queue) {
        this.sampleQueue = queue;
    }

    public void link(MMU memory) {
        memory.linkDebugger(this);
    }

    private void decompile() {
        int addr = cpuState.getPc().read();
        for (Instruction instr : instructionQueue) {
            if (addr >= 0x8000 && addr <= 0x9FFF || addr >= 0xFE00 && addr <= 0xFF7F || addr == 0xFFFF || addr >= 0x0104 && addr <= 0x014F) {
                instr.setAddr(addr);
                instr.setLength(0x10 - (addr & 0xF));
                for (int i = 0; i < instr.getLength(); i++)
                    instr.setParam(i, gameboy.getMemory().readByte(addr++));
                instr.setName("db   ");
                instr.setOpcode(0x00);
            } else {
                int opcode = gameboy.getMemory().readByte(addr++);
                if (opcode == 0xCB) {
                    instr.copyMeta(gameboy.getCpu().cb_opcodes.get(gameboy.getMemory().readByte(addr++)));
                    instr.setAddr(addr - 2);
                } else {
                    instr.copyMeta(gameboy.getCpu().opcodes.get(opcode));
                    instr.setAddr(addr - 1);
                }
                if (instr.getLength() == 2)
                    instr.setParams(gameboy.getMemory().readByte(addr++));

                if (instr.getLength() == 3)
                    instr.setParams(gameboy.getMemory().readByte(addr++), gameboy.getMemory().readByte(addr++));
            }
            instr.updateStrings();
        }
    }

    public void addBreakpoint(int addr, BreakPoint.Type type) {
        breakpoints.put(addr, new BreakPoint(addr, type));
    }

    public void removeBreakpoint(int addr) {
        breakpoints.remove(addr);
    }

    public void breakpointCheck() {
        int addr = cpuState.getInstruction().getAddr();
        if (breakpoints.containsKey(addr) && (breakpoints.get(addr).type() == BreakPoint.Type.EXEC  || breakpoints.get(addr).type() == BreakPoint.Type.ALL)) {
            gameboy.setState(GameBoyState.DEBUG);
            Console.getInstance().log(Console.Type.WARNING, "Execution stopped at $" + String.format("%04X", cpuState.getInstruction().getAddr()) + " (breakpoint reached (EXEC))");
        }
        addr = cpuState.getInstruction().getParamAddress();
        if (cpuState.getInstruction().getType() == Instruction.Type.R || cpuState.getInstruction().getType() == Instruction.Type.RW) {
            if (breakpoints.containsKey(addr) && (breakpoints.get(addr).type() == BreakPoint.Type.READ || breakpoints.get(addr).type() == BreakPoint.Type.RW  || breakpoints.get(addr).type() == BreakPoint.Type.ALL)) {
                gameboy.setState(GameBoyState.DEBUG);
                Console.getInstance().log(Console.Type.WARNING, "Execution stopped at $" + String.format("%04X", cpuState.getInstruction().getAddr()) + " (read from " + String.format("%04X", addr) + ")");
            }
        }

        if (cpuState.getInstruction().getType() == Instruction.Type.W || cpuState.getInstruction().getType() == Instruction.Type.RW) {
            if (breakpoints.containsKey(addr) && (breakpoints.get(addr).type() == BreakPoint.Type.WRITE || breakpoints.get(addr).type() == BreakPoint.Type.RW  || breakpoints.get(addr).type() == BreakPoint.Type.ALL)) {
                gameboy.setState(GameBoyState.DEBUG);
                Console.getInstance().log(Console.Type.WARNING, "Execution stopped at $" + String.format("%04X", cpuState.getInstruction().getAddr()) + " (write to " + String.format("%04X", addr) + ")");
            }
        }
    }

    public void clock() {
        if (isHooked(DebuggerMode.CPU) && gameboy.getState() == GameBoyState.DEBUG)
            decompile();
        if (isHooked(DebuggerMode.PPU)) {
            if (isHooked(DebuggerMode.OAMS)) {
                gameboy.getPpu().computeOAM(hoveredSprite.x, hoveredSprite.y);
            } else if (isHooked(DebuggerMode.TILES)) {
                gameboy.getPpu().computeTileTables(hoveredTileTables.x, hoveredTileTables.y, tileTablesGrid);
                computeHoveredTileTables();
            } else if (isHooked(DebuggerMode.TILEMAPS)) {
                gameboy.getPpu().computeTileMaps(showViewport, selectedTileMap, hoveredTileMap.x, hoveredTileMap.y, tileMapGrid);
                computeHoveredTileMap();
            }
        }
    }

    private void computeHoveredTileTables() {
        boolean mode1 = hoveredTileTables.y <= 0xF;
        int tileId = ((hoveredTileTables.y << 4) & 0xF0) | hoveredTileTables.x & 0x0F;
        int tileAddr = MMU.TILE_BLOCK_START | (tileId << 4) + (mode1 ? 0x1000 : 0x0000);
        int bank = hoveredTileTables.x > 0x0F ? 1 : 0;
        gameboy.getPpu().renderTile(tileId, 0, tileTableHoveredTile.renderTarget, bank, mode1, GameBoy.Mode.DMG);
        tileTableHoveredTile.fill(0, 0, 0, tileAddr, tileId, 0, bank);
    }

    private void computeHoveredTileMap() {
        boolean mode1 = gameboy.getMemory().readIORegisterBit(MMU.LCDC, Flags.LCDC_BG_TILE_DATA);
        int mapAddr = (selectedTileMap == 0 ? MMU.BG_MAP0_START : MMU.BG_MAP1_START) | hoveredTileMap.x | (hoveredTileMap.y << 5);
        int tileId = gameboy.getMemory().readVRAM(mapAddr, 0);
        int tileAddr = MMU.TILE_BLOCK_START + 0x800 * (mode1 ? 0 : 2) + tileId * 16;
        int attrib = gameboy.mode == GameBoy.Mode.CGB ? gameboy.getMemory().readVRAM(mapAddr, 1) : 0;
        gameboy.getPpu().renderTile(tileId, attrib, tileMapHoveredTile.renderTarget,0 , mode1, gameboy.mode);
        tileMapHoveredTile.fill(hoveredTileMap.x, hoveredTileMap.y, mapAddr, tileAddr, tileId, attrib, (attrib & Flags.CGB_TILE_VRAM_BANK) != 0 ? 1 : 0);
    }

    public synchronized State getCpuState() {
        return cpuState;
    }

    public Queue<Instruction> getInstructionQueue() {
        return instructionQueue;
    }

    public synchronized boolean isHooked(DebuggerMode mode) {
        if (!enabled)
            return false;
        return hookedModes.get(mode);
    }

    public GameBoyState getGameboyState() {
        return gameboy.getState();
    }

    public void setHooked(DebuggerMode mode, boolean hooked) {
        hookedModes.put(mode, hooked);
    }

    public void setGameboyState(GameBoyState state) {
        gameboy.setState(state);
    }

    public Queue<Sample> getSampleQueue() {
        return sampleQueue;
    }

    public int readCGBPalette(boolean obj_pal, int addr) {
        synchronized (gameboy.getMemory()) {
            return gameboy.getMemory().readCGBPalette(obj_pal, addr);
        }
    }

    public String getSerialOutput() {
        return gameboy.getSerialOutput();
    }

    public void flushSerialOutput() {
        gameboy.flushSerialOutput();
    }

    public void reset() {
        gameboy.reset();
    }

    public void init() {
        instructionQueue.clear();
        for (int i = 0; i < DECOMPILE_SIZE; i++)
            instructionQueue.add(new Instruction(0, Instruction.Type.MISC,"NOP", 1, null, cpuState));
    }

    public synchronized void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public synchronized SwappingByteBuffer getTileTableBuffer() {
        return ppuState.getTileTableBuffer();
    }

    public synchronized SwappingByteBuffer getTileMapBuffer(int index) {
        return ppuState.getTileMapBuffers()[index];
    }

    public synchronized SwappingByteBuffer getOAMBuffer() {
        return ppuState.getOAMBuffer();
    }

    public synchronized boolean isEnabled() {
        return enabled;
    }

    public int readMemory(int addr) {
        synchronized (gameboy.getMemory()) {
            return gameboy.getMemory().readByte(addr, true);
        }
    }

    public int getROMBank() {
        synchronized (gameboy.getMemory()) {
            return gameboy.getMemory().getROMBank();
        }
    }

    public int getRAMBank() {
        synchronized (gameboy.getMemory()) {
            return gameboy.getMemory().getRAMBank();
        }
    }

    public int getVRAMBank() {
        synchronized (gameboy.getMemory()) {
            return gameboy.getMemory().getVRAMBank();
        }
    }

    public int getWRAMBank() {
        synchronized (gameboy.getMemory()) {
            return gameboy.getMemory().getWRAMBank();
        }
    }

    public Map<Integer, BreakPoint> getBreakpoints() {
        return breakpoints;
    }

    public synchronized Tile getTileMapHoveredTile() {
        return tileMapHoveredTile;
    }

    public synchronized Tile getTileTableHoveredTile() {
        return tileTableHoveredTile;
    }

    public synchronized void setHoveredTileOnMap(int x, int y) {
        if (x < 0 || x > 0x1F || y < 0 || y > 0x1F) {
            hoveredTileMap.x = -1;
            hoveredTileMap.y = -1;
        } else {
            hoveredTileMap.x = x;
            hoveredTileMap.y = y;
        }
    }

    public synchronized void selectTileMap(int id) {
        selectedTileMap = id & 0x1;
    }

    public synchronized void enableViewport(boolean enabled) {
        showViewport = enabled;
    }

    public synchronized void enableTileTablesGrid(boolean enabled) {
        tileTablesGrid = enabled;
    }

    public synchronized void enableTileMapGrid(boolean enabled) {
        tileMapGrid = enabled;
    }

    public synchronized void setHoveredTileOnTables(int x, int y) {
        if ((x < 0 || x > 0x1F || y < 0 || y > 0x18)) {
            hoveredTileTables.x = -1;
            hoveredTileTables.y = -1;
        } else {
            hoveredTileTables.x = x;
            hoveredTileTables.y = y;
        }
    }

    public synchronized void setHoveredSprite(int x, int y) {
        hoveredSprite.x = x;
        hoveredSprite.y = y;
    }
}

