package core.ppu.helper;

public interface IMMUListener {

    void onWriteToMMU(int addr, int data);
    default int onReadToMMU(int addr) {
        return -1;
    }
}
