package core.ppu.helper;

public class Sprite implements Comparable<Sprite> {

    public int y;
    public int x;
    public int tileId;
    public int attributes;

    public Sprite(int y, int x, int tileId, int attributes) {
        this.y = y;
        this.x = x;
        this.tileId = tileId;
        this.attributes = attributes;
    }

    @Override
    public int compareTo(Sprite o) {
        if (x < o.x) {
            return -1;
        } else if (x == o.x) {
            if (tileId < o.tileId)
                return -1;
            else if (tileId == o.tileId)
                return 0;
        }
        return 1;
    }
}
