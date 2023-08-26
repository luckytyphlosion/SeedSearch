package seedsearch;

public class SeedResultSimple {
    public final long seed;
    public final boolean isRight;
    public final int firstCardOrientation;

    public SeedResultSimple(long seed, boolean isRight, int firstCardOrientation) {
        this.seed = seed;
        this.isRight = isRight;
        this.firstCardOrientation = firstCardOrientation;
    }
}
