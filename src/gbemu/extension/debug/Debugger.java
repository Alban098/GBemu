package gbemu.extension.debug;

import console.Console;
import console.LogLevel;
import gbemu.core.Flags;
import gbemu.core.GameBoy;
import gbemu.core.GameBoyState;
import gbemu.core.apu.Sample;
import gbemu.core.cpu.Instruction;
import gbemu.core.cpu.State;
import gbemu.core.memory.MMU;
import glwrapper.SwappingByteBuffer;

import java.awt.*;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class represent a Debugger that can be hooked to the Emulator and observe it
 * It is the entry point of all Debugging UI, making it independent of how the Game Boy is implemented
 */
public class Debugger {

    private static final int DECOMPILE_SIZE = 0x11;

    private final GameBoy gameboy;

    private gbemu.core.cpu.State cpu_state;
    private gbemu.core.ppu.State ppu_state;
    private Queue<Sample> sample_queue;

    private final Map<DebuggerMode, Boolean> hooked_modes;
    private final Map<Integer, BreakPoint> breakpoints;
    private final Queue<Instruction> instruction_queue;

    private final Point hovered_tile_map;
    private final Point hovered_tile_tables;
    private final Point hovered_sprite;
    private final Tile tile_map_hovered_tile;
    private final Tile tile_table_hovered_tile;

    private boolean enabled = false;
    private int selected_tile_map = 0;
    private boolean tile_map_grid = false;
    private boolean tile_tables_grid = false;
    private boolean show_viewport = true;

    /**
     * Create a new Debugger linked to a Game Boy
     * @param gb the Game Boy to observe
     */
    public Debugger(GameBoy gb) {
        breakpoints = new ConcurrentHashMap<>();
        instruction_queue = new ConcurrentLinkedQueue<>();
        this.gameboy = gb;

        hovered_tile_map = new Point(-1, -1);
        hovered_tile_tables = new Point(-1, -1);
        hovered_sprite = new Point(-1, -1);
        tile_map_hovered_tile = new Tile();
        tile_table_hovered_tile = new Tile();

        hooked_modes = new ConcurrentHashMap<>();
        for (DebuggerMode mode : DebuggerMode.values())
            hooked_modes.put(mode, false);
    }

    /**
     * Return the sector of a memory address
     * @param addr the memory address to check
     * @return the sector of the passed address
     */
    public String getSector(int addr) {
        return gameboy.getMemory().getSector(addr);
    }

    /**
     * Link a CPU State to the Debugger
     * therefore enabling the debugger to observe the CPU at any time
     * @param state the state to link
     */
    public void link(gbemu.core.cpu.State state) {
        this.cpu_state = state;
    }

    /**
     * Link a PPU State to the Debugger
     * therefore enabling the debugger to observe the PPU at any time
     * @param state the state to link
     */
    public void link(gbemu.core.ppu.State state) {
        this.ppu_state = state;
    }

    /**
     * Link a Sample Queue to the Debugger
     * therefore enabling the debugger to observe the APU samples at any time
     * @param queue the sample queue to link
     */
    public void link(Queue<Sample> queue) {
        this.sample_queue = queue;
    }

