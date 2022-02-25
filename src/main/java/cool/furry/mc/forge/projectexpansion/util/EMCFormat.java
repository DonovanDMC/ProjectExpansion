package cool.furry.mc.forge.projectexpansion.util;

import cool.furry.mc.forge.projectexpansion.config.Config;
import net.minecraft.client.gui.screen.Screen;

import javax.annotation.Nullable;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.util.Arrays;

public class EMCFormat extends DecimalFormat {
    public static EMCFormat INSTANCE = new EMCFormat();

    private EMCFormat() {
        super("#,###");
        setRoundingMode(RoundingMode.DOWN);
    }

    private static final Object[][] list = {
            { 1_000_000_000_000_000_000_000_000_000_000_000_000_000_000_000_000_000_000_000_000D, "No", "Novemdecillion" }, // 10^60
            { 1_000_000_000_000_000_000_000_000_000_000_000_000_000_000_000_000_000_000_000D, "Oc", "Octodecillion" }, // 10^57
            { 1_000_000_000_000_000_000_000_000_000_000_000_000_000_000_000_000_000_000D, "St", "Septendecillion" }, // 10^54
            { 1_000_000_000_000_000_000_000_000_000_000_000_000_000_000_000_000_000D, "Sd", "Sexdecillion" }, // 10^51
            { 1_000_000_000_000_000_000_000_000_000_000_000_000_000_000_000_000D, "Qd", "Quindecillion" }, // 10^48
            { 1_000_000_000_000_000_000_000_000_000_000_000_000_000_000_000D, "Qt", "Quattuordecillion" }, // 10^45
            { 1_000_000_000_000_000_000_000_000_000_000_000_000_000_000D, "Tr", "Tredecillion" }, // 10^42
            { 1_000_000_000_000_000_000_000_000_000_000_000_000_000D, "Du", "Duodecillion" }, // 10^39
            { 1_000_000_000_000_000_000_000_000_000_000_000_000D, "U", "Undecillion" }, // 10^36
            { 1_000_000_000_000_000_000_000_000_000_000_000D, "D", "Decillion" }, // 10^33
            { 1_000_000_000_000_000_000_000_000_000_000D, "N", "Nonillion" }, // 10^30
            { 1_000_000_000_000_000_000_000_000_000D, "O", "Octillion" }, // 10^27
            { 1_000_000_000_000_000_000_000_000D, "Sp", "Septillion" }, // 10^24
            { 1_000_000_000_000_000_000_000D, "Sx", "Sextillion" }, // 10^21
            { 1_000_000_000_000_000_000D, "Qu", "Quintillion" }, // 10^18
            { 1_000_000_000_000_000D, "Qa", "Quadrillion" }, // 10^15
            { 1_000_000_000_000D, "T", "Trillion" }, // 10^12
            { 1_000_000_000D, "B", "Billion" }, // 10^9
            { 1_000_000D, "M", "Million" }, // 10^6
    };

    @Override
    public StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition) {
        // TransmutationEMCFormatter.formatEMC(emcAmount)
        if(Config.formatEMC.get() && number > 1_000_000D && !Screen.hasShiftDown()) {
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

        return super.format(number, result, fieldPosition);
    }

    @Override
    public StringBuffer format(long number, StringBuffer result, FieldPosition fieldPosition) {
        return format(Double.parseDouble(Long.toString(number)), result, fieldPosition);
    }
}