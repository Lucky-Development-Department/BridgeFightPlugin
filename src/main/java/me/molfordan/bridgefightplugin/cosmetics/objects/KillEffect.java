package me.molfordan.bridgefightplugin.cosmetics.objects;

import org.bukkit.Effect;

public class KillEffect {
    private final String id;
    private final String displayName;
    private final String effectType;
    private final int requiredBalance;
    private final String permission;
    private Effect bukkitEffect;
    private SpecialEffect specialEffect;

    public enum SpecialEffect {
        NONE, REDSTONE, LIGHTNING, FIREWORK
    }

    public KillEffect(String id, String displayName, String effectType, int requiredBalance, String permission) {
        this.id = id;
        this.displayName = displayName;
        this.effectType = effectType;
        this.requiredBalance = requiredBalance;
        this.permission = permission;
        
        String upper = effectType.toUpperCase();
        switch (upper) {
            case "REDSTONE":
                this.specialEffect = SpecialEffect.REDSTONE;
                break;
            case "LIGHTNING":
                this.specialEffect = SpecialEffect.LIGHTNING;
                break;
            case "FIREWORK":
                this.specialEffect = SpecialEffect.FIREWORK;
                break;
            case "NONE":
                this.specialEffect = SpecialEffect.NONE;
                break;
            default:
                try {
                    this.bukkitEffect = Effect.valueOf(upper);
                    this.specialEffect = null;
                } catch (Exception e) {
                    this.specialEffect = SpecialEffect.NONE;
                }
                break;
        }
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getEffectType() { return effectType; }
    public int getRequiredBalance() { return requiredBalance; }
    public String getPermission() { return permission; }
    public Effect getBukkitEffect() { return bukkitEffect; }
    public SpecialEffect getSpecialEffect() { return specialEffect; }
}
