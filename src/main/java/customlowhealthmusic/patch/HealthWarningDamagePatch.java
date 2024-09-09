package customlowhealthmusic.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import customlowhealthmusic.ModFile;

import static customlowhealthmusic.ModFile.checkPlayerHealth;

@SpirePatch(clz = AbstractPlayer.class, method = "damage")
public class HealthWarningDamagePatch {

    public static void Postfix(AbstractPlayer player, DamageInfo info) {
        if (isInCombat() && player.currentHealth > 0) {
            checkHealthWarning(player);
        }
    }

    private static boolean isInCombat() {
        return AbstractDungeon.getCurrRoom() != null
                && AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT;
    }

    private static void checkHealthWarning(AbstractPlayer player) {
        ModFile.checkPlayerHealth();
    }
}

