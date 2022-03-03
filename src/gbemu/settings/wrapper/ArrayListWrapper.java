package gbemu.settings.wrapper;

import java.util.ArrayList;
import java.util.Collection;

public class ArrayListWrapper<T extends ISerializable> extends ArrayList<T> implements ISerializable {

    private final Class<T> c;

    public ArrayListWrapper(int initialCapacity, Class<T> c) {
        super(initialCapacity);
        this.c = c;
    }

    public ArrayListWrapper(Class<T> c) {
        super();
        this.c = c;
    }

    public ArrayListWrapper(Collection<? extends T> collection, Class<T> c) {
        super(collection);
        this.c = c;
    }

    @Override
    public String serialize() {
        StringBuilder builder = new StringBuilder();
        for (T item : this) {
            builder.append("{").append(item.serialize()).append("}");
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
                    T obj = c.getDeclaredConstructor().newInstance();
                    obj.deserialize(currentItem.toString());
                    add(obj);
                    currentItem = new StringBuilder();
                }
            }
        } catch (Exception ignored) {}
    }
}