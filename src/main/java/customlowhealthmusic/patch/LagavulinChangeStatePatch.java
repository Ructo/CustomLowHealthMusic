package customlowhealthmusic.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.monsters.exordium.Lagavulin;
import com.megacrit.cardcrawl.actions.animations.TalkAction;
import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import customlowhealthmusic.ModFile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SpirePatch(clz = Lagavulin.class, method = "changeState")
public class LagavulinChangeStatePatch {

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(Lagavulin instance, String stateName) {
        try {
            if (stateName.equals("OPEN") && !instance.isDying) {
                // Use reflection to access and modify the private 'isOut' field
                Field isOutField = instance.getClass().getDeclaredField("isOut");
                isOutField.setAccessible(true);
                isOutField.setBoolean(instance, true);

                // Use reflection to call the protected 'updateHitbox' method
                Method updateHitboxMethod = instance.getClass().getSuperclass()
                        .getDeclaredMethod("updateHitbox", float.class, float.class, float.class, float.class);
                updateHitboxMethod.setAccessible(true);
                updateHitboxMethod.invoke(instance, 0.0F, -25.0F, 320.0F, 360.0F);

                // Add game logic for Lagavulin's state change
                AbstractDungeon.actionManager.addToBottom(new TalkAction(instance, Lagavulin.DIALOG[3], 0.5F, 2.0F));
                AbstractDungeon.actionManager.addToBottom(new ReducePowerAction(instance, instance, "Metallicize", 8));
                LagavulinSleepPatch.isAsleep.set(instance, false);

                // Music logic
                if (!ModFile.isPlaying) {
                    CardCrawlGame.music.precacheTempBgm("ELITE");
                    // If low health music is not playing, allow the elite music
                    CardCrawlGame.music.unsilenceBGM();
                    AbstractDungeon.scene.fadeOutAmbiance();
                    CardCrawlGame.music.playPrecachedTempBgm();
                } else {
                    // Low health music is playing, do not play the elite music
                    System.out.println("Low-health music is playing, preventing elite music.");
                }

                // Continue with animations
                instance.state.setAnimation(0, "Coming_out", false);
                instance.state.addAnimation(0, "Idle_2", true, 0.0F);

                // Return early to prevent the original method from running its logic
                return SpireReturn.Return();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // If not returning early, continue to the original method
        return SpireReturn.Continue();
    }
}