package customlowhealthmusic;

import basemod.*;
import basemod.interfaces.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.ConfigUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.beyond.MindBloom;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.exordium.Lagavulin;
import com.megacrit.cardcrawl.rooms.*;
import com.megacrit.cardcrawl.screens.DeathScreen;
import com.megacrit.cardcrawl.screens.options.DropdownMenu;
import com.megacrit.cardcrawl.screens.options.DropdownMenuListener;
import customlowhealthmusic.patch.HexaghostActivationFieldPatch;
import customlowhealthmusic.patch.LagavulinSleepPatch;
import customlowhealthmusic.patch.LowHealthMusicPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import customlowhealthmusic.util.ModSliderBetter;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

@SpireInitializer
public class ModFile implements
        EditCardsSubscriber,
        EditRelicsSubscriber,
        EditStringsSubscriber,
        EditKeywordsSubscriber,
        PostBattleSubscriber,
        OnStartBattleSubscriber,
        OnPlayerTurnStartSubscriber,
        PostDungeonInitializeSubscriber,
        PostUpdateSubscriber,
        AddAudioSubscriber,
        PostInitializeSubscriber {

    public static final String modID = "customlowhealthmusic";
    private static final String PREFS_NAME = "customlowhealthmusicPrefs";
    private static final String LOW_HEALTH_MUSIC_ENABLED_KEY = "lowHealthMusicEnabled";
    private static final String SELECTED_FILE_KEY = "selectedWarningIntroFile";
    public static boolean isPlaying = false;
    public static boolean isTesting = false;
    public static boolean isDead = false;
    public static boolean isBossStingerPlaying = false;  // Flag for boss stinger
    public static boolean bossBattleEnded = false;       // Prevent health checks after boss defeat
    public static String currentTempMusicKey = null;
    public static String currentRoomType = null;
    public static ModPanel settingsPanel;
    private static int currentFileIndex = 0;
    private static List<String> availableWarningIntroFiles = new ArrayList<>();
    private static String currentWarningIntroFilePath = null;
    private static Music currentlyPlayingMusic;
    private static boolean lowHealthMusicEnabled = true;
    private static float volumeMultiplier = 1.0f;
    public static ModLabel playingLabel;
    private static float lowHealthThreshold = 0.2f;
    private static final String[] SPECIAL_TEMP_TRACKS = {
            "CREDITS"
    };

    public ModFile() {
        BaseMod.subscribe(this);
        loadAvailableFiles(); // Load the available audio files during initialization

    }

    public static String getCustomMusicFolderPath() {
            return ConfigUtils.CONFIG_DIR + File.separator + "CustomLowHealthMusic";
        }

    public static void initialize() {
        new ModFile();
    }

    private void loadPreferences() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        lowHealthMusicEnabled = prefs.getBoolean(LOW_HEALTH_MUSIC_ENABLED_KEY, true);
        currentWarningIntroFilePath = prefs.getString(SELECTED_FILE_KEY, "Pokemon Low HP.ogg"); // Default to Pokemon if nothing is saved
        volumeMultiplier = prefs.getFloat("volumeMultiplier", 1.0f);
        // If the path is not null, set the currentFileIndex to the correct file
        if (currentWarningIntroFilePath != null && availableWarningIntroFiles.contains(currentWarningIntroFilePath)) {
            currentFileIndex = availableWarningIntroFiles.indexOf(currentWarningIntroFilePath);
            System.out.println("Loaded selected file from preferences: " + currentWarningIntroFilePath);
        } else {
            System.out.println("Saved file not found or no preference, defaulting to Pokemon Low HP.ogg");
            currentWarningIntroFilePath = "Pokemon Low HP.ogg";
            currentFileIndex = availableWarningIntroFiles.indexOf(currentWarningIntroFilePath);
            savePreferences(); // Save the default selection
        }
    }


    private void savePreferences() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putBoolean(LOW_HEALTH_MUSIC_ENABLED_KEY, lowHealthMusicEnabled);
        prefs.putFloat("volumeMultiplier", volumeMultiplier);

        if (currentWarningIntroFilePath != null) {
            System.out.println("Saving preferences with selected file: " + currentWarningIntroFilePath);
            prefs.putString(SELECTED_FILE_KEY, currentWarningIntroFilePath);
        } else {
            System.out.println("No file selected, saving default.");
        }

        prefs.flush(); // Ensure changes are saved immediately
    }

    public static float getVolumeMultiplier() {
        return volumeMultiplier;
    }

    public void setVolumeMultiplier(float newMultiplier) {
        volumeMultiplier = newMultiplier;
        savePreferences(); // Save the new multiplier to preferences
    }
    @Override
    public void receiveEditStrings() {
        // Localization loading...
    }

    @Override
    public void receiveAddAudio() {
        // Adding audio files through BaseMod
        for (String fileName : availableWarningIntroFiles) {
            File file = new File(getCustomMusicFolderPath() + File.separator + fileName);
            if (file.exists()) {  // Check if the file exists before adding
                String fileKey = fileName.substring(0, fileName.length() - 4); // Remove ".ogg" from the name
                BaseMod.addAudio(makeID(fileKey), file.getAbsolutePath());
            }
        }
    }

    @Override
    public void receiveEditCards() {
        // Implement card edits here if needed
    }

    @Override
    public void receiveEditKeywords() {
        // Implement keyword edits here if needed
    }

    @Override
    public void receiveEditRelics() {
        // Implement relic edits here if needed
    }

    public static String makeID(String idText) {
        return modID + ":" + idText;
    }

    public static void playTempBgm(String fullPath) {
        if (!lowHealthMusicEnabled) return;
        try {
            if (!isTesting) {
                stopCurrentMusic();
            }
            System.out.println("Attempting to play file: " + fullPath);
            FileHandle fileHandle = Gdx.files.absolute(fullPath); // Use the full path here
            currentlyPlayingMusic = Gdx.audio.newMusic(fileHandle);
            float adjustedVolume = Settings.MUSIC_VOLUME * volumeMultiplier;
            currentlyPlayingMusic.setVolume(adjustedVolume);
            currentlyPlayingMusic.setLooping(true);  // Set the music to loop
            currentlyPlayingMusic.play();
            isPlaying = true;
        } catch (Exception e) {
            System.err.println("Music file not found or failed to load: " + fullPath);
            e.printStackTrace();
        }
    }
    public static void stopCurrentMusic() {
        if (currentlyPlayingMusic != null) {
            // Stop music only when leaving the settings panel or when not in testing mode
                currentlyPlayingMusic.stop();
                currentlyPlayingMusic.dispose();
                currentlyPlayingMusic = null;
                isPlaying = false;
                System.out.println("Music stopped.");
            }
        }


    public static void stopTempBgm() {
        CardCrawlGame.music.silenceTempBgmInstantly();
        currentTempMusicKey = null;
    }

    private void loadAvailableFiles() {
        availableWarningIntroFiles.clear(); // Clear the list before adding new files

        File dir = new File(getCustomMusicFolderPath());
        if (!dir.exists()) {
            dir.mkdirs(); // Create the directory if it doesn't exist
        }

        // Copy default files from resources if they are missing
        copyDefaultFiles(dir);

        // Now, load the available .ogg files from the directory
        for (File file : dir.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".ogg")) {
                availableWarningIntroFiles.add(file.getName());
            }
        }

        if (availableWarningIntroFiles.isEmpty()) {
            System.out.println("No .ogg files found in: " + dir.getAbsolutePath());
        }
    }

    private void copyDefaultFiles(File targetDir) {
        String[] defaultFiles = {
                "Pokemon Low HP.ogg",
                "Sonic Drowning Remix.ogg",
                "Mario Hurry Up.ogg",
                "Zelda Low Heart.ogg",
                "Kingdom Hearts Low Health.ogg",
                "Super Metroid Low Energy.ogg",
                "Pokemon Alternate Low HP.ogg",
                "Heartbeat Sounds.ogg"
        };

        for (String fileName : defaultFiles) {
            File targetFile = new File(targetDir, fileName);
            if (!targetFile.exists()) { // Only copy if the file doesn't already exist
                try (InputStream in = getClass().getResourceAsStream("/customlowhealthmusicResources/audio/music/" + fileName)) {
                    if (in != null) {
                        try (OutputStream out = Files.newOutputStream(targetFile.toPath())) {
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = in.read(buffer)) > 0) {
                                out.write(buffer, 0, length);
                            }
                        }
                        System.out.println("Copied default file: " + fileName);
                    } else {
                        System.err.println("Default file not found in resources: " + fileName);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to copy default file: " + fileName);
                    e.printStackTrace();
                }
            } else {
                System.out.println("File already exists, skipping copy: " + fileName);
            }
        }
    }

    private static String getCurrentWarningIntroFileName() {
        if (availableWarningIntroFiles.isEmpty()) {
            return "No files available";
        }
        return currentWarningIntroFilePath;
    }
    public void receivePostInitialize() {
        settingsPanel = new ModPanel();
        if (playingLabel != null) {
            playingLabel.text = ""; // Ensure the label is cleared on panel open
        }
        // Load available warning intro files (clear list first)
        loadAvailableFiles();
        loadPreferences();
        // Convert the list of files to an array for the dropdown
        String[] fileOptions = availableWarningIntroFiles.toArray(new String[0]);
        int savedIndex = Math.max(availableWarningIntroFiles.indexOf(new File(currentWarningIntroFilePath).getName()), 0);
        // Toggle for enabling/disabling low health music
        ModLabeledToggleButton lowHealthMusicToggle = new ModLabeledToggleButton(
                "Enable Low Health Music",
                386.686f, 720f, Settings.CREAM_COLOR, FontHelper.charDescFont,
                lowHealthMusicEnabled,
                settingsPanel,
                (label) -> {
                },
                (button) -> {
                    lowHealthMusicEnabled = button.enabled;
                    savePreferences();
                });
        settingsPanel.addUIElement(lowHealthMusicToggle);

        // Dropdown menu for file selection
        DropdownMenu fileDropdownMenu = new DropdownMenu(
                (dropdownMenu, index, s) -> {
                    if (isPlaying) {
                        stopCurrentMusic(); // Stop the preview music if it's playing
                        isTesting = false; // Reset the testing flag
                    }
                    setSelectedWarningIntroFile(index); // Set the selected file when an option is selected
                },
                fileOptions, // Array of file options
                FontHelper.tipBodyFont, // Font for the dropdown
                Settings.CREAM_COLOR // Text color
        );
        fileDropdownMenu.setSelectedIndex(savedIndex);
        // Create an IUIElement wrapper for the dropdown
        IUIElement dropdownWrapper = new IUIElement() {
            @Override
            public void render(SpriteBatch sb) {
                // Render the dropdown at specific coordinates
                fileDropdownMenu.render(sb, 386.686f * Settings.xScale, 515f * Settings.yScale);
            }

            @Override
            public void update() {
                // Update the dropdown
                fileDropdownMenu.update();
            }

            @Override
            public int renderLayer() {
                return 3;
            }

            @Override
            public int updateOrder() {
                return 0;
            }
        };

        // Add the dropdown wrapper to the settings panel
        settingsPanel.addUIElement(dropdownWrapper);

        // Label to show the current selected file
        ModLabel currentFileLabel = new ModLabel(
                "Current File:",
                500f,
                550f,
                Settings.CREAM_COLOR,
                FontHelper.charDescFont,
                settingsPanel,
                (label) -> {
                }
        );
        settingsPanel.addUIElement(currentFileLabel);

        String explanationText = "To add new files, they must be .ogg files and they need to be added to the following location:\n" + getCustomMusicFolderPath();

        // Label to display the explanation
        ModLabel explanationLabel = new ModLabel(
                explanationText,
                400f,
                380f,
                Settings.CREAM_COLOR,
                FontHelper.charDescFont,
                settingsPanel,
                (label) -> {
                }
        );
        settingsPanel.addUIElement(explanationLabel);

        ModSliderBetter volumeSlider = new ModSliderBetter(
                "Volume Multiplier (x0.0 - x2.0)", // Slider label
                690.0F, // x position on the screen
                685.0F, // y position on the screen
                0.0F, // Min value
                2.0F, // Max value
                getVolumeMultiplier(), // Default value
                "%.2f", // Format string for the displayed value
                settingsPanel,
                (slider) -> {
                    float sliderValue = slider.getValue();
                    setVolumeMultiplier(sliderValue);
                    savePreferences();
                    try {
                        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
                        prefs.putFloat("volumeMultiplier", sliderValue);
                        prefs.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        );

// Set the slider's value to the current volume multiplier
        volumeSlider.setValue(getVolumeMultiplier());
        settingsPanel.addUIElement(volumeSlider);

        ModButton openFolderButton = new ModButton(
                386.686f, // X position
                220.0f, // Y position
                settingsPanel,
                (button) -> {
                    openFileExplorer(getCustomMusicFolderPath());
                }
        );
        settingsPanel.addUIElement(openFolderButton);

// Create the label next to the button
        ModLabel folderButtonLabel = new ModLabel(
                "Open Custom Music Folder", // Text
                511.0f, // X position, adjust as needed to be right of the button
                275.0f, // Y position, slightly higher to align with button
                Settings.CREAM_COLOR, // Text color
                FontHelper.charDescFont, // Font
                settingsPanel,
                (label) -> {
                }
        );
        settingsPanel.addUIElement(folderButtonLabel);

// Button for previewing the selected music file
        ModButton previewButton = new ModButton(
                760.0f, // X position, adjust as needed
                440f, // Y position, align with dropdown
                settingsPanel,
                (button) -> {
                    if (isTesting || isPlaying) {
                        // If it's testing and music is playing, stop it
                        isTesting = false;
                        stopCurrentMusic(); // Stop music if already playing
                        System.out.println("Music stopped.");
                        if (playingLabel != null) {
                            playingLabel.text = ""; // Clear the label when the music stops
                        }
                        CardCrawlGame.music.unsilenceBGM(); // Resume the normal background music
                    } else {
                        // If not testing, start playing the preview music
                        String selectedWarningIntro = getCurrentWarningIntroFileName();
                        String fullPath = getCustomMusicFolderPath() + File.separator + selectedWarningIntro;
                        System.out.println("Attempting to play: " + fullPath);
                        isTesting = true;  // Set testing mode to true
                        CardCrawlGame.music.silenceBGMInstantly(); // Silence the normal background music
                        playTempBgm(fullPath); // Play selected music file
                        isPlaying = true;
                        System.out.println("Music playing: " + selectedWarningIntro);
                        if (playingLabel != null) {
                            playingLabel.text = "Playing..."; // Show the label when the music is playing
                        }
                    }
                }
        );

        settingsPanel.addUIElement(previewButton);


        playingLabel = new ModLabel(
                "", // Initially empty
                890.0f, // X position, adjust as needed to be right of the button
                490.0f, // Y position, slightly higher to align with button
                Settings.CREAM_COLOR, // Text color
                FontHelper.charDescFont, // Font
                settingsPanel,
                (label) -> {
                }
        );
        settingsPanel.addUIElement(playingLabel);
        if (isTesting) {
            playingLabel.text = "Playing...";
        } else {
            playingLabel.text = "";
        }

// Label to show "Preview" next to the button
        ModLabel previewButtonLabel = new ModLabel(
                "Preview", // Text
                777.0f, // X position, adjust as needed to be right of the button
                550.0f, // Y position, slightly higher to align with button
                Settings.CREAM_COLOR, // Text color
                FontHelper.charDescFont, // Font
                settingsPanel,
                (label) -> {
                }
        );
        settingsPanel.addUIElement(previewButtonLabel);

        ModSliderBetter lowHealthSlider = new ModSliderBetter(
                "Health % of Max HP Trigger", // Slider label
                690.0F, // X position
                630.0F, // Y position
                0.0F, // Min value (0%)
                100.0F, // Max value (100%)
                getLowHealthThreshold() * 100.0F, // Default value (current threshold percentage)
                "%.0f%%", // Format string for displayed value
                settingsPanel,
                (slider) -> {
                    float sliderValue = slider.getValue();
                    setLowHealthThreshold(sliderValue / 100.0F);  // Convert percentage to decimal
                    savePreferences();  // Save the threshold to preferences
                }
        );

        lowHealthSlider.setValue(getLowHealthThreshold() * 100.0F);  // Set slider value to current threshold
        settingsPanel.addUIElement(lowHealthSlider);

        Texture badgeTexture = new Texture(Gdx.files.internal("customlowhealthmusicResources/images/ui/badge.png"));
        BaseMod.registerModBadge(badgeTexture, "Custom Low Health Music", "Ninja Puppy", "Custom music for low health situations.", settingsPanel);
    }
    private void openFileExplorer(String folderPath) {
        if (Desktop.isDesktopSupported()) {
            try {
                File file = new File(folderPath);
                if (file.exists()) {
                    Desktop.getDesktop().open(file);
                } else {
                    System.err.println("Folder does not exist: " + folderPath);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Failed to open folder: " + folderPath);
            }
        } else {
            System.err.println("Desktop is not supported on this platform.");
        }
    }
    // Method to update the selected warning intro file
    private void setSelectedWarningIntroFile(int selectedIndex) {
        if (selectedIndex >= 0 && selectedIndex < availableWarningIntroFiles.size()) {
            currentWarningIntroFilePath = availableWarningIntroFiles.get(selectedIndex);

            // Log the file path being saved
            System.out.println("Saving selected file: " + currentWarningIntroFilePath);

            // Save the selected file to preferences
            Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
            prefs.putString(SELECTED_FILE_KEY, currentWarningIntroFilePath);

            savePreferences();

            // Stop the current music and reset the label when a new file is selected
            if (isPlaying) {
                stopCurrentMusic(); // Stop the preview music if it's playing
                isTesting = false; // Reset the testing flag
            }
            if (playingLabel != null) {
                playingLabel.text = ""; // Clear the "Playing..." label
            }
        }
    }

    @Override
    public void receiveOnBattleStart(AbstractRoom abstractRoom) {
        if (lowHealthMusicEnabled) {
            resetMusicStates();
            updateRoomType(abstractRoom);
            checkPlayerHealth();
        }
    }
    @Override
    public void receiveOnPlayerTurnStart() {
        // Additional logic for player turn start if needed
    }

    @Override
    public void receivePostBattle(AbstractRoom abstractRoom) {
        if (lowHealthMusicEnabled && AbstractDungeon.screen != AbstractDungeon.CurrentScreen.DEATH) {
            stopHealthWarningMusic();
            resetMusicStates();
        }
    }

    @Override
    public void receivePostDungeonInitialize() {
        if (CardCrawlGame.isInARun() && lowHealthMusicEnabled && AbstractDungeon.currMapNode != null && AbstractDungeon.getCurrRoom() != null && AbstractDungeon.actionManager != null) {
            resetMusicStates();
            checkPlayerHealth();
        }
    }

    @Override
    public void receivePostUpdate() {
        // Check if the player is on the Death Screen to avoid stopping music prematurely
        if (!CardCrawlGame.isInARun() && AbstractDungeon.screen != AbstractDungeon.CurrentScreen.DEATH) {
            if (!isTesting && !BaseMod.modSettingsUp){
                stopHealthWarningMusic();  // Stop health music only if not on the death screen
                CardCrawlGame.music.silenceTempBgmInstantly();  // Silence background music, but not on death screen
            }
        }else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.DEATH) {
        isDead = true;
            stopHealthWarningMusic();
        }
        else if (AbstractDungeon.currMapNode != null && AbstractDungeon.actionManager != null && isPlaying) {
            // Ensure that music only plays in combat rooms and not during transitions
            if (AbstractDungeon.getCurrRoom() == null || !AbstractDungeon.getCurrRoom().phase.equals(AbstractRoom.RoomPhase.COMBAT)) {
                stopHealthWarningMusic();  // Stop music if not in combat
            }
            if (!BaseMod.modSettingsUp && isTesting) {
                stopCurrentMusic();  // Stop preview music when settings panel is closed
                isTesting = false;  // Reset testing flag
                if (playingLabel != null) {
                    playingLabel.text = "";  // Clear the "Playing..." label
                }
                System.out.println("Mod config closed, preview music stopped.");
            }
        }

        // Adjust volume based on game settings
        if (currentlyPlayingMusic != null) {
            float adjustedVolume = Settings.MUSIC_VOLUME * volumeMultiplier;
            currentlyPlayingMusic.setVolume(adjustedVolume);
        }
    }


    private void resetMusicStates() {
        isPlaying = false;
        isBossStingerPlaying = false;
        bossBattleEnded = false;
        currentRoomType = null;
        currentTempMusicKey = null;
    }

    private void updateRoomType(AbstractRoom room) {
        if (room instanceof MonsterRoomBoss) {
            currentRoomType = "BOSS";
        } else if (room instanceof MonsterRoomElite) {
            currentRoomType = "ELITE";
        } else if (room instanceof EventRoom) {
            currentRoomType = "EVENT";
        } else {
            currentRoomType = null;
        }
    }

    public static boolean isSpecialTempTrackPlaying() {
        if (currentTempMusicKey != null) {
            for (String specialTrack : SPECIAL_TEMP_TRACKS) {
                if (currentTempMusicKey.equals(specialTrack)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void playBossStinger() {
        if (isBossStingerPlaying) {
            return; // Prevent playing anything else if the boss stinger is playing.
        }

        isBossStingerPlaying = true;

        // Play the boss victory stinger sound effect
        CardCrawlGame.sound.play("BOSS_VICTORY_STINGER");

        if (AbstractDungeon.id.equals("TheEnding")) {
            CardCrawlGame.music.playTempBgmInstantly("STS_EndingStinger_v1.ogg", false);
        } else {
            switch (MathUtils.random(0, 3)) {
                case 0:
                    CardCrawlGame.music.playTempBgmInstantly("STS_BossVictoryStinger_1_v3_MUSIC.ogg", false);
                    break;
                case 1:
                    CardCrawlGame.music.playTempBgmInstantly("STS_BossVictoryStinger_2_v3_MUSIC.ogg", false);
                    break;
                case 2:
                    CardCrawlGame.music.playTempBgmInstantly("STS_BossVictoryStinger_3_v3_MUSIC.ogg", false);
                    break;
                case 3:
                    CardCrawlGame.music.playTempBgmInstantly("STS_BossVictoryStinger_4_v3_MUSIC.ogg", false);
                    break;
            }
        }
    }
    public static float getLowHealthThreshold() {
        return lowHealthThreshold;
    }

    public static void setLowHealthThreshold(float threshold) {
        lowHealthThreshold = threshold;
    }
    public static AbstractGameAction checkPlayerHealth() {
        if (!lowHealthMusicEnabled) {
            return null;  // Skip health check if the option is disabled
        }

        AbstractPlayer player = AbstractDungeon.player;
        float healthThreshold = player.maxHealth * getLowHealthThreshold();  // Use user-defined threshold

        if (player.currentHealth <= healthThreshold && !ModFile.isPlaying) {
            playHealthWarningMusic();
            ModFile.isPlaying = true;
        } else if (player.currentHealth > healthThreshold && ModFile.isPlaying) {
            stopHealthWarningMusic();
            ModFile.isPlaying = false;
        }
        return null;
    }

    private static String getBossMusicKey() {
        switch (AbstractDungeon.actNum) {
            case 1:
                return "STS_Boss1_NewMix_v1.ogg";
            case 2:
                return "STS_Boss2_NewMix_v1.ogg";
            case 3:
                return "STS_Boss3_NewMix_v1.ogg";
            case 4:
                return "STS_Boss4_v6.ogg";
            default:
                return "STS_Boss1_NewMix_v1.ogg";  // Default boss music
        }
    }

    public static void playHealthWarningMusic() {
        if (!lowHealthMusicEnabled || isSpecialTempTrackPlaying() || isBossStingerPlaying || bossBattleEnded) {
            return; // Exit if low health music is disabled or any special conditions are met
        }
            System.out.println("PlayWarningMusicMain");
            CardCrawlGame.music.silenceTempBgmInstantly();
            CardCrawlGame.music.silenceBGM();
            String selectedWarningIntro = getCurrentWarningIntroFileName(); // Fetch the current file name
            String fullPath = getCustomMusicFolderPath() + File.separator + selectedWarningIntro; // Get the full path
            System.out.println("Playing selected file: " + fullPath);
            ModFile.playTempBgm(fullPath); // Play the music using the full path
            ModFile.isPlaying = true;
        }


    public static void stopHealthWarningMusic() {
        if (isBossStingerPlaying) {
            return; // Prevent any music from interrupting the boss stinger.
        } else if (isDead){
            stopCurrentMusic();
            }
        if (isPlaying) {
            if (AbstractDungeon.currMapNode == null ||AbstractDungeon.getCurrRoom() == null || AbstractDungeon.getCurrRoom().monsters == null) {
                stopCurrentMusic();
                return;  // Early exit if not in a combat room or no monsters are present
            }

            boolean isFightingLagavulin = false;
            boolean isLagavulinAsleep = false;
            boolean isFightingHexaghost = false;
            boolean isHexaghostActivated = false;
            boolean isFightingHeart = false;
            boolean isFightingSpireSpearOrShield = false;
            boolean isEventRoomBoss = false;
            boolean isEventMindBloom = false;
            boolean allMonstersDead = true;

            // Check the status of all monsters
            for (AbstractMonster mo : AbstractDungeon.getCurrRoom().monsters.monsters) {
                if (!mo.isDeadOrEscaped() && !mo.isDying) {
                    allMonstersDead = false;  // At least one monster is still alive
                }

                // Check for specific bosses and elites
                if (mo.id.equals("Lagavulin")) {
                    isFightingLagavulin = true;
                    isLagavulinAsleep = LagavulinSleepPatch.isLagavulinAsleep((Lagavulin) mo); // Check if Lagavulin is asleep
                } else if (mo.id.equals("Hexaghost")) {
                    isFightingHexaghost = true;
                    isHexaghostActivated = HexaghostActivationFieldPatch.isActivated.get(mo);
                } else if (mo.id.equals("CorruptHeart")) {
                    isFightingHeart = true;
                } else if (mo.id.equals("SpireShield") || mo.id.equals("SpireSpear")) {
                    isFightingSpireSpearOrShield = true;
                } else if (AbstractDungeon.getCurrRoom().event instanceof MindBloom) {
                    isEventMindBloom = true;
                }
            }

            // Handle transitions if all monsters are dead
            if (allMonstersDead) {
                stopCurrentMusic(); // Stop the current health warning music

                if (currentRoomType != null && currentRoomType.equals("ELITE")) {
                    if (isFightingLagavulin) {
                        // Switch back to Exordium music after Lagavulin dies
                        CardCrawlGame.music.silenceTempBgmInstantly();
                        CardCrawlGame.music.unsilenceBGM();
                    } else {
                        // Switch back to area music after other elites die
                        CardCrawlGame.music.silenceTempBgmInstantly();
                        CardCrawlGame.music.unsilenceBGM();
                    }
                } else {
                    // Non-boss or non-elite room: handle music transitions
                    CardCrawlGame.music.silenceTempBgmInstantly();
                    CardCrawlGame.music.unsilenceBGM();
                }
            } else {
                // If monsters are still alive, play the appropriate music for elites or bosses
                stopCurrentMusic();
                {
                    if (isFightingLagavulin) {
                        stopCurrentMusic(); // Stop the current health warning music
                        if (!isLagavulinAsleep) {
                            CardCrawlGame.music.silenceTempBgmInstantly();
                            CardCrawlGame.music.playTempBgmInstantly("STS_EliteBoss_NewMix_v1.ogg");
                        } else {
                            CardCrawlGame.music.silenceTempBgmInstantly();
                            CardCrawlGame.music.silenceBGM();
                        }
                    } else if (isFightingHexaghost && !isEventMindBloom) {
                        stopCurrentMusic(); // Stop the current health warning music
                        if (isHexaghostActivated) {
                            CardCrawlGame.music.silenceTempBgmInstantly();
                            CardCrawlGame.music.playTempBgmInstantly(getBossMusicKey());
                        } else {
                            CardCrawlGame.music.silenceTempBgmInstantly();
                            CardCrawlGame.music.silenceBGM();
                        }
                    } else if (isFightingHeart) {
                        stopCurrentMusic(); // Stop the current health warning music
                        CardCrawlGame.music.silenceTempBgmInstantly();
                        CardCrawlGame.music.playTempBgmInstantly("STS_Boss4_v6.ogg");
                    } else if (isFightingSpireSpearOrShield) {
                        stopCurrentMusic(); // Stop the current health warning music
                        CardCrawlGame.music.silenceTempBgmInstantly();
                        CardCrawlGame.music.playTempBgmInstantly("STS_Act4_BGM_v2.ogg");
                    } else if (isEventMindBloom) {
                        stopCurrentMusic(); // Stop the current health warning music
                        CardCrawlGame.music.silenceTempBgmInstantly();
                        CardCrawlGame.music.playTempBgmInstantly("MINDBLOOM");
                    } else if (currentRoomType != null && currentRoomType.equals("BOSS")) {
                        // Handle normal boss music based on the act number
                        String bossMusicKey = getBossMusicKey();
                        stopCurrentMusic(); // Stop the current health warning music
                        CardCrawlGame.music.silenceTempBgmInstantly();
                        CardCrawlGame.music.playTempBgmInstantly(bossMusicKey);
                    } else if (currentRoomType != null && currentRoomType.equals("ELITE")) {
                        // Keep playing elite music until elites are dead
                        stopCurrentMusic(); // Stop the current health warning music
                        CardCrawlGame.music.silenceTempBgmInstantly();
                        CardCrawlGame.music.playTempBgmInstantly("STS_EliteBoss_NewMix_v1.ogg");
                    } else {
                        // Handle non-boss, non-elite rooms
                        stopCurrentMusic(); // Stop the current health warning music
                        CardCrawlGame.music.silenceTempBgmInstantly();
                        CardCrawlGame.music.unsilenceBGM();
                    }
                }
            }
            // Reset the health warning music state
            isPlaying = false;
        }
    }
}
