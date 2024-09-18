package customlowhealthmusic.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import customlowhealthmusic.ModFile;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.megacrit.cardcrawl.monsters.ending.CorruptHeart;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import javassist.CannotCompileException;

@SpirePatch(
        clz = CorruptHeart.class,
        method = "usePreBattleAction"
)
public class CorruptHeartPatch {
    @SpireInstrumentPatch
    public static ExprEditor Instrument() {
        return new ExprEditor() {
            boolean insertedCheck = false;
            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                if (!insertedCheck) {
                    // Insert ModFile.checkPlayerHealth() at the very beginning
                    m.replace("{" +
                            ModFile.class.getName() + ".checkPlayerHealth();" +
                            "$_ = $proceed($$);" +
                            "}");
                    insertedCheck = true;
                    return;
                }
                // Target the unsilenceBGM method
                if (m.getClassName().equals("com.megacrit.cardcrawl.audio.MainMusic") && m.getMethodName().equals("unsilenceBGM")) {
                    m.replace("{ if (!" + ModFile.class.getName() + ".isPlaying) { $_ = $proceed($$); } }");
                }
                // Target the fadeOutAmbiance method
                else if (m.getClassName().equals("com.megacrit.cardcrawl.scenes.AbstractScene") && m.getMethodName().equals("fadeOutAmbiance")) {
                    m.replace("{ if (!" + ModFile.class.getName() + ".isPlaying) { $_ = $proceed($$); } }");
                }
                // Target the playBgmInstantly method
                else if (m.getClassName().equals("com.megacrit.cardcrawl.rooms.AbstractRoom") && m.getMethodName().equals("playBgmInstantly")) {
                    m.replace("{ if (!" + ModFile.class.getName() + ".isPlaying) { $_ = $proceed($$); } }");
                }
            }
        };
    }
}
