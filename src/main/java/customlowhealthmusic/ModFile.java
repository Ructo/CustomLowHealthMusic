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
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
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
    private static final String LOOPING_PREF_KEY = "isMusicLooping";
    public static boolean isPlaying = false;
    public static boolean isTesting = false;
    public static boolean isDead = false;
    public static boolean isLoaded = false;
    private static boolean isMusicLooping = true;
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
    private static String lastPlayedTrack = null;
    private DropdownMenu fileDropdownMenu;
    private IUIElement dropdownWrapper;
    private static UIStrings uiStrings;
    private static String[] TEXT;

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

        // Load the looping preference
        isMusicLooping = prefs.getBoolean(LOOPING_PREF_KEY, true); // Default to true
        System.out.println("Preferences loaded with looping: " + isMusicLooping);

        // Determine the current file index for the dropdown menu
        if ("RANDOM_SELECTION".equals(currentWarningIntroFilePath)) {
            currentFileIndex = 0; // Index for "Random Selection"
            System.out.println("Loaded selected file from preferences: Random Selection");
        } else if (currentWarningIntroFilePath != null && availableWarningIntroFiles.contains(currentWarningIntroFilePath)) {
            currentFileIndex = availableWarningIntroFiles.indexOf(currentWarningIntroFilePath) + 1; // Adjust index
            System.out.println("Loaded selected file from preferences: " + currentWarningIntroFilePath);
        } else {
            System.out.println("Saved file not found or no preference, defaulting to Pokemon Low HP.ogg");
            currentWarningIntroFilePath = "Pokemon Low HP.ogg";
            currentFileIndex = availableWarningIntroFiles.indexOf(currentWarningIntroFilePath) + 1;
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
            prefs.putString(SELECTED_FILE_KEY, "Pokemon Low HP.ogg"); // Default value
        }

        // Save the looping preference
        prefs.putBoolean(LOOPING_PREF_KEY, isMusicLooping);

        prefs.flush(); // Ensure changes are saved immediately
        System.out.println("Preferences saved with looping: " + isMusicLooping);
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
        // Always checking for "UIStrings.json"
        String locPath = makeLocPath("UIStrings");
        System.out.println("Loading localization file: " + locPath); // Debugging statement
        BaseMod.loadCustomStringsFile(UIStrings.class, locPath);
    }
    private static String makeLocPath(String filename) {
        String basePath = "customlowhealthmusicResources/localization/";
        String langPath;

        switch (Settings.language) {
            case RUS:
                langPath = basePath + "rus/" + filename + ".json";
                break;
            case ZHS:
                langPath = basePath + "zhs/" + filename + ".json";
                break;
            case ZHT:
                langPath = basePath + "zht/" + filename + ".json";
                break;
            case SPA:
                langPath = basePath + "spa/" + filename + ".json";
                break;
            case SRB:
                langPath = basePath + "srb/" + filename + ".json";
                break;
            case SRP:
                langPath = basePath + "srp/" + filename + ".json";
                break;
            case DEU:
                langPath = basePath + "deu/" + filename + ".json";
                break;
            case DUT:
                langPath = basePath + "dut/" + filename + ".json";
                break;
            case EPO:
                langPath = basePath + "epo/" + filename + ".json";
                break;
            case FIN:
                langPath = basePath + "fin/" + filename + ".json";
                break;
            case FRA:
                langPath = basePath + "fra/" + filename + ".json";
                break;
            case GRE:
                langPath = basePath + "gre/" + filename + ".json";
                break;
            case IND:
                langPath = basePath + "ind/" + filename + ".json";
                break;
            case ITA:
                langPath = basePath + "ita/" + filename + ".json";
                break;
            case JPN:
                langPath = basePath + "jpn/" + filename + ".json";
                break;
            case KOR:
                langPath = basePath + "kor/" + filename + ".json";
                break;
            case NOR:
                langPath = basePath + "nor/" + filename + ".json";
                break;
            case POL:
                langPath = basePath + "pol/" + filename + ".json";
                break;
            case PTB:
                langPath = basePath + "ptb/" + filename + ".json";
                break;
            case THA:
                langPath = basePath + "tha/" + filename + ".json";
                break;
            case TUR:
                langPath = basePath + "tur/" + filename + ".json";
                break;
            case UKR:
                langPath = basePath + "ukr/" + filename + ".json";
                break;
            case VIE:
                langPath = basePath + "vie/" + filename + ".json";
                break;
            default:
                // Default to English if no matching language is found
                langPath = basePath + "eng/" + filename + ".json";
                break;
        }
        return langPath;
    }
    @Override
    public void receiveAddAudio() {
        registerCustomAudio();
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

    public static void playTempBgm(String path) {
        try {
            // Stop any currently playing music
            stopCurrentMusic();

            System.out.println("Attempting to play file: " + path);
            FileHandle fileHandle = Gdx.files.absolute(path);

            if (!fileHandle.exists()) {
                System.out.println("Music file not found: " + path);
                return;
            }

            currentlyPlayingMusic = Gdx.audio.newMusic(fileHandle);
            currentlyPlayingMusic.setLooping(isMusicLooping); // Use the user's preference
            currentlyPlayingMusic.setVolume(Settings.MUSIC_VOLUME * volumeMultiplier);

            if (!isMusicLooping) {
                // Set an OnCompletionListener to handle when the music finishes playing
                currentlyPlayingMusic.setOnCompletionListener(new Music.OnCompletionListener() {
                    @Override
                    public void onCompletion(Music music) {
                        System.out.println("Music playback completed.");
                        // Set flags to indicate music has stopped
                        currentlyPlayingMusic = null;
                        stopHealthWarningMusic();
                    }
                });
            }

            currentlyPlayingMusic.play();
            isPlaying = true;
        } catch (Exception e) {
            System.err.println("Failed to play music: " + path);
            e.printStackTrace();
            // Ensure the music system remains functional
            stopCurrentMusic();
            isPlaying = false;
            currentlyPlayingMusic = null;
        }
    }

    public static void stopCurrentMusic() {
        if (currentlyPlayingMusic != null) {
            currentlyPlayingMusic.stop();
            // Remove the completion listener to prevent potential memory leaks
            currentlyPlayingMusic.setOnCompletionListener(null);
            currentlyPlayingMusic.dispose();
            currentlyPlayingMusic = null;
            isPlaying = false;
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

        // Now, load the available .ogg files from the directory
        if (dir.listFiles() != null) {
            for (File file : dir.listFiles()) {
                if (file.isFile() && file.getName().endsWith(".ogg")) {
                    availableWarningIntroFiles.add(file.getName());
                }
            }
        }

        if (availableWarningIntroFiles.isEmpty()) {
            System.out.println("No .ogg files found in: " + dir.getAbsolutePath());
        }
    }


    private void restoreDefaultTracks() {
        File targetDir = new File(getCustomMusicFolderPath());
        if (!targetDir.exists()) {
            targetDir.mkdirs(); // Create the directory if it doesn't exist
        }

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
            try (InputStream in = getClass().getResourceAsStream("/customlowhealthmusicResources/audio/music/" + fileName)) {
                if (in != null) {
                    try (OutputStream out = Files.newOutputStream(targetFile.toPath())) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = in.read(buffer)) > 0) {
                            out.write(buffer, 0, length);
                        }
                    }
                    System.out.println("Restored default file: " + fileName);
                } else {
                    System.err.println("Default file not found in resources: " + fileName);
                }
            } catch (Exception e) {
                System.err.println("Failed to restore default file: " + fileName);
                e.printStackTrace();
            }
        }

        // Reload the available files after restoring defaults
        loadAvailableFiles();

        // Register the new audio files with BaseMod
        registerCustomAudio();

        // Set the currentWarningIntroFilePath to a valid file
        if (currentWarningIntroFilePath == null || currentWarningIntroFilePath.isEmpty()) {
            if (!availableWarningIntroFiles.isEmpty()) {
                currentWarningIntroFilePath = availableWarningIntroFiles.get(0);
                savePreferences();
            }
        }

        // Refresh the settings panel UI elements
        updateDropdownMenu();
    }
    private void registerCustomAudio() {
        for (String fileName : availableWarningIntroFiles) {
            File file = new File(getCustomMusicFolderPath() + File.separator + fileName);
            if (file.exists()) {
                String fileKey = fileName.substring(0, fileName.length() - 4); // Remove ".ogg" from the name
                BaseMod.addAudio(makeID(fileKey), file.getAbsolutePath());
                System.out.println("Registered audio file: " + fileName);
            }
        }
    }
    private void updateDropdownMenu() {
        // Recreate the file options list
        List<String> fileOptionsList = new ArrayList<>();

        if (availableWarningIntroFiles.isEmpty()) {
            fileOptionsList.add("No Valid Files Found");
        } else {
            fileOptionsList.add(TEXT[5]); // Add the new option at the top

            // Add truncated file names for the dropdown display
            for (String fileName : availableWarningIntroFiles) {
                fileOptionsList.add(truncateFileName(fileName)); // Use the truncated names
            }
        }

        String[] fileOptions = fileOptionsList.toArray(new String[0]);

        // Recreate the dropdown menu
        fileDropdownMenu = new DropdownMenu(
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

        // Set the selected index based on currentWarningIntroFilePath
        int savedIndex = 0; // Default to first option
        if (currentWarningIntroFilePath != null && !currentWarningIntroFilePath.isEmpty()) {
            if ("RANDOM_SELECTION".equals(currentWarningIntroFilePath)) {
                savedIndex = 0; // Index for "Random Selection"
            } else {
                int index = availableWarningIntroFiles.indexOf(currentWarningIntroFilePath);
                if (index != -1) {
                    savedIndex = index + 1; // Adjust index since "Random Selection" is at index 0
                }
            }
        }
        fileDropdownMenu.setSelectedIndex(savedIndex);
    }


    private static String getCurrentWarningIntroFileName() {
        if (availableWarningIntroFiles.isEmpty()) {
            return "No files available";
        }
        if ("RANDOM_SELECTION".equals(currentWarningIntroFilePath)) {
            return TEXT[5];
        }
        return currentWarningIntroFilePath;
    }
    public void receivePostInitialize() {

            uiStrings = CardCrawlGame.languagePack.getUIString("customlowhealthmusic:EnableLowHealthMusic");
            if (uiStrings != null && uiStrings.TEXT != null) {
                TEXT = uiStrings.TEXT;
            }
            settingsPanel = new ModPanel();
            if (playingLabel != null) {
                playingLabel.text = ""; // Ensure the label is cleared on panel open
            }
            // Load available warning intro files (clear list first)
            loadAvailableFiles();
            loadPreferences();
            // Convert the list of files to an array for the dropdown
            List<String> fileOptionsList = new ArrayList<>();
            if (availableWarningIntroFiles.isEmpty()) {
                fileOptionsList.add("No Valid Files Found");
                currentWarningIntroFilePath = null;
            } else {
                fileOptionsList.add(TEXT[5]); // Add the new option at the top

                // Add truncated file names for the dropdown display
                for (String fileName : availableWarningIntroFiles) {
                    fileOptionsList.add(truncateFileName(fileName)); // Use the truncated names
                }
            }
            String[] fileOptions = fileOptionsList.toArray(new String[0]);

            int savedIndex = 0;

            if (currentWarningIntroFilePath != null) {
                if ("RANDOM_SELECTION".equals(currentWarningIntroFilePath)) {
                    savedIndex = 0; // Index for "Random Selection"
                } else {
                    int index = availableWarningIntroFiles.indexOf(currentWarningIntroFilePath);
                    if (index != -1) {
                        savedIndex = index + 1; // Adjust index since "Random Selection" is at index 0
                    }
                }
            }
            // Toggle for enabling/disabling low health music
            ModLabeledToggleButton lowHealthMusicToggle = new ModLabeledToggleButton(
                    TEXT[0],
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
            fileDropdownMenu = new DropdownMenu(
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
            dropdownWrapper = new IUIElement() {
                @Override
                public void render(SpriteBatch sb) {
                    // Render the dropdown at specific coordinates
                    fileDropdownMenu.render(sb,
                            565.686f * Settings.xScale,
                            518f * Settings.yScale);
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
            settingsPanel.addUIElement(dropdownWrapper);


            // Label to show the current selected file
            ModLabel currentFileLabel = new ModLabel(
                    TEXT[3],
                    386.686f,
                    492.5f,
                    Settings.CREAM_COLOR,
                    FontHelper.charDescFont,
                    settingsPanel,
                    (label) -> {
                    }
            );
            settingsPanel.addUIElement(currentFileLabel);

            String explanationText = TEXT[4];

            // Label to display the explanation
            ModLabel explanationLabel = new ModLabel(
                    explanationText,
                    386.686f,
                    555f,
                    Settings.CREAM_COLOR,
                    FontHelper.charDescFont,
                    settingsPanel,
                    (label) -> {
                    }
            );
            settingsPanel.addUIElement(explanationLabel);

            ModSliderBetter volumeSlider = new ModSliderBetter(
                    TEXT[1], // Slider label
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
                    1100f, // X position
                    440f, // Y position
                    settingsPanel,
                    (button) -> {
                        openFileExplorer(getCustomMusicFolderPath());
                    }
            );
            settingsPanel.addUIElement(openFolderButton);

// Create the label next to the button
            ModLabel folderButtonLabel = new ModLabel(
                    TEXT[6], // Open Custom Folder
                    1224.314f, // X position, adjust as needed to be right of the button
                    495f, // Y position, slightly higher to align with button
                    Settings.CREAM_COLOR, // Text color
                    FontHelper.charDescFont, // Font
                    settingsPanel,
                    (label) -> {
                    }
            );
            settingsPanel.addUIElement(folderButtonLabel);

// Button for previewing the selected music file
            ModButton previewButton = new ModButton(
                    940.0f, // X position
                    440.0f,    // Y position
                    settingsPanel,
                    (button) -> {
                        if (availableWarningIntroFiles.isEmpty() || currentWarningIntroFilePath == null || currentWarningIntroFilePath.isEmpty()) {
                            // No valid files available
                            System.out.println("No valid files found to preview.");
                            if (playingLabel != null) {
                                playingLabel.text = TEXT[7]; // Show the message
                            }
                            return;
                        }

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
                            String selectedWarningIntro;
                            if ("RANDOM_SELECTION".equals(currentWarningIntroFilePath)) {
                                // Handle random selection
                                if (availableWarningIntroFiles.isEmpty()) {
                                    // No files available
                                    System.out.println("No valid files found to preview.");
                                    if (playingLabel != null) {
                                        playingLabel.text = TEXT[7]; // Show the message
                                    }
                                    return;
                                }

                                String randomTrack;
                                if (availableWarningIntroFiles.size() == 1) {
                                    randomTrack = availableWarningIntroFiles.get(0);
                                } else {
                                    do {
                                        int randomIndex = MathUtils.random(availableWarningIntroFiles.size() - 1);
                                        randomTrack = availableWarningIntroFiles.get(randomIndex);
                                    } while (randomTrack.equals(lastPlayedTrack));
                                }
                                selectedWarningIntro = randomTrack;
                                lastPlayedTrack = selectedWarningIntro;
                                System.out.println("Randomly selected file for preview: " + selectedWarningIntro);
                            } else {
                                selectedWarningIntro = getCurrentWarningIntroFileName();
                                if (selectedWarningIntro == null || selectedWarningIntro.isEmpty()) {
                                    System.out.println("No valid files found to preview.");
                                    if (playingLabel != null) {
                                        playingLabel.text = TEXT[7]; // Show the message
                                    }
                                    return;
                                }
                            }

                            String fullPath = getCustomMusicFolderPath() + File.separator + selectedWarningIntro;
                            File file = new File(fullPath);

                            if (!file.exists()) {
                                System.out.println("Selected file does not exist: " + fullPath);
                                if (playingLabel != null) {
                                    playingLabel.text = TEXT[8]; // Show the message
                                }
                                return;
                            }

                            System.out.println("Attempting to play: " + fullPath);
                            isTesting = true;  // Set testing mode to true
                            CardCrawlGame.music.silenceBGMInstantly(); // Silence the normal background music
                            playTempBgm(fullPath); // Play selected music file
                            isPlaying = true;
                            System.out.println("Music playing: " + selectedWarningIntro);
                            if (playingLabel != null) {
                                playingLabel.text = TEXT[9]; // Show the label when the music is playing
                            }
                        }
                    }
            );
            settingsPanel.addUIElement(previewButton);

            playingLabel = new ModLabel(
                    "", // Initially empty
                    955.0f, // X position, adjust as needed to be right of the button
                    435.0f, // Y position, slightly higher to align with button
                    Settings.CREAM_COLOR, // Text color
                    FontHelper.charDescFont, // Font
                    settingsPanel,
                    (label) -> {
                    }
            );
            settingsPanel.addUIElement(playingLabel);
            if (isTesting) {
                playingLabel.text = TEXT[10];
            } else {
                playingLabel.text = "";
            }

// Label to show "Preview" next to the button
            ModLabel previewButtonLabel = new ModLabel(
                    TEXT[2], // Text
                    955f, // X position, adjust as needed to be right of the button
                    555.0f, // Y position, slightly higher to align with button
                    Settings.CREAM_COLOR, // Text color
                    FontHelper.charDescFont, // Font
                    settingsPanel,
                    (label) -> {
                    }
            );
            settingsPanel.addUIElement(previewButtonLabel);

            ModSliderBetter lowHealthSlider = new ModSliderBetter(
                    TEXT[10], // Slider label
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
// Restore Default Tracks Button
            ModButton restoreDefaultsButton = new ModButton(
                    1100f, // X position
                    620.0f, // Y position
                    settingsPanel,
                    (button) -> {
                        restoreDefaultTracks();
                    }
            );
            settingsPanel.addUIElement(restoreDefaultsButton);

// Label for the Restore Defaults Button
            ModLabel restoreDefaultsLabel = new ModLabel(
                    TEXT[11], // Text
                    1224.314f, // X position
                    675.0f, // Y position
                    Settings.CREAM_COLOR, // Text color
                    FontHelper.charDescFont, // Font
                    settingsPanel,
                    (label) -> {
                    }
            );
            settingsPanel.addUIElement(restoreDefaultsLabel);

            // Refresh Files Button
            ModButton refreshFilesButton = new ModButton(
                    1100f, // X position
                    530.0f, // Y position
                    settingsPanel,
                    (button) -> {
                        refreshAvailableFiles();
                    }
            );
            settingsPanel.addUIElement(refreshFilesButton);

// Label for the Refresh Files Button
            ModLabel refreshFilesLabel = new ModLabel(
                    TEXT[12], // Text
                    1224.314f, // X position
                    585.0f, // Y position
                    Settings.CREAM_COLOR, // Text color
                    FontHelper.charDescFont, // Font
                    settingsPanel,
                    (label) -> {
                    }
            );
            settingsPanel.addUIElement(refreshFilesLabel);

            ModLabeledToggleButton loopingToggleButton = new ModLabeledToggleButton(
                    TEXT[13], // Text next to the checkbox
                    386.686f,    // X position
                    430.0f,    // Y position
                    Settings.CREAM_COLOR, // Text color
                    FontHelper.charDescFont, // Font
                    isMusicLooping, // Initial value from preferences
                    settingsPanel, // Parent panel
                    (label) -> {
                    }, // Label updater
                    (button) -> {
                        // Update the looping setting when the checkbox is clicked
                        isMusicLooping = button.enabled;
                        savePreferences(); // Save the new setting
                        System.out.println("Looping setting changed to: " + isMusicLooping);
                    }
            );

            settingsPanel.addUIElement(loopingToggleButton);


        Texture badgeTexture = new Texture(Gdx.files.internal("customlowhealthmusicResources/images/ui/badge.png"));
        BaseMod.registerModBadge(badgeTexture, TEXT[14], "Ninja Puppy", TEXT[15], settingsPanel);
    }
    private void refreshAvailableFiles() {
        // Reload the available files
        loadAvailableFiles();

        // Register the new audio files with BaseMod
        registerCustomAudio();

        // Refresh the dropdown menu
        updateDropdownMenu();
    }
    private String truncateFileName(String fileName) {
        if (fileName.length() > 28) {
            return fileName.substring(0, 25) + "...";
        } else {
            return fileName;
        }
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
        if (availableWarningIntroFiles.isEmpty()) {
            // Do not change currentWarningIntroFilePath if there are no files
            return;
        }

        if (selectedIndex >= 0) {
            if (selectedIndex == 0) {
                // "Random Selection" is selected
                currentWarningIntroFilePath = "RANDOM_SELECTION";
            } else if (selectedIndex - 1 < availableWarningIntroFiles.size()) {
                currentWarningIntroFilePath = availableWarningIntroFiles.get(selectedIndex - 1); // Use full file name
            }

            // Log the file path being saved
            System.out.println("Saving selected file: " + currentWarningIntroFilePath);

            // Save the selected file to preferences
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
        // If the game is backgrounded and MUTE_IF_BG is enabled, set volume to 0
        if (CardCrawlGame.MUTE_IF_BG && Settings.isBackgrounded) {
            if (currentlyPlayingMusic != null) {
                currentlyPlayingMusic.setVolume(0.0f);  // Silence music when backgrounded
            }
            return;  // Exit early, no need to do anything else
        }

        // If the game is in the foreground, apply the volume multiplier
        if (currentlyPlayingMusic != null && !Settings.isBackgrounded) {
            float adjustedVolume = getVolumeMultiplier();  // Only use the custom mod multiplier
            currentlyPlayingMusic.setVolume(adjustedVolume);  // Adjust volume based on mod settings
        }
            // Check if the player is on the Death Screen to avoid stopping music prematurely
        if (!CardCrawlGame.isInARun() && AbstractDungeon.screen != AbstractDungeon.CurrentScreen.DEATH) {
            if (!isTesting && !BaseMod.modSettingsUp){
                stopHealthWarningMusic();  // Stop health music only if not on the death screen
                CardCrawlGame.music.silenceTempBgmInstantly();  // Silence background music, but not on death screen
            }
        } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.DEATH) {
            isDead = true;
            stopHealthWarningMusic();
        } else if (AbstractDungeon.currMapNode != null && AbstractDungeon.actionManager != null && isPlaying) {
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
        if (availableWarningIntroFiles.isEmpty()) {
            System.out.println("No valid files found to play for low health warning.");
            return;
        }
        // Silence the game's background music and any temporary background music
        CardCrawlGame.music.silenceBGMInstantly(); // Silence the normal background music
        CardCrawlGame.music.silenceTempBgmInstantly(); // Silence any temporary background music
        // Proceed to play the low health music using playTempBgm()
        String selectedWarningIntro;
        if ("RANDOM_SELECTION".equals(currentWarningIntroFilePath)) {
            // Handle random selection
            String randomTrack;
            if (availableWarningIntroFiles.size() == 1) {
                randomTrack = availableWarningIntroFiles.get(0);
            } else {
                do {
                    int randomIndex = MathUtils.random(availableWarningIntroFiles.size() - 1);
                    randomTrack = availableWarningIntroFiles.get(randomIndex);
                } while (randomTrack.equals(lastPlayedTrack));
            }
            selectedWarningIntro = randomTrack;
            lastPlayedTrack = selectedWarningIntro;
            System.out.println("Randomly selected file: " + selectedWarningIntro);
        } else {
            selectedWarningIntro = getCurrentWarningIntroFileName();
            if (selectedWarningIntro == null || selectedWarningIntro.isEmpty()) {
                System.out.println("No valid files found to play.");
                return;
            }
        }

        String fullPath = getCustomMusicFolderPath() + File.separator + selectedWarningIntro;
        File file = new File(fullPath);
        if (!file.exists()) {
            System.out.println("Selected music file does not exist: " + fullPath);
            return;
        }

        System.out.println("Playing selected file: " + fullPath);
        playTempBgm(fullPath); // Play the music using the full path
        isPlaying = true;
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
                        AbstractDungeon.getCurrRoom().playBgmInstantly("BOSS_ENDING");
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
                        System.out.println("Non-boss or Elite room stop.");
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
