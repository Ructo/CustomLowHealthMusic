package customlowhealthmusic.patch;

import basemod.BaseMod;
import basemod.ModPanel;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;

@SpirePatch(
        clz = ModPanel.class,
        method = "update"
)
public class ModPanelPatch {

    private static boolean wasSettingsPanelOpen = false;

    @SpirePrefixPatch
    public static void Prefix(ModPanel __instance) {
        // Logic when settings panel closes
        if (!BaseMod.modSettingsUp && wasSettingsPanelOpen) {
            System.out.println("Settings panel was closed.");
            if (customlowhealthmusic.ModFile.isPlaying || customlowhealthmusic.ModFile.isTesting) {
                customlowhealthmusic.ModFile.stopCurrentMusic(); // Stop the music when the settings panel is closed
                customlowhealthmusic.ModFile.isTesting = false;  // Reset the testing flag
            }
            if (customlowhealthmusic.ModFile.playingLabel != null) {
                customlowhealthmusic.ModFile.playingLabel.text = ""; // Clear the "Playing..." label when the panel is closed
            }
            wasSettingsPanelOpen = false; // Update panel state
        }

        // Logic when settings panel opens
        if (BaseMod.modSettingsUp && !wasSettingsPanelOpen) {
            System.out.println("Settings panel was opened.");
            wasSettingsPanelOpen = true; // Update panel state
            if (customlowhealthmusic.ModFile.playingLabel != null) {
                if (customlowhealthmusic.ModFile.isTesting) {
                    customlowhealthmusic.ModFile.playingLabel.text = "Playing..."; // Show the label if testing
                } else {
                    customlowhealthmusic.ModFile.playingLabel.text = ""; // Clear the label otherwise
                }
            }
        }
    }
}
