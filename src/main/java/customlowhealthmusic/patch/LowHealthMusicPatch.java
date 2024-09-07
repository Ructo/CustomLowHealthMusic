package customlowhealthmusic.patch;

import com.badlogic.gdx.audio.Music;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.audio.MainMusic;
import com.megacrit.cardcrawl.audio.TempMusic;
import customlowhealthmusic.ModFile;

import java.io.File;

@SpirePatch(
        clz = TempMusic.class,
        method = "getSong"
)
public class LowHealthMusicPatch {

    public static String warningIntroFileName = "default_warningintro"; // Default fallback

    public static void assignWarningIntro(String fileName) {
        warningIntroFileName = fileName;
    }

    @SpirePrefixPatch
    public static SpireReturn<Music> Prefix(TempMusic __instance, String key) {
        if (key.equals("warningintro")) {
            // Construct the full path to the music file
            String filePath = ModFile.getCustomMusicFolderPath() + File.separator + warningIntroFileName;
            return SpireReturn.Return(MainMusic.newMusic(filePath));
        }
        return SpireReturn.Continue();
    }
}
