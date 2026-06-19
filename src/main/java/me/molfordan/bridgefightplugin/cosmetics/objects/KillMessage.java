package me.molfordan.bridgefightplugin.cosmetics.objects;

public class KillMessage {
    private final String id;
    private final String displayName;
    private final String message;
    private final String voidMessage;
    private final int requiredBalance;
    private final String permission;

    public KillMessage(String id, String displayName, String message, String voidMessage, int requiredBalance, String permission) {
        this.id = id;
        this.displayName = displayName;
        this.message = message;
        this.voidMessage = voidMessage;
        this.requiredBalance = requiredBalance;
        this.permission = permission;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getMessage() { return message; }
    public String getVoidMessage() { return voidMessage; }
    public int getRequiredBalance() { return requiredBalance; }
    public String getPermission() { return permission; }
}
