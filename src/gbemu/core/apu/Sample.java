package gbemu.core.apu;

public record Sample(int square_1, int square_2, int wave, int noise) {

    public float getNormalizedValue() {
        return (square_1 + square_2 + wave + noise) / 60f;
    }
}
