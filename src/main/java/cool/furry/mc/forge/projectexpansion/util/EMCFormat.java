package cool.furry.mc.forge.projectexpansion.util;

import cool.furry.mc.forge.projectexpansion.config.Config;
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
    public static EMCFormat INSTANCE = new EMCFormat();
    private static final DecimalFormat decimalFormat = new DecimalFormat();
    public static final BigDecimal FORMAT_START = BigDecimal.valueOf(1000000);

    private EMCFormat() {
        super("#,###");
        setRoundingMode(RoundingMode.DOWN);
    }

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

    public static String formatForceShort(BigInteger value) {
        return format(value, IgnoreShiftType.FORMAT);
    }
    public static String formatForceLong(BigInteger value) {
        return format(value, IgnoreShiftType.NO_FORMAT);
    }
    public static String format(BigInteger value) {
        return format(value, IgnoreShiftType.NONE);
    }
    public static String format(BigInteger value, IgnoreShiftType ignoreShiftType) {
        return format(new BigDecimal(value), ignoreShiftType);
    }


    public static String formatForceShort(BigDecimal value) {
        return format(value, IgnoreShiftType.FORMAT);
    }

    public static String formatForceLong(BigDecimal value) {
        return format(value, IgnoreShiftType.NO_FORMAT);
    }
    public static String format(BigDecimal value) {
        return format(value, IgnoreShiftType.NONE);
    }
    public static String format(BigDecimal value, IgnoreShiftType ignoreShiftType) {
        if (shouldFormat(value, ignoreShiftType)) {
            NumberName name = NumberName.findName(value);
            if(name == null) return NumberFormat.getNumberInstance(Locale.US).format(value);
            return getShort(value) + (Config.fullNumberNames.get() ? String.format(" %s", name.getName(false)) : name.getName(true));
        } else return NumberFormat.getNumberInstance(Locale.US).format(value);
    }

    public boolean shouldFormat(BigDecimal value) {
        return shouldFormat(value, IgnoreShiftType.NONE);
    }
    public static boolean shouldFormat(BigDecimal value, IgnoreShiftType ignoreShiftType) {
        return Config.formatEMC.get() && ignoreShiftType != IgnoreShiftType.NO_FORMAT && (ignoreShiftType == IgnoreShiftType.FORMAT || !Screen.hasShiftDown()) && value.compareTo(FORMAT_START) > -1;
    }

    public static String getShort(BigDecimal value) {
        return getShort(value.toBigInteger());
    }

    static String getShort(BigInteger value) {
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
        String sig = list.get(list.size() - 1);
        String dec = list.get(list.size() - 2).substring(0, 2);
        return String.format("%s.%s", sig, dec);
    }

    public enum IgnoreShiftType {
        NONE,
        FORMAT,
        NO_FORMAT
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

