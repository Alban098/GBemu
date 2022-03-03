package gbemu.settings.wrapper;

import java.util.HashMap;

public class HashMapWrapper<K extends ISerializable, V extends ISerializable> extends HashMap<K, V> implements ISerializable {

    private static final String SEPARATOR = "//";
    private final Class<K> key_class;
    private final Class<V> value_class;

    public HashMapWrapper(int initialCapacity, Class<K> key_class, Class<V> value_class) {
        super(initialCapacity);
        this.key_class = key_class;
        this.value_class = value_class;
    }

    public HashMapWrapper(Class<K> key_class, Class<V> value_class) {
        super();
        this.key_class = key_class;
        this.value_class = value_class;
    }

    public static Class<?> getWrapperType() {
        return HashMap.class;
    }

    @Override
    public String serialize() {
        StringBuilder builder = new StringBuilder();
        for (Entry<K, V> entry : entrySet()) {
            builder.append("{").append(entry.getKey().serialize()).append(";").append(entry.getValue().serialize()).append("}");
        }
        return builder.toString();
    }

    @Override
    public void deserialize(String str) {
        try {
            clear();
            int level = 0;
            StringBuilder currentItem = new StringBuilder();
            for (char chr : str.toCharArray()) {
                if (chr == '{') {
                    if (level > 0)
                        currentItem.append(chr);
                    level++;
                    continue;
                } else if (chr == '}') {
                    level--;
                }
                if (level >= 1) {
                    currentItem.append(chr);
                } else {
                    String[] split = currentItem.toString().split(";");
                    K key = key_class.getDeclaredConstructor().newInstance();
                    V val = value_class.getDeclaredConstructor().newInstance();
                    key.deserialize(split[0]);
                    val.deserialize(split[1]);
                    put(key, val);
                    currentItem = new StringBuilder();
                }
            }
        } catch (Exception ignored) {}
    }
}