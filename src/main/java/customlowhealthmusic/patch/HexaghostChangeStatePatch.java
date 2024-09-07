package customlowhealthmusic.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.monsters.exordium.Hexaghost;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

@SpirePatch(
        clz = Hexaghost.class,
        method = "changeState"
)
public class HexaghostChangeStatePatch {

    @SpirePrefixPatch
    public static void Prefix(Hexaghost __instance, String stateName) {
        if (stateName.equals("Activate")) {
            // Ensure boss music is re-queued and played
            if (AbstractDungeon.getCurrRoom() instanceof com.megacrit.cardcrawl.rooms.MonsterRoomBoss) {
                CardCrawlGame.music.precacheTempBgm("BOSS_BOTTOM");
            }
        }
    }
}
