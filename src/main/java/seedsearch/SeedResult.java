package seedsearch;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.SeedHelper;
import com.megacrit.cardcrawl.neow.NeowReward;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.relics.AbstractRelic;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SeedResult {

    private static long seed;
    private static boolean isRight;
    private static String scoreStats;

    static {
        SeedResult.seed = 0;
        SeedResult.isRight = false;
        SeedResult.scoreStats = "";
    }

    private SeedResult() {
        
    }

    public static void setSeed(long seed) {
        SeedResult.seed = seed;
    }

    public static void setHasDesiredScoreTrueAndIsRight(boolean isRight) {
        SeedResult.isRight = isRight;
    }

    private static void formatDeck(ArrayList<String> cardNames, StringBuilder output) {
        String prevCardName = cardNames.get(0);
        int curCardCount = 0;

        for (String cardName : cardNames) {
            if (!cardName.equals(prevCardName)) {
                output.append(String.format("%s x%d\n", prevCardName, curCardCount));
                curCardCount = 0;
                prevCardName = cardName;
            }
            curCardCount++;
        }
    }

    public static boolean testFinalFiltersScore(CardGroup leftMasterDeck, CardGroup rightMasterDeck, SearchSettings settings) {
        int leftScore = 50;
        int rightScore = 50;
        leftScore += leftMasterDeck.fullSetCheck() * 25;
        rightScore += rightMasterDeck.fullSetCheck() * 25;
        CardGroup chosenMasterDeck;

        if (leftScore >= settings.desiredScore) {
            chosenMasterDeck = leftMasterDeck;
            SeedResult.setHasDesiredScoreTrueAndIsRight(false);
        } else if (rightScore >= settings.desiredScore) {
            chosenMasterDeck = rightMasterDeck;
            SeedResult.setHasDesiredScoreTrueAndIsRight(true);
        } else {
            return false;
        }

        ArrayList<String> cardNames = chosenMasterDeck.getCardNames();
        ArrayList<String> insanityCardNames = new ArrayList<>(cardNames.subList(0, 50));
        //Collections.sort(insanityCardNames);
        String specializedCardName = cardNames.get(51);

        StringBuilder output = new StringBuilder();
        output.append(MessageFormat.format("Seed: {0} ({1}). Pick rewards on: {2}\n", SeedHelper.getString(seed), seed, SeedResult.isRight ? "right" : "left"));

        output.append("== Insanity Deck ==\n");
        SeedResult.formatDeck(insanityCardNames, output);

        output.append("\nSpecialized Card: " + specializedCardName + "\n");
        output.append("\n== Full Deck ==\n");
        Collections.sort(cardNames);

        SeedResult.formatDeck(cardNames, output);

        SeedResult.scoreStats = output.toString();

        return true;
    }

    public static void printSeedStatsScore() {
        System.out.println(SeedResult.scoreStats);
    }
}
