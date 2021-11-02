package gbemu.core.memory;

public interface IMMUListener {

    void onWriteToMMU(int addr, int data);

}
