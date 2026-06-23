package me.molfordan.bridgefightplugin.cosmetics.objects;

import org.bukkit.Effect;

public class Trail {
    private final String id;
    private final String displayName;
    private final String particle;
    private final int requiredBalance;
    private final String permission;
    private final CosmeticTier tier;
    private Effect effect;

    public Trail(String id, String displayName, String particle, int requiredBalance, String permission, CosmeticTier tier) {
        this.id = id;
        this.displayName = displayName;
        this.particle = particle;
        this.requiredBalance = requiredBalance;
        this.permission = permission;
        this.tier = tier;
        try {
            this.effect = Effect.valueOf(particle);
        } catch (Exception e) {
            this.effect = null;
        }
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getParticle() { return particle; }
    public int getRequiredBalance() { return requiredBalance; }
    public String getPermission() { return permission; }
    public Effect getEffect() { return effect; }
    public CosmeticTier getTier() { return tier; }
}
