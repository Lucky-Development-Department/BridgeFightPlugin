package me.molfordan.bridgefightplugin.queue.enums;

public enum QueueType {
    SOLO_UNRANKED(MatchType.SOLO, StatisticType.UNRANKED, 2, "Solo Unranked"),
    SOLO_RANKED(MatchType.SOLO, StatisticType.RANKED, 2, "Solo Ranked"),
    DUO_UNRANKED(MatchType.DUO, StatisticType.UNRANKED, 4, "Duo Unranked"),
    DUO_RANKED(MatchType.DUO, StatisticType.RANKED, 4, "Duo Ranked"),
    DUEL(MatchType.SOLO, StatisticType.NONE, 2, "Duel"),
    PARTY_FIGHT(MatchType.PARTY, StatisticType.NONE, 2, "Party Fight"),
    PARTY_SPLIT(MatchType.PARTY, StatisticType.NONE, 2, "Party Split"),
    PARTY_DUO_QUEUE(MatchType.DUO, StatisticType.UNRANKED, 4, "Party Duo Queue");

    private final MatchType matchType;
    private final StatisticType statisticType;
    private final int minPlayers;
    private final String displayName;

    QueueType(MatchType matchType, StatisticType statisticType, int minPlayers, String displayName) {
        this.matchType = matchType;
        this.statisticType = statisticType;
        this.minPlayers = minPlayers;
        this.displayName = displayName;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public StatisticType getStatisticType() {
        return statisticType;
    }
    
    public int getMinPlayers() {
        return minPlayers;
    }

    public String getDisplayName() {
        return displayName;
    }
}
