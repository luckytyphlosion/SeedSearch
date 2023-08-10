package seedsearch;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.core.Settings.GameLanguage;
import com.megacrit.cardcrawl.helpers.SeedHelper;
import com.megacrit.cardcrawl.unlock.UnlockTracker;

import java.util.ArrayList;

import static java.lang.System.exit;

import java.text.MessageFormat;

public class SeedSearch {

    public static boolean loadingEnabled = true;
    public static SearchSettings settings;

    private static void unlockBosses(String[] bosslist, int unlockLevel) {
        for (int i = 0; i < unlockLevel; i++) {
            if (i >= 3) {
                break;
            }
            UnlockTracker.unlockPref.putInteger(bosslist[i], 2);
            UnlockTracker.bossSeenPref.putInteger(bosslist[i], 1);
        }
    }

    private static boolean isPlayerClassValid(SearchSettings settings) {
        if (settings.playerClass == null) {
            System.out.println("Invalid playerClass specified in search settings.");
            System.out.println("Possible values: ");
            for (AbstractPlayer.PlayerClass c: AbstractPlayer.PlayerClass.values()) {
                System.out.println(c.name());
            }
            return false;
        }
        return true;
    }

    public static void search() {
        loadingEnabled = false;
        settings = SearchSettings.loadSettings();
        if (!isPlayerClassValid(settings)) {
            exit(1);
        }
        String[] expectedBaseUnlocks = {"The Silent", "Defect", "Watcher"};
        String[] firstBossUnlocks = {"GUARDIAN", "GHOST", "SLIME"};
        String[] secondBossUnlocks = {"CHAMP", "AUTOMATON", "COLLECTOR"};
        String[] thirdBossUnlocks = {"CROW", "DONUT", "WIZARD"};
        UnlockTracker.unlockPref.data.clear();
        UnlockTracker.bossSeenPref.data.clear();
        for (String key : expectedBaseUnlocks) {
            UnlockTracker.unlockPref.putInteger(key, 2);
        }
        unlockBosses(firstBossUnlocks, settings.firstBoss);
        unlockBosses(secondBossUnlocks, settings.secondBoss);
        unlockBosses(thirdBossUnlocks, settings.thirdBoss);
        UnlockTracker.resetUnlockProgress(AbstractPlayer.PlayerClass.IRONCLAD);
        UnlockTracker.unlockProgress.putInteger("IRONCLADUnlockLevel", settings.ironcladUnlocks);
        UnlockTracker.resetUnlockProgress(AbstractPlayer.PlayerClass.THE_SILENT);
        UnlockTracker.unlockProgress.putInteger("THE_SILENTUnlockLevel", settings.silentUnlocks);
        UnlockTracker.resetUnlockProgress(AbstractPlayer.PlayerClass.DEFECT);
        UnlockTracker.unlockProgress.putInteger("DEFECTUnlockLevel", settings.defectUnlocks);
        UnlockTracker.resetUnlockProgress(AbstractPlayer.PlayerClass.WATCHER);
        UnlockTracker.unlockProgress.putInteger("WATCHERUnlockLevel", settings.watcherUnlocks);
        UnlockTracker.retroactiveUnlock();
        UnlockTracker.refresh();
        Settings.setLanguage(GameLanguage.ENG, true);

        if (settings.doFastSearch) {
            SeedRunnerFast fastRunner = new SeedRunnerFast(settings);
            boolean findSeedSuccess = fastRunner.findSeed(settings.startSeed, settings.endSeed);
            if (findSeedSuccess) {
                long foundSeed = fastRunner.getFoundSeed();
                boolean isRight = fastRunner.getIsRight();

                System.out.println(MessageFormat.format("Seed: {0} ({1}). Pick rewards on: {2}\n", SeedHelper.getString(foundSeed), foundSeed, isRight ? "right" : "left"));
            } else {
                System.out.println("Could not find a seed!");
            }
        } else {
            SeedRunner runner = new SeedRunner(settings);
            ArrayList<Long> foundSeeds = new ArrayList<>();
            
            for (long seed = settings.startSeed; seed < settings.endSeed; seed++) {
                if (runner.runSeed(seed)) {
                    foundSeeds.add(seed);
                    if (settings.verbose) {
                        SeedResult.printSeedStatsScore();
                    }
                    break;
                }
                if (seed % 10000 == 0) {
                    System.out.println("seed: " + seed);
                }
            }
            System.out.println(String.format("%d seeds found: ", foundSeeds.size()));
            System.out.println(foundSeeds);            
        }

        if (settings.exitAfterSearch) {
            exit(0);
        } else {
            System.out.println("Search complete. Manually close this program when finished.");
        }
    }

}
