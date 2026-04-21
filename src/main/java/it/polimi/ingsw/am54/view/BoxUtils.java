package it.polimi.ingsw.am54.view;

public class BoxUtils {

    public static String singleLineBox(String text, int width) {
        String horizontal = "─".repeat(width - 2);
        return "┌" + horizontal + "┐\n│" + center(text, width - 2) + "│\n└" + horizontal + "┘";
    }

    public static String doubleLineBox(String text, int width) {
        String horizontal = "═".repeat(width - 2);
        return "╔" + horizontal + "╗\n║" + center(text, width - 2) + "║\n╚" + horizontal + "╝";
    }

    public static String heavyLineBox(String text, int width) {
        String horizontal = "─".repeat(width - 2);
        return "┍" + horizontal + "┑\n┋" + center(text, width - 2) + "┋\n┕" + horizontal + "┙";
    }

    public static String heavyDoubleLineBox(String text, int width) {
        String horizontal = "═".repeat(width - 2);
        return "┏" + horizontal + "┓\n┃" + center(text, width - 2) + "┃\n┗" + horizontal + "┛";
    }

    public static String roundedBox(String text, int width) {
        String horizontal = "─".repeat(width - 2);
        return "╭" + horizontal + "╮\n│" + center(text, width - 2) + "│\n╰" + horizontal + "╯";
    }

    public static String withTitle(String title, String content, int width) {
        String top = "┌" + "─".repeat(width - 2) + "┐";
        String titleLine = "│ " + title + " ".repeat(width - title.length() - 3) + "│";

        String[] lines = content.split("\n");
        StringBuilder body = new StringBuilder();
        for (String line : lines) {
            body.append("│ ").append(line).append(" ".repeat(width - line.length() - 3)).append("│\n");
        }

        String bottom = "└" + "─".repeat(width - 2) + "┘";
        return top + "\n" + titleLine + "\n" + body + bottom;
    }

    public static String table(String[] headers, String[][] rows) {
        int[] colWidths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            colWidths[i] = headers[i].length();
            for (String[] row : rows) {
                if (i < row.length) {
                    colWidths[i] = Math.max(colWidths[i], row[i].length());
                }
            }
        }

        StringBuilder sb = new StringBuilder();

        String headerLine = "│";
        for (int i = 0; i < headers.length; i++) {
            headerLine += " " + pad(headers[i], colWidths[i]) + " │";
        }
        sb.append(headerLine).append("\n");

        String separator = "├";
        for (int i = 0; i < headers.length; i++) {
            separator += "-".repeat(colWidths[i] + 2) + (i < headers.length - 1 ? "┼" : "┤");
        }
        sb.append(separator).append("\n");

        for (String[] row : rows) {
            String rowLine = "│";
            for (int i = 0; i < headers.length; i++) {
                String cell = i < row.length ? row[i] : "";
                rowLine += " " + pad(cell, colWidths[i]) + " │";
            }
            sb.append(rowLine).append("\n");
        }

        return sb.toString();
    }

    private static String center(String text, int width) {
        int padding = Math.max(0, (width - text.length()) / 2);
        String left = " ".repeat(padding);
        String right = " ".repeat(Math.max(0, width - text.length() - padding));
        return left + text + right;
    }

    private static String pad(String text, int width) {
        return text + " ".repeat(Math.max(0, width - text.length()));
    }

    public static String progressBar(int current, int max, int width, String filled, String empty) {
        if (max <= 0) return "[" + " ".repeat(width) + "]";
        int filledLen = (int) ((double) current / max * width);
        int emptyLen = width - filledLen;
        return "[" + filled.repeat(filledLen) + empty.repeat(emptyLen) + "] " + current + "/" + max;
    }
}