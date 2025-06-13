package cool.furry.mc.neoforge.projectexpansion.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

// https://github.com/FisheyLP/TableGenerator/blob/f6f75f5e41fdeb1ecd32d94c1510f83c1f13c64f/TableGenerator.java
// https://www.spigotmc.org/threads/help-making-a-chat-table-based.170306
public class TableGenerator {
    private static final List<Character> char7 = Arrays.asList('°', '~', '@');
    private static final List<Character> char5 = Arrays.asList('"', '{', '}', '(', ')', '*', 'f', 'k', '<', '>');
    private static final List<Character> char4 = Arrays.asList('I', 't', ' ', '[', ']', '€');
    private static final List<Character> char3 = Arrays.asList('l', '`', '³', '\'');
    private static final List<Character> char2 = Arrays.asList(',', '.', '!', 'i', '´', ':', ';', '|');
    private static final char char1 = '\u17f2';
    private final Alignment[] alignments;
    private final ArrayList<Row> table = new ArrayList<>();
    private final int columns;

    public TableGenerator(Alignment... alignments) {
        if (alignments.length < 1 || (Arrays.stream(alignments).allMatch(Objects::isNull))) {
            throw new IllegalArgumentException("Must provide at least 1 alignment.");
        }

        this.columns = alignments.length;
        this.alignments = alignments;
    }

    public ArrayList<ArrayList<Component>> generate(Receiver receiver) {
        Integer[] columWidths = new Integer[columns];

        for (Row r : table) {
            for (int i = 0; i < columns; i++) {
                Component component = r.components.get(i);
                int length = getCustomLength(component, receiver);

                if (columWidths[i] == null) {
                    columWidths[i] = length;
                } else if (length > columWidths[i]) {
                    columWidths[i] = length;
                }
            }
        }

        ArrayList<ArrayList<Component>> lines = new ArrayList<>();

        for (Row r : table) {
            ArrayList<Component> list = new ArrayList<>();

            if (r.empty) {
                list.add(Component.empty());
                continue;
            }

            for (int i = 0; i < columns; i++) {
                Alignment alignment = alignments[i];
                Component component = r.components.get(i);
                int length = getCustomLength(component, receiver);
                int empty = columWidths[i] - length;
                int spacesAmount = empty;
                if (receiver == Receiver.CLIENT) spacesAmount = (int) Math.floor(empty / 4d);
                int char1Amount = 0;
                if (receiver == Receiver.CLIENT) char1Amount = empty - 4 * spacesAmount;

                String spaces = concatChars(' ', spacesAmount);
                Component char1s = Component.literal(concatChars(char1, char1Amount)).withStyle(ChatFormatting.DARK_GRAY);

                if (alignment == Alignment.LEFT) {
                    MutableComponent mutable = component.copy();
                    if (i < columns - 1) mutable.append(char1s).append(spaces);
                    list.add(mutable);
                }
                if (alignment == Alignment.RIGHT) {
                    list.add(Component.literal(spaces).append(char1s).append(component));
                }
                if (alignment == Alignment.CENTER) {
                    int leftAmount = empty / 2;
                    int rightAmount = empty - leftAmount;

                    int spacesLeftAmount = leftAmount;
                    int spacesRightAmount = rightAmount;
                    if (receiver == Receiver.CLIENT) {
                        spacesLeftAmount = (int) Math.floor(spacesLeftAmount / 4d);
                        spacesRightAmount = (int) Math.floor(spacesRightAmount / 4d);
                    }

                    int char1LeftAmount = 0;
                    int char1RightAmount = 0;
                    if (receiver == Receiver.CLIENT) {
                        char1LeftAmount = leftAmount - 4 * spacesLeftAmount;
                        char1RightAmount = rightAmount - 4 * spacesRightAmount;
                    }

                    String spacesLeft = concatChars(' ', spacesLeftAmount);
                    String spacesRight = concatChars(' ', spacesRightAmount);
                    Component char1Left = Component.literal(concatChars(char1, char1LeftAmount)).withStyle(ChatFormatting.DARK_GRAY);
                    Component char1Right = Component.literal(concatChars(char1, char1RightAmount)).withStyle(ChatFormatting.DARK_GRAY);

                    MutableComponent mutable = Component.literal(spacesLeft).append(char1Left).append(component);
                    if (i < columns - 1) mutable.append(char1Right).append(spacesRight);
                    list.add(mutable);
                }
            }

            lines.add(list);
        }
        return lines;
    }

    public void eachRow(Receiver receiver, Consumer<ArrayList<Component>> supplier) {
        ArrayList<ArrayList<Component>> lines = generate(receiver);

        for (ArrayList<Component> list : lines) {
            supplier.accept(list);
        }
    }

    protected static int getCustomLength(Component component, Receiver receiver) {
        return getCustomLength(component.getString(), receiver);
    }

    protected static int getCustomLength(String text, Receiver receiver) {
        if (receiver == Receiver.CONSOLE) return text.length();

        int length = 0;
        for (char c : text.toCharArray()) {
            length += getCustomCharLength(c);
        }

        return length;
    }

    protected static int getCustomCharLength(char c) {
        if (char1 == c) return 1;
        if (char2.contains(c)) return 2;
        if (char3.contains(c)) return 3;
        if (char4.contains(c)) return 4;
        if (char5.contains(c)) return 5;
        if (char7.contains(c)) return 7;

        return 6;
    }

    protected String concatChars(char c, int length) {
        StringBuilder s = new StringBuilder();
        if (length < 1) return s.toString();

        s.append(String.valueOf(c).repeat(length));
        return s.toString();
    }

    public void addRow(Component... components) {
        if (Arrays.stream(components).allMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Components must not be null.");
        }
        if (components.length > columns) {
            throw new IllegalArgumentException("Too big for the table.");
        }

        Row r = new Row(components);

        table.add(r);
    }

    private class Row {
        public ArrayList<Component> components = new ArrayList<>();
        public boolean empty = true;

        public Row(Component... components) {
            if (components.length == 0 || Arrays.stream(components).allMatch(Objects::isNull)) {
                for (int i = 0; i < columns; i++)
                    this.components.add(Component.empty());
                return;
            }

            for (Component component : components) {
                if (component != null && !component.getString().isEmpty()) empty = false;

                this.components.add(component);
            }

            for (int i = 0; i < columns; i++) {
                if (i >= components.length) this.components.add(Component.empty());
            }
        }
    }

    public enum Receiver {
        CONSOLE, CLIENT
    }

    public enum Alignment {
        CENTER, LEFT, RIGHT
    }
}