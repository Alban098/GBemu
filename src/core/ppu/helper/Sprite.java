package core.ppu.helper;

public record Sprite(int y, int x, int tileId, int attributes) implements Comparable<Sprite> {

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
