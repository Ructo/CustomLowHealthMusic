package customlowhealthmusic.patch;

import basemod.BaseMod;
import basemod.ModPanel;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import customlowhealthmusic.ModFile;

@SpirePatch(
        clz = ModPanel.class,
        method = "update"
)
public class ModPanelUpdatePatch {
    public static void Postfix(ModPanel __instance) {
        if (!BaseMod.modSettingsUp && ModFile.isTesting) {
            ModFile.stopCurrentMusic(); // Stop the preview music if the panel is closed
            ModFile.isTesting = false; // Reset the testing flag
            if (ModFile.playingLabel != null) {
                ModFile.playingLabel.text = ""; // Clear the "Playing..." label
                System.out.println("Playing label reset to empty.");
            }
            System.out.println("Mod config closed, preview music stopped.");
        }
    }
}