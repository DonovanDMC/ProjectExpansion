package cool.furry.mc.projectexpansion.wiki.images;

import cool.furry.mc.projectexpansion.wiki.EmcDisplay;
import cool.furry.mc.projectexpansion.wiki.WikiImage;

import java.math.BigInteger;

// see WikiImage, listed in WikiImages.DEFINITIONS
public final class Displays {
    private Displays() {}

    /**************
     * EMC Values *
     **************/

    // 36.21 Novemdecillion, the same as the old screenshot
    @WikiImage("EMC-Values/shortened-display.png")
    @EmcDisplay
    public static BigInteger shortenedDisplay() {
        return HUGE_EMC;
    }

    @WikiImage(value = "EMC-Values/full-display.png", fullEmc = true)
    @EmcDisplay
    public static BigInteger fullDisplay() {
        return HUGE_EMC;
    }

    /***********
     * Helpers *
     ***********/

    private static final BigInteger HUGE_EMC = new BigInteger("36213621362136210").multiply(BigInteger.TEN.pow(45));
}
