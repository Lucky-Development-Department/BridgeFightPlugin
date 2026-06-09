package me.molfordan.bridgefightplugin.queue.enums;

public enum QueueType {
    SOLO_UNRANKED(MatchType.SOLO, StatisticType.UNRANKED, 2),
    SOLO_RANKED(MatchType.SOLO, StatisticType.RANKED, 2),
    DUO_UNRANKED(MatchType.DUO, StatisticType.UNRANKED, 4),
    DUO_RANKED(MatchType.DUO, StatisticType.RANKED, 4),
    DUEL(MatchType.SOLO, StatisticType.NONE, 2),
    PARTY_FIGHT(MatchType.PARTY, StatisticType.NONE, 2),
    PARTY_SPLIT(MatchType.PARTY, StatisticType.NONE, 2),
    PARTY_DUO_QUEUE(MatchType.DUO, StatisticType.UNRANKED, 4);

    private final MatchType matchType;
    private final StatisticType statisticType;
    private final int minPlayers;

    QueueType(MatchType matchType, StatisticType statisticType, int minPlayers) {
        this.matchType = matchType;
        this.statisticType = statisticType;
        this.minPlayers = minPlayers;
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
}
