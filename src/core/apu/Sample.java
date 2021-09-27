package core.apu;

public class Sample {
    public int square1;
    public int square2;
    public int wave;
    public int noise;

    public Sample(int square1, int square2, int wave, int noise) {
        this.square1 = square1;
        this.square2 = square2;
        this.wave = wave;
        this.noise = noise;
    }

    public float getNormalizedValue() {
        return (square1 + square2 + wave + noise) / 60f;
    }
}
