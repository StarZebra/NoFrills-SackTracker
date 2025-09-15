package nofrills.features.mining;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import nofrills.Main;
import nofrills.config.Feature;
import nofrills.config.SettingBool;
import nofrills.config.SettingEnum;
import nofrills.events.ChatMsgEvent;
import nofrills.events.ServerTickEvent;
import nofrills.misc.TrackerSession;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SackTracker {
    public static final Feature instance = new Feature("sacktracker");

    public static final SettingEnum<supportedCollections> selectedCollection = new SettingEnum<>(supportedCollections.diamond, supportedCollections.class, "collection", instance.key());
    public static final SettingBool hideRates = new SettingBool(false, "hideRates", instance.key());
    public static final SettingBool hideEfficiency = new SettingBool(false, "hideEff", instance.key());
    public static final SettingBool hideTime = new SettingBool(false, "hideTime", instance.key());

    private static final int ENCHANTED = 160;
    private static final int REMOVED_INDEX = 3;
    private static final int GAINED_INDEX = 0;
    private static final long TIMEOUT_NANOS = 60000000000L;

    private static String displayedCPH = "";
    private static String displayedTotal = "";
    private static String displayedTime = "";
    private static String displayedEfficiency = "";

    public static final MutableText displayNone = Text.literal("§bCollection Tracker\n§7Currently not tracking...");
    public static MutableText display = displayNone;

    private static long tick = 0;

    @EventHandler
    private static void onTick(ServerTickEvent event){
        if(tick % 20 == 0) {
            if(isSessionActive()){
                refreshDisplay();
                if(Main.session.isPaused()) return;

                if(!selectedCollection.value().toString().equalsIgnoreCase(Main.session.getTrackedCollection())){
                    Main.session.stop();
                    return;
                }

                if(System.nanoTime() - Main.session.getLastSackMessageNanos() >= TIMEOUT_NANOS){
                    Main.session.stop();
                    return;
                }

                displayedTime = String.format("%,d", Main.session.getElapsedSeconds()) + "s";

            }
        }
        tick++;
    }

    @SuppressWarnings("unused")
    @EventHandler
    private static void onChat(ChatMsgEvent event) {
        if (Main.mc.player != null && instance.isActive()) {
            if (isSackMessage(event.getPlainMessage())) {

                int timeIndex = event.messagePlain.indexOf("Last");
                String str = event.messagePlain.substring(timeIndex, event.messagePlain.lastIndexOf("s"));
                int seconds = Integer.parseInt(str.split("t")[1].trim());

                String trackedColl = selectedCollection.value().toString();

                int removedAmount = getAmountFromMessage(event.getMessage(), trackedColl, REMOVED_INDEX);
                int gainedAmount = getAmountFromMessage(event.getMessage(), trackedColl, GAINED_INDEX);
                int diff = gainedAmount - removedAmount;

                if (diff == 0) {
                    Main.LOGGER.info("Caught prohibited action, ignoring values.");
                    return;
                } else if (diff > 0) {
                    gainedAmount = diff;
                }

                if (!isSessionActive()) {
                    Main.session = new TrackerSession(trackedColl);
                    Main.session.start(seconds);
                }

                if (Main.session.isPaused()) return;

                Main.session.increaseTrackedSeconds(seconds);
                Main.session.increaseTotalItems(gainedAmount);

                Main.session.updateSackMessage();
                refreshDisplay();

                displayedTotal = String.format("%,d", Main.session.getTotalItemsGained());
                displayedCPH = String.format("%,d", Main.session.getCollectionPerHour()) +"/h";
                displayedEfficiency = new DecimalFormat("#.##").format(Main.session.getUptime() * 100) + "%";

            }
        }
    }

    public static void clearLines(){
        display = displayNone;
        displayedTotal = "";
        displayedCPH = "";
        displayedEfficiency = "";
        displayedTime = "";
    }

    public static void refreshDisplay(){
        List<String> lines = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        builder.append("§bCollection Tracker");
        lines.add("§bDiamond: " + displayedTotal);
        if(!hideRates.value()) lines.add("§bRates: " + displayedCPH);
        if(!hideEfficiency.value()) lines.add("§bEfficiency: " + displayedEfficiency);
        if(!hideTime.value()) lines.add("§bTime Elapsed: " + displayedTime);
        if(Main.session.isPaused()) lines.add("§c§lPAUSED");
        for(String line : lines){
            builder.append("\n").append(line);
        }
        display = Text.literal(builder.toString());
    }

    private static boolean isSackMessage(String message){
        return message.startsWith("[Sacks] ");
    }

    private static int getAmountFromMessage(Text message, String trackedColl, int index){
        int totalAmount = 0;
        Style style = message.getSiblings().get(index).getStyle();
        if(style.getHoverEvent() == null) return 0;
        if(style.getColor() == null) return 0;
        if(index == 0 && style.getColor().getHexCode().equals("#FF5555")) return 0;
        HoverEvent hoverEvent = style.getHoverEvent();
        if(hoverEvent == null) return 0;
        HoverEvent.ShowText showText = (HoverEvent.ShowText) hoverEvent;
        List<Text> siblings = showText.value().getSiblings();

        for (int i = 0; i < siblings.size(); i++) {
            Text sibling = siblings.get(i);
            if(sibling.getString().toLowerCase().contains(trackedColl)){
                int amount = Integer.parseInt(siblings.get(i - 1).getString().trim().replaceAll("[,\\-]", ""));
                String item = sibling.getString();
                if(item.startsWith("Enchanted ")){
                    amount *= ENCHANTED;
                    if(item.endsWith(" Block")){
                        amount *= ENCHANTED;
                    }
                }
                totalAmount += amount;
            }
        }
        return totalAmount;
    }

    public static boolean isSessionActive(){
        return Main.session != null && Main.session.isActive();
    }

    public enum supportedCollections {
        diamond,
        gold,
        iron,
        emerald,
        coal,
        redstone,
        lapis,
        cobblestone,
        glacite,
        umber,
        tungsten,
        gravel,
        glowstone,
        ice,
        mithril,
        mycelium,
        quartz,
        netherrack,
        obsidian,
        sand
    }

}
