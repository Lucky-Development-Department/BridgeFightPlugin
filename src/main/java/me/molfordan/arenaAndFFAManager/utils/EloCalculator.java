package me.molfordan.arenaAndFFAManager.utils;

public class EloCalculator {

    private static final int K_FACTOR = 32;

    /**
     * Calculates the new rating for a player.
     * @param rating Current rating of the player
     * @param opponentRating Current rating of the opponent
     * @param score 1 for win, 0 for loss, 0.5 for draw
     * @return New rating
     */
    public static int calculateNewRating(int rating, int opponentRating, double score) {
        double expectedScore = 1.0 / (1.0 + Math.pow(10, (double) (opponentRating - rating) / 400.0));
        return (int) Math.round(rating + K_FACTOR * (score - expectedScore));
    }
}
