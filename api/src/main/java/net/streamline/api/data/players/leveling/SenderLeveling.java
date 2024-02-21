package net.streamline.api.data.players.leveling;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.data.IUuidable;
import net.streamline.api.data.console.StreamSender;
import net.streamline.api.data.players.events.LevelChangeEvent;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.modules.ModuleUtils;
import tv.quaint.utils.MathUtils;

@Getter @Setter
public class SenderLeveling implements IUuidable {
    private String uuid;

    private StreamSender sender;

    private int level;
    private double totalExperience;
    private double currentExperience;

    private String equationString;
    private int startedLevel;
    private double startedExperience;

    public SenderLeveling(StreamSender sender, int startedLevel, double startedExperience, String equationString) {
        this.uuid = sender.getUuid();
        this.sender = sender;
        this.level = startedLevel;
        this.totalExperience = startedExperience;
        this.currentExperience = startedExperience;
        this.equationString = equationString;
        this.startedLevel = startedLevel;
        this.startedExperience = startedExperience;
    }

    public SenderLeveling(StreamSender sender) {
        this(sender, getGivenStartingLevel(), getGivenStartingExperience(), getGivenEquationString());
    }

    public static int getGivenStartingLevel() {
        return GivenConfigs.getMainConfig().playerStartingLevel();
    }

    public static double getGivenStartingExperience() {
        return GivenConfigs.getMainConfig().playerStartingExperienceAmount();
    }

    public static String getGivenEquationString() {
        return GivenConfigs.getMainConfig().playerLevelingEquation();
    }

    public void addExperience(double experience) {
        this.totalExperience += experience;
        this.currentExperience += experience;

        performLevelCheck();
    }

    public void removeExperience(double experience) {
        this.totalExperience -= experience;
        this.currentExperience -= experience;

        performLevelCheck();
    }

    public boolean needsLevelDown() {
        return this.currentExperience < 0;
    }

    public void performLevelCheck() {
        while (canLevelUpTo(this.level + 1)) {
            performLevelUp();
        }
        while (needsLevelDown()) {
            this.level--;
            double temp = this.currentExperience;
            this.currentExperience = getNeededExperience(this.sender, this.level) - temp;
        }
    }

    public boolean canLevelUpTo(int level) {
        return this.currentExperience >= getNeededExperience(this.sender, level);
    }

    public void performLevelUp() {
        level++;
        currentExperience -= getNeededExperience(this.sender, level);

        LevelChangeEvent event = new LevelChangeEvent(this.sender, this.level, this.level - 1);
        event.fire();
    }

    public void performLevelDown() {
        level--;
        currentExperience += getNeededExperience(this.sender, level);

        LevelChangeEvent event = new LevelChangeEvent(this.sender, this.level, this.level + 1);
        event.fire();
    }

    public static double getNeededExperience(StreamSender sender, int level) {
        return MathUtils.eval(getFinalEquationString(sender, level));
    }

    public static double getNeededExperience(StreamSender sender) {
        return getNeededExperience(sender, sender.getLeveling().getLevel() + 1);
    }

    public static String getFinalEquationString(StreamSender sender, int level) {
        return ModuleUtils.replaceAllPlayerBungee(sender,
                sender.getLeveling().getEquationString().replace("%streamline_user_level%", String.valueOf(level))
        );
    }

    public static double getExperienceNeededForNextLevel(StreamPlayer player) {
        return getNeededExperience(player) - player.getLeveling().getCurrentExperience();
    }

    public void setExperience(double amount) {
        this.level = this.startedLevel;
        this.totalExperience = amount;
        this.currentExperience = amount;

        performLevelCheck();
    }
}
