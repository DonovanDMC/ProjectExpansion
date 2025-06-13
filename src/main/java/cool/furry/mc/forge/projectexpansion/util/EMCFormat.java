package cool.furry.mc.neoforge.projectexpansion.util;

import cool.furry.mc.neoforge.projectexpansion.config.Config;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

@SuppressWarnings("unused")
public class EMCFormat extends DecimalFormat {
    public static final EMCFormat INSTANCE = new EMCFormat();
    private static final DecimalFormat decimalFormat = new DecimalFormat();
    public static final BigDecimal FORMAT_START = BigDecimal.valueOf(1000000);

    private EMCFormat() {
        super("#,###");
        setRoundingMode(RoundingMode.DOWN);
    }

    public static EMCFormatWrapper wrap(BigInteger value) { return new EMCFormatWrapper(new BigDecimal(value)); }
    public static EMCFormatWrapper wrap(BigDecimal value) { return new EMCFormatWrapper(value); }

    @Override
    public StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition) {
        String formatted = format(BigDecimal.valueOf(number));
        return new StringBuffer(formatted);
    }

    @Override
    public StringBuffer format(long number, StringBuffer result, FieldPosition fieldPosition) {
        return format(Double.parseDouble(Long.toString(number)), result, fieldPosition);
    }

    public static MutableComponent getComponent(long value) {
        return getComponent(BigDecimal.valueOf(value));
    }
    public static MutableComponent getComponent(BigInteger value) {
        return getComponent(new BigDecimal(value));
    }
    public static MutableComponent getComponent(BigDecimal value) {
        return Component.literal(format(value));
    }

    public static String formatForce(BigInteger value) {
        return format(value, FormatOptions.create().force());
    }
    public static String formatForceShort(BigInteger value) {
        return format(value, FormatOptions.create().forceShort());
    }
    public static String formatForceLong(BigInteger value) {
        return format(value, FormatOptions.create().forceLong());
    }
    public static String format(BigInteger value) {
        return format(value, FormatOptions.create());
    }
    public static String format(BigInteger value, FormatOptions formatOptions) {
        return format(new BigDecimal(value), formatOptions);
    }


    public static String formatForce(BigDecimal value) {
        return format(value, FormatOptions.create().force());
    }
    public static String formatForceShort(BigDecimal value) {
        return format(value, FormatOptions.create().forceShort());
    }
    public static String formatForceLong(BigDecimal value) {
        return format(value, FormatOptions.create().forceLong());
    }

    public static String format(BigDecimal value) {
        return format(value, FormatOptions.create());
    }
    public static String format(BigDecimal value, FormatOptions options) {
        if (options.shouldFormat()) {
            NumberName name = NumberName.findName(value);
            if(name == null) return NumberFormat.getNumberInstance(Locale.US).format(value);
            return getString(value) + (options.useLongNames() ? String.format(" %s", name.getName(false)) : name.getName(true));
        } else return NumberFormat.getNumberInstance(Locale.US).format(value);
    }

    public static String getString(BigDecimal value) {
        return getString(value.toBigInteger());
    }

    static String getString(BigInteger value) {
        String str = value.toString();
        ArrayList<String> list = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for(int i = str.length(); i > 0; i--) {
            if(current.length() >= 3) {
                list.add(current.reverse().toString());
                current.delete(0, current.length());
            }
            current.append(str.charAt(i - 1));
        }
        if(!current.isEmpty()) {
            list.add(current.reverse().toString());
            current.delete(0, current.length());
        }
        String sig = list.getLast();
        String dec = list.get(list.size() - 2).substring(0, 2);
        return String.format("%s.%s", sig, dec);
    }

    public static class FormatOptions {
        private boolean ignoreShift = false;
        private boolean force = false;
        private boolean forceLong = false;
        private boolean forceShort = false;
        private boolean none = false;

        public FormatOptions ignoreShift() { this.ignoreShift = true; return this; }
        public FormatOptions force() { this.force = true; return this; }
        public FormatOptions forceLong() { this.force = this.forceLong = true; return this; }
        public FormatOptions forceShort() { this.force = this.forceShort = true; return this; }
        public FormatOptions none() { this.none = true; return this; }

        public boolean shouldFormat() {
            return !none && (force || (Config.client.formatEMC.get() && (ignoreShift || !Screen.hasShiftDown())));
        }

        public boolean useShortNames() {
            return !forceLong && (forceShort || !Config.client.fullNumberNames.get());
        }

        public boolean useLongNames() {
            return !forceShort && (forceLong || Config.client.fullNumberNames.get());
        }

        public static FormatOptions create() { return new FormatOptions(); }
    }

    public static class EMCFormatWrapper {
        private final BigDecimal value;
        private final FormatOptions options = FormatOptions.create();
        public EMCFormatWrapper(BigDecimal value) { this.value = value; }

        public EMCFormatWrapper ignoreShift() { this.options.ignoreShift(); return this; }
        public EMCFormatWrapper force() { this.options.force(); return this; }
        public EMCFormatWrapper forceLong() { this.options.forceLong(); return this; }
        public EMCFormatWrapper forceShort() { this.options.forceShort(); return this; }
        public EMCFormatWrapper none() { this.options.none(); return this; }

        public String format() { return EMCFormat.format(value, options); }
    }

    public enum NumberName {
        MILLION(1e6, "M"),
        BILLION(1e9, "B"),
        TRILLION(1e12, "T"),
        QUADRILLION(1e15, "Qa"),
        QUINTILLION(1e18, "Qi"),
        SEXTILLION(1e21, "Sx"),
        SEPTILLION(1e24, "Sp"),
        OCTILLION(1e27, "O"),
        NONILLION(1e30, "N"),
        DECILLION(1e33, "D"),
        UNDECILLION(1e36, "U"),
        DUODECILLION(1e39, "Du"),
        TREDECILLION(1e42, "Tr"),
        QUATTUORDECILLION(1e45, "Qt"),
        QUINDECILLION(1e48, "Qd"),
        SEXDECILLION(1e51, "Sd"),
        SEPTENDECILLION( 1e54, "St"),
        OCTODECILLION(1e57, "Oc"),
        NOVEMDECILLION(1e60, "No");

        public static final NumberName[] VALUES = values();

        private final double value;
        private final String shortName;
        NumberName(double val, String shortName) {
            this.value  = val;
            this.shortName = shortName;
        }

        public String getName() {
            return getName(false);
        }

        public String getName(boolean getShort) {
            return getShort ? shortName : (name().charAt(0) + name().toLowerCase(Locale.US).substring(1)).trim();
        }

        public double getValue() {
            return value;
        }

        public BigInteger getBigIntegerValue() {
            return new BigInteger(String.valueOf(getValue()));
        }

        public BigDecimal getBigDecimalValue() {
            return new BigDecimal(String.valueOf(getValue()));
        }

        static @Nullable NumberName findName(BigDecimal value) {
            // reduce is a quick and dirty solution to get the last element
            return Arrays.stream(VALUES).filter(v -> value.compareTo(v.getBigDecimalValue()) > -1).reduce((first, second) -> second).orElse(null);
        }
    }
}

