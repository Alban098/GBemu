package core.ppu.helper;

public interface IMMUListener {

    void onWriteToMMU(int addr, int data);
}
