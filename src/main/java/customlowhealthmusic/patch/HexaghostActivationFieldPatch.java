package customlowhealthmusic.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.monsters.exordium.Hexaghost;

@SpirePatch(
        clz = Hexaghost.class,
        method = SpirePatch.CLASS
)
public class HexaghostActivationFieldPatch {
    // SpireField to hold the activation state
    public static SpireField<Boolean> isActivated = new SpireField<>(() -> false);

    // Patch the constructor to set the activation state
    @SpirePatch(
            clz = Hexaghost.class,
            method = SpirePatch.CONSTRUCTOR
    )
    public static class HexaghostConstructorPatch {
        @SpirePrefixPatch
        public static void Prefix(Hexaghost __instance) {
            // Set initial activation state (e.g., false if starting unactivated)
            isActivated.set(__instance, false);
        }
    }
}
