package singularity.holders.builtin;

public interface CosmicGeyserHolder {
    public boolean isBedrockUUID(String uuid);

    public boolean isBedrockName(String name);

    public String getBedrockPrefix();

    public String getUsernameFromBedrockUUID(String uuid);

    public String getBedrockUUIDFromUsername(String name);
}
