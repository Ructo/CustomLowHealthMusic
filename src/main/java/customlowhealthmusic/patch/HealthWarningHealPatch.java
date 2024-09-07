package customlowhealthmusic.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

import static customlowhealthmusic.ModFile.checkPlayerHealth;

@SpirePatch(clz = AbstractPlayer.class, method = "heal")
public class HealthWarningHealPatch {

    public static void Postfix(AbstractPlayer player, int healAmount) {
        if (isInCombat()) {
            checkHealthWarning(player);
        }
    }

    private static boolean isInCombat() {
        return AbstractDungeon.getCurrRoom() != null
                && AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT;
    }

    private static void checkHealthWarning(AbstractPlayer player) {
        checkPlayerHealth();

    }
}


