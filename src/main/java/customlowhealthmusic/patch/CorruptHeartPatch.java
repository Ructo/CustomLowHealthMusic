package customlowhealthmusic.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.ending.CorruptHeart;
import com.megacrit.cardcrawl.powers.BeatOfDeathPower;
import com.megacrit.cardcrawl.powers.InvinciblePower;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import customlowhealthmusic.ModFile;

@SpirePatch(
        clz = CorruptHeart.class,
        method = "usePreBattleAction"
)
public class CorruptHeartPatch {

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(CorruptHeart __instance) {
        // Replace the original music and unsilence logic
        CardCrawlGame.music.precacheTempBgm("BOSS_ENDING");

        // Check for player's low health and handle low health music if necessary
        ModFile.checkPlayerHealth();

        // Replicate the rest of the original method logic
        int invincibleAmt = 300;
        if (AbstractDungeon.ascensionLevel >= 19) {
            invincibleAmt -= 100;
        }

        int beatAmount = 1;
        if (AbstractDungeon.ascensionLevel >= 19) {
            ++beatAmount;
        }

        // Apply powers as in the original method
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(__instance, __instance, new InvinciblePower(__instance, invincibleAmt), invincibleAmt));
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(__instance, __instance, new BeatOfDeathPower(__instance, beatAmount), beatAmount));

        // Return early to prevent the original method from running
        return SpireReturn.Return();
    }
}
