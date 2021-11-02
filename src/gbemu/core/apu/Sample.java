package gbemu.core.apu;

public record Sample(int square1, int square2, int wave, int noise) {

    public float getNormalizedValue() {
        return (square1 + square2 + wave + noise) / 60f;
    }
}
