package me.molfordan.arenaAndFFAManager.utils;

public class DurationParser {

    public static long parseDuration(String input) {
        if (input == null) return -1;

        input = input.toLowerCase();

        long multiplier;

        if (input.endsWith("s")) multiplier = 1000L;
        else if (input.endsWith("min")) multiplier = 60_000L;
        else if (input.endsWith("h")) multiplier = 3_600_000L;
        else if (input.endsWith("d")) multiplier = 86_400_000L;
        else if (input.endsWith("w")) multiplier = 604_800_000L;
        else if (input.endsWith("mo")) multiplier = 2_592_000_000L; // 30 days
        else if (input.endsWith("y")) multiplier = 31_536_000_000L; // 365 days
        else return -1;

        String numberPart = input.replaceAll("[^0-9]", "");
        if (numberPart.isEmpty()) return -1;

        long value = Long.parseLong(numberPart);
        return value * multiplier;
    }
}
