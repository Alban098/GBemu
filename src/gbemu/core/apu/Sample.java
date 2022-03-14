package gbemu.core.apu;

public record Sample(float square_1, float square_2, int wave, int noise) {

    public float getNormalizedValue() {
        return (square_1 + square_2 + wave + noise) / 60f;
    }
}
