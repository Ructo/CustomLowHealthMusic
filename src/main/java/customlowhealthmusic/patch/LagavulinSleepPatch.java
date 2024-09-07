package customlowhealthmusic.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.monsters.exordium.Lagavulin;

@SpirePatch(
        clz = Lagavulin.class,
        method = SpirePatch.CLASS
)
public class LagavulinSleepPatch {
    // SpireField to hold the sleep state
    public static SpireField<Boolean> isAsleep = new SpireField<>(() -> false);

    @SpirePatch(
            clz = Lagavulin.class,
            method = SpirePatch.CONSTRUCTOR,
            paramtypez = {boolean.class}
    )
    public static class LagavulinConstructorPatch {
        @SpirePrefixPatch
        public static void Prefix(Lagavulin __instance, boolean setAsleep) {
            isAsleep.set(__instance, setAsleep);
        }
    }

    // Public static method to get the sleep state
    public static boolean isLagavulinAsleep(Lagavulin lagavulin) {
        return isAsleep.get(lagavulin);
    }
}
