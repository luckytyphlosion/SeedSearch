package seedsearch;

import java.util.ArrayList;
import java.util.Map;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.characters.CharacterManager;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.random.Random;

public class SeedRunnerFast {
    private SearchSettings settings;
    private Random cardRng;
    private Random cardRandomRng;
    private ArrayList<AbstractCard> characterCards;
    private ArrayList<AbstractCard> commonCards;
    private ArrayList<AbstractCard> uncommonCards;
    private ArrayList<AbstractCard> rareCards;
    private int numUncommonColorlessCards;
    private int numRareColorlessCards;

    private final int desiredScore;
    private final int desiredScoreMinusEncyclopedian;
    private final boolean doAllstar;

    private int[] commonCardsNumeric;
    private int[] uncommonCardsNumeric;
    private int[] rareCardsNumeric;
    private int[] allCardsNumeric;
    private int[] uncommonColorlessCardsNumeric;
    private int[] rareColorlessCardsNumeric;

    private int[] deckHistogram;
    private int[] rightDeckHistogram;
    private int[] blankDeckHistogram;
    
    private boolean isRight;
    private long foundSeed;
    
    public SeedRunnerFast(SearchSettings settings) {
        this.settings = settings;
        this.doAllstar = this.settings.doAllstar;

        this.settings.checkIds();
        this.characterCards = new ArrayList<>();
        this.commonCards = new ArrayList<>();
        this.uncommonCards = new ArrayList<>();
        this.rareCards = new ArrayList<>();
        this.isRight = false;
        this.desiredScore = this.settings.desiredScore;
        this.desiredScoreMinusEncyclopedian = this.desiredScore - 50;
        this.initialize();
    }

    private void addColorlessCards() {
        for (Map.Entry<String, AbstractCard> c : (Iterable<Map.Entry<String, AbstractCard>>)CardLibrary.cards.entrySet()) {
            AbstractCard card = c.getValue();
            if (card.color == AbstractCard.CardColor.COLORLESS && card.rarity != AbstractCard.CardRarity.BASIC && card.rarity != AbstractCard.CardRarity.SPECIAL && card.type != AbstractCard.CardType.STATUS) {
                if (card.rarity == AbstractCard.CardRarity.UNCOMMON) {
                    this.numUncommonColorlessCards++;
                } else if (card.rarity == AbstractCard.CardRarity.RARE) {
                    this.numRareColorlessCards++;
                }
            }
        }
    }

    private void initialize() {
        AbstractPlayer.PlayerClass playerClass = settings.playerClass;

        if (playerClass.equals(AbstractPlayer.PlayerClass.IRONCLAD)) {
            CardLibrary.addRedCards(this.characterCards);
        } else if (playerClass.equals(AbstractPlayer.PlayerClass.THE_SILENT)) {
            CardLibrary.addGreenCards(this.characterCards);
        } else if (playerClass.equals(AbstractPlayer.PlayerClass.DEFECT)) {
            CardLibrary.addBlueCards(this.characterCards);
        } else if (playerClass.equals(AbstractPlayer.PlayerClass.WATCHER)) {
            CardLibrary.addPurpleCards(this.characterCards);
        }

        for (AbstractCard c : this.characterCards) {
            switch (c.rarity) {
            case COMMON:
                commonCards.add(0, c);
                break;
            case UNCOMMON:
                uncommonCards.add(0, c);
                break;
            case RARE:
                rareCards.add(0, c);
                break;
            }
            
        }

        final int numCommonCards = commonCards.size();
        final int numUncommonCards = uncommonCards.size();
        final int numRareCards = rareCards.size();
        final int numCards = numCommonCards + numUncommonCards + numRareCards;

        this.commonCardsNumeric = new int[numCommonCards];
        this.uncommonCardsNumeric = new int[numUncommonCards];
        this.rareCardsNumeric = new int[numRareCards];
        this.allCardsNumeric = new int[numCards];

        for (int i = 0; i < numCommonCards; i++) {
            commonCardsNumeric[i] = i;
        }

        for (int i = 0; i < numUncommonCards; i++) {
            uncommonCardsNumeric[i] = i + numCommonCards;
        }

        for (int i = 0; i < numRareCards; i++) {
            rareCardsNumeric[i] = i + numCommonCards + numRareCards;
        }

        for (int i = 0; i < numCards; i++) {
            allCardsNumeric[i] = i;
        }

        this.blankDeckHistogram = new int[numCommonCards + numUncommonCards + numRareCards];
        this.deckHistogram = new int[numCommonCards + numUncommonCards + numRareCards];
        this.rightDeckHistogram = new int[numCommonCards + numUncommonCards + numRareCards];

        this.addColorlessCards();
    }

    private void addCardNumeric(int cardNumeric) {
        this.deckHistogram[cardNumeric]++;
    }

    private void addCardNumeric(int cardNumeric, int amount) {
        this.deckHistogram[cardNumeric] += amount;
    }

    private void addCardNumeric(int[] providedDeckHistogram, int cardNumeric, int amount) {
        providedDeckHistogram[cardNumeric] += amount;
    }

