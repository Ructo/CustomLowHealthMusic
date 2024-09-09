package customlowhealthmusic.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.ending.SpireShield;
import com.megacrit.cardcrawl.monsters.ending.SpireSpear;
import com.megacrit.cardcrawl.monsters.exordium.Lagavulin;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import customlowhealthmusic.ModFile;

@SpirePatch(
        clz = MonsterRoomElite.class,
        method = "onPlayerEntry"
)
public class MonsterRoomEliteMusicPatch {

    @SpirePostfixPatch
    public static void Postfix(MonsterRoomElite __instance) {
        ModFile.checkPlayerHealth();

        if (ModFile.isPlaying) {
            // Prevent the elite music from overriding the low-health music
            System.out.println("Low health music is active, skipping elite room music.");
            return;
        }

        // Existing elite music logic
        boolean isLagavulinPresent = false;
        boolean isLagavulinAsleep = false;
        boolean isSpearOrShieldPresent = false;

        for (AbstractMonster monster : __instance.monsters.monsters) {
            if (monster instanceof Lagavulin) {
                isLagavulinPresent = true;
                isLagavulinAsleep = LagavulinSleepPatch.isLagavulinAsleep((Lagavulin) monster);
                break;
            } else if (monster instanceof SpireSpear || monster instanceof SpireShield) {
                isSpearOrShieldPresent = true;
            }
        }

        if (CardCrawlGame.isInARun() && AbstractDungeon.currMapNode != null && AbstractDungeon.actionManager != null) {
            if (isLagavulinPresent) {
                if (!isLagavulinAsleep) {
                    // Lagavulin is awake, play the elite music
                    CardCrawlGame.music.precacheTempBgm("STS_EliteBoss_NewMix_v1.ogg");
                } else {
                    // Lagavulin is asleep, silence the music
                    CardCrawlGame.music.silenceTempBgmInstantly();
                    CardCrawlGame.music.silenceBGM();
                }
            } else if (isSpearOrShieldPresent) {
                // Play special Act 4 music for Spire Spear/Shield
                CardCrawlGame.music.playTempBgmInstantly("STS_Act4_BGM_v2.ogg", true);
            } else {
                // Play the standard elite music
                CardCrawlGame.music.playTempBgmInstantly("STS_EliteBoss_NewMix_v1.ogg", true);
            }
        }
    }
}