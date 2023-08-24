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
        if (settings.playerClass == null || settings.playerClasses == null) {
            System.out.println("Invalid playerClass or playerClasses specified in search settings.");
            System.out.println("Possible values: ");
            for (AbstractPlayer.PlayerClass c: AbstractPlayer.PlayerClass.values()) {
                System.out.println(c.name());
            }
            return false;
        }
        return true;
    }

    public static void fastSearchLoop() {
        for (AbstractPlayer.PlayerClass playerClass : settings.playerClasses) {
            System.out.println("Working on " + playerClass + "!");
            final int desiredScoresSize = settings.desiredScores.size();

            for (int i = 0; i < desiredScoresSize; i++) {
                int desiredScore = settings.desiredScores.get(i);
                int numSeedsToFind = settings.numSeedsToFindTargeted.get(i);

                System.out.println("Working on finding " + numSeedsToFind + " seeds with score " + desiredScore + "!");
                SeedRunnerFast fastRunner = new SeedRunnerFast(settings, desiredScore, numSeedsToFind, playerClass);
                boolean findSeedSuccess = fastRunner.findSeed(settings.startSeed, settings.endSeed);
                if (findSeedSuccess) {
                    ArrayList<SeedResultSimple> foundSeedsResults = fastRunner.getFoundSeedsResults();

                    StringBuilder foundSeedResultsOutput = new StringBuilder();

                    for (SeedResultSimple foundSeedResult : foundSeedsResults) {
                        String foundSeedResultStr = MessageFormat.format(
                            "Seed: {0} ({1}).",
                            SeedHelper.getString(foundSeedResult.seed),
                            foundSeedResult.seed
                        );
                        if (settings.insanityOnly) {
                            foundSeedResultStr += "\n";
                        } else {
                            String orientation;
                            if (settings.centerOnly) {
                                orientation = "center";
                            } else {
                                orientation = foundSeedResult.isRight ? "right" : "left";
                            }
                            foundSeedResultStr += " Pick rewards on: " + orientation + "\n";
                        }
                        foundSeedResultsOutput.append(foundSeedResultStr);
                    }
                    System.out.println(foundSeedResultsOutput.toString());
                } else {
                    System.out.println("Could not find a seed!");
                }
            }
        }
    }

    public static void search() {
        loadingEnabled = false;
        settings = SearchSettings.loadSettings();
        if (isPlayerClassValid(settings)) {
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
                if (settings.playerClasses.size() == 0) {
                    settings.playerClasses.add(settings.playerClass);
                }
                if (settings.desiredScores.size() == 0) {
                    settings.desiredScores.add(settings.desiredScore);
                }
                if (settings.numSeedsToFindTargeted.size() == 0) {
                    settings.numSeedsToFindTargeted.add(settings.numSeedsToFind);
                }
                if (settings.desiredScores.size() != settings.numSeedsToFindTargeted.size()) {
                    System.out.println("Error: desiredScores and numSeedsToFindTargeted do not have the same length!");
                } else {
                    SeedSearch.fastSearchLoop();
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

}
