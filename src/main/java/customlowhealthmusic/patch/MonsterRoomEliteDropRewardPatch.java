package customlowhealthmusic.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;

import java.lang.reflect.Method;

@SpirePatch(
        clz = MonsterRoomElite.class,
        method = "dropReward"
)
public class MonsterRoomEliteDropRewardPatch {
    private static float musicDelayTimer = 2.5f;
    @SpirePrefixPatch
    public static SpireReturn<Void> dropReward(MonsterRoomElite __instance) {
        // Custom reward logic
        AbstractRelic.RelicTier tier = returnRandomRelicTier();
        if (Settings.isEndless && AbstractDungeon.player.hasBlight("MimicInfestation")) {
            AbstractDungeon.player.getBlight("MimicInfestation").flash();
        } else {
            __instance.addRelicToRewards(tier);
            if (AbstractDungeon.player.hasRelic("Black Star")) {
                __instance.addNoncampRelicToRewards(returnRandomRelicTier());
            }
            try {
                // Access the private method via reflection
                Method addEmeraldKey = MonsterRoomElite.class.getDeclaredMethod("addEmeraldKey");
                addEmeraldKey.setAccessible(true);
                addEmeraldKey.invoke(__instance);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Custom music logic
        CardCrawlGame.music.fadeOutTempBGM();
        musicDelayTimer = 2.5f;
        CardCrawlGame.music.unsilenceBGM();

        // Prevent the original dropReward method from running
        return SpireReturn.Return();
    }

    // Method to replace returnRandomRelicTier functionality
    private static AbstractRelic.RelicTier returnRandomRelicTier() {
        int roll = AbstractDungeon.relicRng.random(0, 99);
        if (AbstractDungeon.player.hasBlight("Elite Swarm")) {
            roll += 10;
        }
        if (roll < 50) {
            return AbstractRelic.RelicTier.COMMON;
        } else if (roll > 82) {
            return AbstractRelic.RelicTier.RARE;
        } else {
            return AbstractRelic.RelicTier.UNCOMMON;
        }
    }
}
