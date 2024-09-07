package customlowhealthmusic.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.audio.TempMusic;
import customlowhealthmusic.ModFile;

@SpirePatch(clz = TempMusic.class, method = SpirePatch.CONSTRUCTOR, paramtypez = {String.class, boolean.class, boolean.class})
public class TempMusicPatch {
    public static void Postfix(TempMusic instance, String key, boolean isFast, boolean loop) {
        // Store the key of the current temp music in the ModFile
        ModFile.currentTempMusicKey = key;
    }
}