    private static final int COMMON = 0;
    private static final int UNCOMMON = 1;
    private static final int RARE = 2;

    /*
    private AbstractCard.CardRarity rollRarity() {
        int roll = cardRng.random(99) + 5;
        if (roll < 3)
            return AbstractCard.CardRarity.RARE; 
        if (roll < 40) {
            return AbstractCard.CardRarity.UNCOMMON;
        }
        return AbstractCard.CardRarity.COMMON;
    }

    private AbstractCard returnRandomCard() {
        AbstractCard.CardRarity rarity = this.rollRarity();
        ArrayList<AbstractCard> chosenCards;

        if (rarity.equals(AbstractCard.CardRarity.COMMON)) {
            chosenCards = this.commonCards;
        } else if (rarity.equals(AbstractCard.CardRarity.UNCOMMON)) {
            chosenCards = this.uncommonCards;
        } else {
            chosenCards = this.rareCards;
        } 

        return chosenCards.get(cardRandomRng.random(chosenCards.size() - 1));
    }
    */

    private int rollRarity() {
        int roll = this.cardRng.random(99) + 5;
        if (roll < 3)
            return RARE; 
        if (roll < 40) {
            return UNCOMMON;
        }
        return COMMON;
    }

    private int returnRandomCard() {
        int rarity = this.rollRarity();
        int[] chosenCards;

        if (rarity == COMMON) {
            chosenCards = this.commonCardsNumeric;
        } else if (rarity == UNCOMMON) {
            chosenCards = this.uncommonCardsNumeric;
        } else {
            chosenCards = this.rareCardsNumeric;
        }

        return chosenCards[this.cardRandomRng.random(chosenCards.length - 1)];
    }

    private int returnTrulyRandomCard() {
        return this.allCardsNumeric[this.cardRandomRng.random(this.allCardsNumeric.length - 1)];
    }

    private void doAllstarRngCalls() {
        for (int i = 0; i < 5; i++) {
            if (cardRng.randomBoolean(0.5F)) {
                // rare
                this.cardRng.random(this.numRareColorlessCards - 1);
            } else {
                // uncommon
                this.cardRng.random(this.numUncommonColorlessCards - 1);
            }
        }
    }

    private void addInsanityCards() {
        for (int i = 0; i < 50; i++) {
            this.addCardNumeric(this.returnRandomCard());
        }
    }

    private void addSpecializedCards() {
        int specializedCardNumeric = this.returnTrulyRandomCard();
        this.addCardNumeric(specializedCardNumeric, 15);
    }

    private void addDraftCards() {
        System.arraycopy(this.deckHistogram, 0, this.rightDeckHistogram, 0, this.deckHistogram.length);

        for (int i = 0; i < 15; i++) {
            int firstCardNumeric = this.returnRandomCard();
            int secondCardNumeric;
            int thirdCardNumeric;

            do {
                secondCardNumeric = this.returnRandomCard();
            } while (firstCardNumeric == secondCardNumeric);

            do {
                thirdCardNumeric = this.returnRandomCard();
            } while (firstCardNumeric == thirdCardNumeric || secondCardNumeric == thirdCardNumeric);

            if (i == 0) {
                this.addCardNumeric(this.deckHistogram, secondCardNumeric, 3);
                this.addCardNumeric(this.rightDeckHistogram, secondCardNumeric, 3);                
            } else {
                this.addCardNumeric(this.deckHistogram, firstCardNumeric, 3);
                this.addCardNumeric(this.rightDeckHistogram, thirdCardNumeric, 3);                
            }
        }
    }

    private boolean testScore(int[] providedDeckHistogram) {
        int score = 0;

        for (int i = 0; i < providedDeckHistogram.length; i++) {
            if (providedDeckHistogram[i] >= 4) {
                score += 25;
                if (score >= desiredScoreMinusEncyclopedian) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean runSeed(long currentSeed) {
        this.cardRng = new Random(currentSeed);
        this.cardRandomRng = new Random(currentSeed);
        System.arraycopy(this.blankDeckHistogram, 0, this.deckHistogram, 0, this.deckHistogram.length);
        this.addInsanityCards();
        if (this.doAllstar) {
            this.doAllstarRngCalls();
        }

        this.addSpecializedCards();
        this.addDraftCards();
        
        if (this.testScore(this.deckHistogram)) {
            this.isRight = false;
            return true;
        } else if (this.testScore(this.rightDeckHistogram)) {
            this.isRight = true;
            return true;
        } else {
            return false;
        }
    }

    public boolean findSeed(long startSeed, long endSeed) {
        for (long currentSeed = startSeed; currentSeed < endSeed; currentSeed++) {
            if (currentSeed % 1000000 == 0) {
                System.out.println("seed: " + currentSeed);
            }

            if (this.runSeed(currentSeed)) {
                this.foundSeed = currentSeed;
                return true;
            }
        }
        return false;
    }

    public long getFoundSeed() {
        return this.foundSeed;
    }

    public boolean getIsRight() {
        return this.isRight;
    }
}
