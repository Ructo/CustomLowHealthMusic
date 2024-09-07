package customlowhealthmusic.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.monsters.exordium.Lagavulin;

@SpirePatch(
        clz = Lagavulin.class,
        method = "changeState"
)
public class LagavulinChangeStatePatch {

    @SpirePrefixPatch
    public static void Prefix(Lagavulin __instance, String stateName) {
        // Check if the state is changing to "OPEN"
        if (stateName.equals("OPEN")) {
            // Update the sleep state to false (awake)
            LagavulinSleepPatch.isAsleep.set(__instance, false);
            CardCrawlGame.music.precacheTempBgm("STS_EliteBoss_NewMix_v1.ogg");
        }
    }
}
