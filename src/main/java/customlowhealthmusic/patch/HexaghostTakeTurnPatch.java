package customlowhealthmusic.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.common.ChangeStateAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.exordium.Hexaghost;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

@SpirePatch(
        clz = Hexaghost.class,
        method = "takeTurn"
)
public class HexaghostTakeTurnPatch {

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(Hexaghost __instance) {
        if (__instance.nextMove == 5 && !HexaghostActivationFieldPatch.isActivated.get(__instance)) {


            // Set the activation state to true
            HexaghostActivationFieldPatch.isActivated.set(__instance, true);
            // Perform the activation logic
            AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(__instance, "Activate"));
            // Continue with the original logic
            int d = AbstractDungeon.player.currentHealth / 12 + 1;
            ((DamageInfo) __instance.damage.get(2)).base = d;
            __instance.applyPowers();
            __instance.setMove((byte) 1, AbstractMonster.Intent.ATTACK, ((DamageInfo) __instance.damage.get(2)).base, 6, true);

            // Return to prevent the original method from continuing
            return SpireReturn.Return();
        }

        // Let the original method run if the conditions are not met
        return SpireReturn.Continue();
    }
}
