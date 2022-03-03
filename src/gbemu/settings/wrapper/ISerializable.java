package gbemu.settings.wrapper;

public interface ISerializable {

    String serialize();
    void deserialize(String str);
}