    /**
     * Decompile code starting at the current CPU Program Counter value
     */
    private void decompile() {
        int addr = cpu_state.getPc().read();

        //For each of the placeholder instruction we populate it with the decompiled instruction informations
        for (Instruction instr : instruction_queue) {
            //If the sector is marked as Data
            if (addr >= 0x8000 && addr <= 0x9FFF || addr >= 0xFE00 && addr <= 0xFF7F || addr == 0xFFFF || addr >= 0x0104 && addr <= 0x014F) {
                instr.setAddr(addr);
                instr.setLength(0x10 - (addr & 0xF));
                for (int i = 0; i < instr.getLength(); i++)
                    instr.setParam(i, gameboy.getMemory().readByte(addr++));
                instr.setName("db   ");
                instr.setOpcode(0x00);
            //Otherwise, populate it normally
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
            //Updating the String representation of the instruction
            instr.updateStrings();
        }
    }

    /**
     * Add a breakpoint to de debugger
     * @param addr the address to watch
     * @param level the access level of the breakpoint
     */
    public void addBreakpoint(int addr, BreakPoint.Type level) {
        breakpoints.put(addr, new BreakPoint(addr, level));
    }

    /**
     * Remove a breakpoint from the debugger
     * @param addr the address to unwatch
     */
    public void removeBreakpoint(int addr) {
        breakpoints.remove(addr);
    }

    /**
     * Check if any breakpoint has been reach,
     * pausing emulation and entering gbemu.extension.debug mode if any has been reached
     */
    public void breakpointCheck() {
        int addr = cpu_state.getInstruction().getAddr();

        //Test for EXEC breakpoints
        if (breakpoints.containsKey(addr) && (breakpoints.get(addr).type() == BreakPoint.Type.EXEC  || breakpoints.get(addr).type() == BreakPoint.Type.ALL)) {
            synchronized (gameboy) {
                gameboy.setState(GameBoyState.DEBUG);
            }
            Console.getInstance().log(LogLevel.WARNING, "Execution stopped at $" + String.format("%04X", cpu_state.getInstruction().getAddr()) + " (breakpoint reached (EXEC))");
        }

        addr = cpu_state.getInstruction().getParamAddress();
        //Test for READ breakpoints
        if (cpu_state.getInstruction().getType() == Instruction.Type.R || cpu_state.getInstruction().getType() == Instruction.Type.RW) {
            if (breakpoints.containsKey(addr) && (breakpoints.get(addr).type() == BreakPoint.Type.READ || breakpoints.get(addr).type() == BreakPoint.Type.RW  || breakpoints.get(addr).type() == BreakPoint.Type.ALL)) {
                synchronized (gameboy) {
                    gameboy.setState(GameBoyState.DEBUG);
                }
                Console.getInstance().log(LogLevel.WARNING, "Execution stopped at $" + String.format("%04X", cpu_state.getInstruction().getAddr()) + " (read from " + String.format("%04X", addr) + ")");
            }
        }

        //Test for WRITE breakpoints
        if (cpu_state.getInstruction().getType() == Instruction.Type.W || cpu_state.getInstruction().getType() == Instruction.Type.RW) {
            if (breakpoints.containsKey(addr) && (breakpoints.get(addr).type() == BreakPoint.Type.WRITE || breakpoints.get(addr).type() == BreakPoint.Type.RW  || breakpoints.get(addr).type() == BreakPoint.Type.ALL)) {
                synchronized (gameboy) {
                    gameboy.setState(GameBoyState.DEBUG);
                }
                Console.getInstance().log(LogLevel.WARNING, "Execution stopped at $" + String.format("%04X", cpu_state.getInstruction().getAddr()) + " (write to " + String.format("%04X", addr) + ")");
            }
        }
    }

    /**
     * Update the Debugger
     */
    public void clock() {
        //Decompile code if the debugger is currently observing the CPU
        if (isHooked(DebuggerMode.CPU) && gameboy.getState() == GameBoyState.DEBUG)
            decompile();
        //If the debugger is observing the PPU
        if (isHooked(DebuggerMode.PPU)) {
            //Render OAMs if the debugger is observing the OAMs
            if (isHooked(DebuggerMode.OAMS)) {
                gameboy.getPpu().computeOAM(hovered_sprite.x, hovered_sprite.y);
            //Render the tile tables if the debugger is observing them, also compute the tile hovered by the mouse
            } else if (isHooked(DebuggerMode.TILES)) {
                gameboy.getPpu().computeTileTables(hovered_tile_tables.x, hovered_tile_tables.y, tile_tables_grid);
                computeHoveredTileTables();
            //Render the tile maps if the debugger is observing them, also compute the tile hovered by the mouse
            } else if (isHooked(DebuggerMode.TILEMAPS)) {
                gameboy.getPpu().computeTileMaps(show_viewport, selected_tile_map, hovered_tile_map.x, hovered_tile_map.y, tile_map_grid);
                computeHoveredTileMap();
            }
        }
    }

    /**
     * Compute the hovered tile on the tile tables view
     */
    private void computeHoveredTileTables() {
        //Selecting addressing mode, depending on the tile bank (Mode 1 : bank 0/1, Mode 0 : bank 2)
        boolean mode_1 = hovered_tile_tables.y <= 0xF;
        //Compute the tile ID and address
        int tile_id = ((hovered_tile_tables.y << 4) & 0xF0) | hovered_tile_tables.x & 0x0F;
        int tile_addr = MMU.TILE_BLOCK_START | (tile_id << 4) + (mode_1 ? 0x1000 : 0x0000);
        //Calculating the VRAM Bank (only relevant in CGB Mode)
        int bank = hovered_tile_tables.x > 0x0F ? 1 : 0;
        //Filling the tile temp variable, tile tables are rendered in null mode, because tile are not palette dependant
        gameboy.getPpu().renderTile(tile_id, 0, tile_table_hovered_tile.render_target, bank, mode_1, null);
        tile_table_hovered_tile.fill(0, 0, 0, tile_addr, tile_id, 0, bank);
    }

    /**
     * Compute the hovered tile on the tile map view
     */
    private void computeHoveredTileMap() {
        //Fetching the tile addressing mode, it is fetched from the Game Boy to reflect current rendering behaviour
        boolean mode_1 = gameboy.getMemory().readIORegisterBit(MMU.LCDC, Flags.LCDC_BG_TILE_DATA);
        //Computing the address of the tile map entry
        int map_addr = (selected_tile_map == 0 ? MMU.BG_MAP0_START : MMU.BG_MAP1_START) | hovered_tile_map.x | (hovered_tile_map.y << 5);
        //Fetching the tileID and computing its address in the tile tables
        int tile_id = gameboy.getMemory().readVRAM(map_addr, 0);
        int tile_addr = MMU.TILE_BLOCK_START + 0x800 * (mode_1 ? 0 : 2) + tile_id * 16;
        //Fetching the attributes of the tile (0 if in DMG mode, because DMG does not have tile map tile attributes)
        int attrib = gameboy.mode == GameBoy.Mode.CGB ? gameboy.getMemory().readVRAM(map_addr, 1) : 0;
        //Filling the temp tile variable
        gameboy.getPpu().renderTile(tile_id, attrib, tile_map_hovered_tile.render_target,0 , mode_1, gameboy.mode);
        tile_map_hovered_tile.fill(hovered_tile_map.x, hovered_tile_map.y, map_addr, tile_addr, tile_id, attrib, (attrib & Flags.CGB_TILE_VRAM_BANK) != 0 ? 1 : 0);
    }

    /**
     * Return the linked CPU State
     * @return the linked CPU state
     */
    public synchronized State getCpuState() {
        return cpu_state;
    }

    /**
     * Return the decompiled Instruction Queue
     * @return the decompiled Instruction Queue
     */
    public Queue<Instruction> getInstructionQueue() {
        return instruction_queue;
    }

    /**
     * Return whether the debugger is hooked to a certain component or not
     * @param mode the component to test
     * @return is the debugger hooked to the specified component
     */
    public synchronized boolean isHooked(DebuggerMode mode) {
        //If the debugger is not enabled, it is not hooked to anything
        if (!enabled)
            return false;
        return hooked_modes.get(mode);
    }

    /**
     * Return the current Game Boy state
     * @return the current state of the Game Boy
     */
    public GameBoyState getGameboyState() {
        synchronized (gameboy) {
            return gameboy.getState();
        }
    }

    /**
     * Hook/Unhook the debugger to/from a component
     * @param mode the component
     * @param hooked hook or unhook
     */
    public void setHooked(DebuggerMode mode, boolean hooked) {
        hooked_modes.put(mode, hooked);
    }

    /**
     * Set the Game Boy current state
     * @param state the new game Boy state
     */
    public void setGameboyState(GameBoyState state) {
        synchronized (gameboy) {
            gameboy.setState(state);
        }
    }

    /**
     * Return the linked Sample Queue
     * @return the linked Sample Queue
     */
    public Queue<Sample> getSampleQueue() {
        return sample_queue;
    }

    /**
     * Return a color of a CGB palettes as RGB555
     * @param obj_pal the palette id
     * @param addr the address if the color (0 to 4)
     * @return the color as RGB555
     */
    public int readCGBPalette(boolean obj_pal, int addr) {
        synchronized (gameboy.getMemory()) {
            return gameboy.getMemory().readCGBPalette(obj_pal, addr);
        }
    }

    /**
     * Return the Game Boy serial output
     * @return the Game Boy serial output
     */
    public String getSerialOutput() {
        synchronized (gameboy) {
            return gameboy.getSerialOutput();
        }
    }

    /**
     * Clear the Game Boy serial output
     */
    public void flushSerialOutput() {
        synchronized (gameboy) {
            gameboy.flushSerialOutput();
        }
    }

    /**
     * Reset the Game Boy
     */
    public void reset() {
        synchronized (gameboy) {
            gameboy.reset();
        }
    }

    /**
     * Initialize the debugger
     * (create the instruction queue)
     */
    public void init() {
        instruction_queue.clear();
        for (int i = 0; i < DECOMPILE_SIZE; i++)
            instruction_queue.add(new Instruction(0, Instruction.Type.MISC,"NOP", 1, null, cpu_state));
    }

    /**
     * Enable/Disable the debugger
     * @param enabled enable/disable the debugger
     */
    public synchronized void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Return the rendered tile tables Buffer, ready to be loaded in Texture
     * @return the rendered tile table Buffer
     */
    public synchronized SwappingByteBuffer getTileTableBuffer() {
        return ppu_state.getTileTableBuffer();
    }

    /**
     * Return the rendered tile map Buffer, ready to be loaded in Texture
     * @param index the tile map index
     * @return the rendered tile map Buffer
     */
    public synchronized SwappingByteBuffer getTileMapBuffer(int index) {
        return ppu_state.getTileMapBuffers()[index];
    }

    /**
     * Return the rendered OAM Buffer, ready to be loaded in Texture
     * @return the rendered OAM Buffer
     */
    public synchronized SwappingByteBuffer getOAMBuffer() {
        return ppu_state.getOAMBuffer();
    }

    /**
     * Return whether the debugger is enabled or not
     * @return is the debugger enabled
     */
    public synchronized boolean isEnabled() {
        return enabled;
    }

    /**
     * Return the value stored at a specified address as seen by the emulator
     * (From addressable space (0x0000 - 0xFFFF)
     * @param addr the address to read
     * @return the stored data
     */
    public int readMemory(int addr) {
        synchronized (gameboy.getMemory()) {
            return gameboy.getMemory().readByte(addr, true);
        }
    }

    /**
     * Write a value at a specified address as seen by the emulator
     * Does not trigger standard behaviour from a normal CPU write such as PPU register update and so
     * (From addressable space (0x0000 - 0xFFFF)
     * @param addr the address to write
     * @param val the value to write
     */
    public void writeMemory(int addr, int val) {
        synchronized (gameboy.getMemory()) {
            gameboy.getMemory().writeRaw(addr, val);
        }
    }

    /**
     * Return the currently mapped ROM Bank
     * (0x4000 - 0x7FFF)
     * @return the mapped ROM Bank
     */
    public int getROMBank() {
        synchronized (gameboy.getMemory()) {
            return gameboy.getMemory().getROMBank();
        }
    }

    /**
     * Return the currently mapped RAM Bank
     * (0xA000 - 0xBFFF)
     * @return the mapped RAM Bank
     */
    public int getRAMBank() {
        synchronized (gameboy.getMemory()) {
            return gameboy.getMemory().getRAMBank();
        }
    }

    /**
     * Return the currently mapped VRAM Bank
     * (0x8000 - 0x9FFF)
     * @return the mapped VRAM Bank
     */
    public int getVRAMBank() {
        synchronized (gameboy.getMemory()) {
            return gameboy.getMemory().getVRAMBank();
        }
    }

    /**
     * Return the currently mapped WRAM Bank
     * (0xD000 - 0xDFFF)
     * @return the mapped WRAM Bank
     */
    public int getWRAMBank() {
        synchronized (gameboy.getMemory()) {
            return gameboy.getMemory().getWRAMBank();
        }
    }

    /**
     * Return a Map containing all breakpoints
     * index by addresses
     * @return a map of all breakpoint
     */
    public Map<Integer, BreakPoint> getBreakpoints() {
        return breakpoints;
    }

    /**
     * Return the hovered tile on tile map view
     * @return the hovered tile map tile
     */
    public synchronized Tile getTileMapHoveredTile() {
        return tile_map_hovered_tile;
    }

    /**
     * Return the hovered tile on tile tables view
     * @return the hovered tile tables tile
     */
    public synchronized Tile getTileTableHoveredTile() {
        return tile_table_hovered_tile;
    }

    /**
     * Set the hovered tile coords on tile map view
     * @param x hovered tile X
     * @param y hovered tile Y
     */
    public synchronized void setHoveredTileOnMap(int x, int y) {
        //Set to -1 if out of bounds
        if (x < 0 || x > 0x1F || y < 0 || y > 0x1F) {
            hovered_tile_map.x = -1;
            hovered_tile_map.y = -1;
        } else {
            hovered_tile_map.x = x;
            hovered_tile_map.y = y;
        }
    }

    /**
     * Set the tile map id to render on tilemap view
     * @param id the tile map id
     */
    public synchronized void selectTileMap(int id) {
        selected_tile_map = id & 0x1;
    }

    /**
     * Enable/Disable the viewport rendering on tile map view
     * @param enabled enable/disable viewport
     */
    public synchronized void enableViewport(boolean enabled) {
        show_viewport = enabled;
    }

    /**
     * Enable/Disable the grid on tile tables view
     * @param enabled enable/disable grid
     */
    public synchronized void enableTileTablesGrid(boolean enabled) {
        tile_tables_grid = enabled;
    }

    /**
     * Enable/Disable the grid on tile map view
     * @param enabled enable/disable grid
     */
    public synchronized void enableTileMapGrid(boolean enabled) {
        tile_map_grid = enabled;
    }

    /**
     * Set the hovered tile coords on tile tables view
     * @param x hovered tile X
     * @param y hovered tile Y
     */
    public synchronized void setHoveredTileOnTables(int x, int y) {
        //Set to -1 if out of bounds
        if ((x < 0 || x > 0x1F || y < 0 || y > 0x18)) {
            hovered_tile_tables.x = -1;
            hovered_tile_tables.y = -1;
        } else {
            hovered_tile_tables.x = x;
            hovered_tile_tables.y = y;
        }
    }

    /**
     * Set the hovered sprite coords on OAM view
     * @param x hovered sprite X
     * @param y hovered sprite Y
     */
    public synchronized void setHoveredSprite(int x, int y) {
        hovered_sprite.x = x;
        hovered_sprite.y = y;
    }
}