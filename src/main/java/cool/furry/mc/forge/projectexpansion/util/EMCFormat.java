package cool.furry.mc.forge.projectexpansion.util;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.config.Config;
import net.minecraft.client.gui.screen.Screen;

import javax.annotation.Nullable;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.util.Arrays;

public class EMCFormat extends DecimalFormat {
    public static EMCFormat INSTANCE = new EMCFormat(false);
    public static EMCFormat INSTANCE_IGNORE_SHIFT = new EMCFormat(true);
    public static final double FORMAT_START = 1e6;
    private final boolean ignoreShift;

    private EMCFormat(boolean ignoreShift) {
        super("#,###");
        setRoundingMode(RoundingMode.DOWN);
        this.ignoreShift = ignoreShift;
    }

    private static final Object[][] list = {
            { 1e60, "No", "Novemdecillion" },
            { 1e57, "Oc", "Octodecillion" },
            { 1e54, "St", "Septendecillion" },
            { 1e51, "Sd", "Sexdecillion" },
            { 1e48, "Qd", "Quindecillion" },
            { 1e45, "Qt", "Quattuordecillion" },
            { 1e42, "Tr", "Tredecillion" },
            { 1e39, "Du", "Duodecillion" },
            { 1e36, "U", "Undecillion" },
            { 1e33, "D", "Decillion" },
            { 1e30, "N", "Nonillion" },
            { 1e27, "O", "Octillion" },
            { 1e24, "Sp", "Septillion" },
            { 1e21, "Sx", "Sextillion" },
            { 1e18, "Qu", "Quintillion" },
            { 1e15, "Qa", "Quadrillion" },
            { 1e12, "T", "Trillion" },
            { 1e9, "B", "Billion" },
            { 1e6, "M", "Million" },
    };

    @Override
    public StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition) {
        if(Config.formatEMC.get() && number > FORMAT_START && (ignoreShift || !Screen.hasShiftDown())) {
            Object[] res = Arrays.stream(list).filter((p) -> number >= (double) p[0]).findFirst().orElse(new Object[]{ number, "", "" });
            double num = number;
            @Nullable
            String abr = null;
            @Nullable
            String full = null;
            if(res[1] != "") {
                num = number / (double) res[0];
                abr = (String) res[1];
                full = (String) res[2];
            }
            StringBuffer str = new StringBuffer();
            str.append(String.format("%.02f", num));
            if(abr != null && full != null) str.append(Config.fullNumberNames.get() ? String.format(" %s", full) : abr);
            return str;
        }

        Main.Logger.info(number);

        return super.format(number, result, fieldPosition);
    }

    @Override
    public StringBuffer format(long number, StringBuffer result, FieldPosition fieldPosition) {
        return format(Double.parseDouble(Long.toString(number)), result, fieldPosition);
    }
}