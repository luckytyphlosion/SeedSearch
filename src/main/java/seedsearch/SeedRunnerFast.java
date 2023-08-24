package seedsearch;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private ArrayList<AbstractCard> characterCards;
    private ArrayList<AbstractCard> commonCards;
    private ArrayList<AbstractCard> uncommonCards;
    private ArrayList<AbstractCard> rareCards;

    private int numUncommonColorlessCards;
    private int numRareColorlessCards;

    private final AbstractPlayer.PlayerClass playerClass;
    private final int desiredScore;
    private final int desiredScoreMinusEncyclopedian;
    private final boolean doAllstar;
    private final boolean insanityOnly;
    private final boolean doSpecialized;
    private final boolean exactScore;
    private final int numSeedsToFind;
    private final int numThreads;
    private final boolean centerOnly;

    private int[] commonCardsNumeric;
    private int[] uncommonCardsNumeric;
    private int[] rareCardsNumeric;
    private int[] uncommonColorlessCardsNumeric;
    private int[] rareColorlessCardsNumeric;
    private int[] allColoredCardsNumeric;

    private int[] blankDeckHistogram;

    private final boolean noOrientation;
    private int foundSeedsIndex;
    private final long foundSeeds[];
    private final boolean foundSeedsIsRight[];

    public SeedRunnerFast(SearchSettings settings, int desiredScore, int numSeedsToFind, AbstractPlayer.PlayerClass playerClass) {
        //this.settings = settings;
        this.playerClass = playerClass;
        this.doAllstar = settings.doAllstar;
        this.insanityOnly = settings.insanityOnly;
        this.doSpecialized = settings.doSpecialized;
        this.exactScore = settings.exactScore;
        this.centerOnly = settings.centerOnly;
        this.numSeedsToFind = numSeedsToFind;
        this.numThreads = settings.numThreads;
        this.foundSeeds = new long[this.numSeedsToFind];
        this.foundSeedsIsRight = new boolean[this.numSeedsToFind];
        this.foundSeedsIndex = 0;

        //settings.checkIds();
        this.characterCards = new ArrayList<>();
        this.commonCards = new ArrayList<>();
        this.uncommonCards = new ArrayList<>();
        this.rareCards = new ArrayList<>();
        this.noOrientation = this.centerOnly || this.insanityOnly;
        this.desiredScore = desiredScore;
        this.desiredScoreMinusEncyclopedian = this.desiredScore - 50;
        this.initialize();
    }

    private void addColorlessCards(ArrayList<AbstractCard> uncommonColorlessCards, ArrayList<AbstractCard> rareColorlessCards) {
        for (Map.Entry<String, AbstractCard> c : (Iterable<Map.Entry<String, AbstractCard>>)CardLibrary.cards.entrySet()) {
            AbstractCard card = c.getValue();
            if (card.color == AbstractCard.CardColor.COLORLESS && card.rarity != AbstractCard.CardRarity.BASIC && card.rarity != AbstractCard.CardRarity.SPECIAL && card.type != AbstractCard.CardType.STATUS) {
                if (card.rarity == AbstractCard.CardRarity.UNCOMMON) {
                    uncommonColorlessCards.add(0, card);
                } else if (card.rarity == AbstractCard.CardRarity.RARE) {
                    rareColorlessCards.add(0, card);
                }
            }
        }
    }

    private void initialize() {
        if (this.playerClass.equals(AbstractPlayer.PlayerClass.IRONCLAD)) {
            CardLibrary.addRedCards(this.characterCards);
        } else if (this.playerClass.equals(AbstractPlayer.PlayerClass.THE_SILENT)) {
            CardLibrary.addGreenCards(this.characterCards);
        } else if (this.playerClass.equals(AbstractPlayer.PlayerClass.DEFECT)) {
            CardLibrary.addBlueCards(this.characterCards);
        } else if (this.playerClass.equals(AbstractPlayer.PlayerClass.WATCHER)) {
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

        ArrayList<AbstractCard> uncommonColorlessCards = new ArrayList<>();
        ArrayList<AbstractCard> rareColorlessCards = new ArrayList<>();

        this.addColorlessCards(uncommonColorlessCards, rareColorlessCards);
        
        final int numCommonCards = commonCards.size();
        final int numUncommonCards = uncommonCards.size();
        final int numRareCards = rareCards.size();
        final int numUncommonColorlessCards, numRareColorlessCards;
        
        this.numUncommonColorlessCards = uncommonColorlessCards.size();
        this.numRareColorlessCards = rareColorlessCards.size();

        if (this.doAllstar && this.insanityOnly) {
            numUncommonColorlessCards = this.numUncommonColorlessCards;
            numRareColorlessCards = this.numRareColorlessCards;
        } else {
            numUncommonColorlessCards = 0;
            numRareColorlessCards = 0;
        }
        
        final int numCards = numCommonCards + numUncommonCards + numRareCards + numUncommonColorlessCards + numRareColorlessCards;

        this.commonCardsNumeric = new int[numCommonCards];
        this.uncommonCardsNumeric = new int[numUncommonCards];
        this.rareCardsNumeric = new int[numRareCards];
        this.uncommonColorlessCardsNumeric = new int[numUncommonColorlessCards];
        this.rareColorlessCardsNumeric = new int[numRareColorlessCards];
        this.allColoredCardsNumeric = new int[numCards];

        for (int i = 0; i < numCommonCards; i++) {
            commonCardsNumeric[i] = i;
        }

        for (int i = 0; i < numUncommonCards; i++) {
            uncommonCardsNumeric[i] = i + numCommonCards;
        }

        for (int i = 0; i < numRareCards; i++) {
            rareCardsNumeric[i] = i + numCommonCards + numUncommonCards;
        }

        for (int i = 0; i < numUncommonColorlessCards; i++) {
            uncommonColorlessCardsNumeric[i] = i + numCommonCards + numUncommonCards + numRareCards;
        }

        for (int i = 0; i < numRareColorlessCards; i++) {
            rareColorlessCardsNumeric[i] = i + numCommonCards + numUncommonCards + numRareCards + numUncommonColorlessCards;
        }

        for (int i = 0; i < numCommonCards + numUncommonCards + numRareCards; i++) {
            allColoredCardsNumeric[i] = i;
        }

        this.blankDeckHistogram = new int[numCards];
    }

    public class SeedRunnerFastRunnable implements Runnable {
        private final long startSeed;
        private final long endSeed;
        private final int threadNum;

        // mutated variables
        private Random cardRng;
        private Random cardRandomRng;
        private int[] deckHistogram;
        private int[] rightDeckHistogram;
        private boolean isRight;
        private boolean success;

        public SeedRunnerFastRunnable(long startSeed, long endSeed, int threadNum) {
            this.startSeed = startSeed;
            this.endSeed = endSeed;
            this.threadNum = threadNum;
            this.deckHistogram = new int[SeedRunnerFast.this.blankDeckHistogram.length];
            this.rightDeckHistogram = new int[SeedRunnerFast.this.blankDeckHistogram.length];
            this.isRight = false;
            this.success = false;
            System.out.println("created threadNum: " + threadNum);
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
                chosenCards = SeedRunnerFast.this.commonCardsNumeric;
            } else if (rarity == UNCOMMON) {
                chosenCards = SeedRunnerFast.this.uncommonCardsNumeric;
            } else {
                chosenCards = SeedRunnerFast.this.rareCardsNumeric;
            }

            return chosenCards[this.cardRandomRng.random(chosenCards.length - 1)];
        }

        private int returnTrulyRandomCard() {
            return SeedRunnerFast.this.allColoredCardsNumeric[this.cardRandomRng.random(SeedRunnerFast.this.allColoredCardsNumeric.length - 1)];
        }

        private void doAllstarRngCalls() {
            for (int i = 0; i < 5; i++) {
                if (cardRng.randomBoolean(0.5F)) {
                    // rare
                    this.cardRng.random(SeedRunnerFast.this.numRareColorlessCards - 1);
                } else {
                    // uncommon
                    this.cardRng.random(SeedRunnerFast.this.numUncommonColorlessCards - 1);
                }
            }
        }

        private void addAllstarCards() {
            for (int i = 0; i < 5; i++) {
                if (cardRng.randomBoolean(0.5F)) {
                    this.addCardNumeric(SeedRunnerFast.this.rareColorlessCardsNumeric[this.cardRng.random(SeedRunnerFast.this.rareColorlessCardsNumeric.length - 1)], 3);
                } else {
                    this.addCardNumeric(SeedRunnerFast.this.uncommonColorlessCardsNumeric[this.cardRng.random(SeedRunnerFast.this.uncommonColorlessCardsNumeric.length - 1)], 3);
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

            if (SeedRunnerFast.this.centerOnly) {
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

                    this.addCardNumeric(this.deckHistogram, secondCardNumeric, 3);
                }
            } else {
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
        }

        private boolean testScore(int[] providedDeckHistogram) {
            int score = 0;

            if (!SeedRunnerFast.this.exactScore) {
                for (int i = 0; i < providedDeckHistogram.length; i++) {
                    if (providedDeckHistogram[i] >= 4) {
                        score += 25;
                        if (score >= desiredScoreMinusEncyclopedian) {
                            return true;
                        }
                    }
                }
            } else {
                for (int i = 0; i < providedDeckHistogram.length; i++) {
                    if (providedDeckHistogram[i] >= 4) {
                        score += 25;
                    }
                }
                if (score == desiredScoreMinusEncyclopedian) {
                    return true;
                }
            }

            return false;
        }

        public boolean runSeed(long currentSeed) {
            this.cardRng = new Random(currentSeed);
            this.cardRandomRng = new Random(currentSeed);
            System.arraycopy(SeedRunnerFast.this.blankDeckHistogram, 0, this.deckHistogram, 0, this.deckHistogram.length);
            this.addInsanityCards();
            if (SeedRunnerFast.this.doAllstar) {
                if (SeedRunnerFast.this.insanityOnly) {
                    this.addAllstarCards();
                } else {
                    this.doAllstarRngCalls();                
                }
            }

            if (SeedRunnerFast.this.doSpecialized) {
                this.addSpecializedCards();            
            }

            if (!SeedRunnerFast.this.insanityOnly) {
                this.addDraftCards();
            }

            if (SeedRunnerFast.this.noOrientation) {
                if (this.testScore(this.deckHistogram)) {
                    this.isRight = false;
                    return true;
                } else {
                    return false;
                }
            } else {
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
        }

        public void run() {
            System.out.println("[" + this.threadNum + "] seed: " + startSeed);

            for (long currentSeed = startSeed; currentSeed < endSeed; currentSeed++) {
                if ((currentSeed & 0x7ffffff) == 0) {
                    System.out.println("[" + this.threadNum + "] seed: " + currentSeed);
                    if (Thread.interrupted()) {
                        System.out.println("Exited via interrupt!");
                        this.success = false;
                        return;
                    }
                }

                if (this.runSeed(currentSeed)) {
                    if (SeedRunnerFast.this.addSeed(currentSeed, this.isRight)) {
                        this.success = true;
                        return;
                    }
                }
            }
            this.success = SeedRunnerFast.this.foundAnySeeds();
        }

        public boolean successful() {
            return this.success;
        }
    }

    public boolean findSeed(long startSeed, long endSeed) {
        ArrayList<SeedRunnerFastRunnable> tasks = new ArrayList<>();
        final long numSeedsToCheck = endSeed - startSeed;
        final BigInteger numSeedsToCheckBig = BigInteger.valueOf(numSeedsToCheck);
        final BigInteger startSeedBig = BigInteger.valueOf(startSeed);
        final BigInteger numThreadsBig = BigInteger.valueOf(this.numThreads);
        ExecutorService executor = Executors.newFixedThreadPool(this.numThreads);

        for (int i = 0; i < this.numThreads; i++) {
            BigInteger iBig = BigInteger.valueOf(i);
            BigInteger iPlus1Big = BigInteger.valueOf(i + 1);

            long threadStartSeed = iBig.multiply(numSeedsToCheckBig).divide(numThreadsBig).add(startSeedBig).longValue();
            long threadEndSeed = iPlus1Big.multiply(numSeedsToCheckBig).divide(numThreadsBig).add(startSeedBig).longValue();

            System.out.printf("threadStartSeed: %d, threadEndSeed: %d\n", threadStartSeed, threadEndSeed);
            SeedRunnerFastRunnable task = new SeedRunnerFastRunnable(threadStartSeed, threadEndSeed, i);
            tasks.add(task);
        }

        CompletableFuture<?> future = CompletableFuture.anyOf(
            tasks.stream()
                .map(task -> CompletableFuture.runAsync(task, executor))
                .toArray(CompletableFuture[]::new)
        );

        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        boolean wasSuccessful = false;

        for (SeedRunnerFastRunnable task : tasks) {
            if (task.successful()) {
                wasSuccessful = true;
                break;
            }
        }

        executor.shutdownNow();

        return wasSuccessful;
    }

    public synchronized boolean addSeed(long seed, boolean isRight) {
        this.foundSeeds[this.foundSeedsIndex] = seed;
        this.foundSeedsIsRight[this.foundSeedsIndex++] = isRight;
        return this.foundSeedsIndex >= this.numSeedsToFind;
    }

    public synchronized boolean foundAnySeeds() {
        return this.foundSeedsIndex != 0;
    }

    public ArrayList<SeedResultSimple> getFoundSeedsResults() {
        ArrayList<SeedResultSimple> foundSeedsResults = new ArrayList<>();
        for (int i = 0; i < this.numSeedsToFind; i++) {
            foundSeedsResults.add(new SeedResultSimple(this.foundSeeds[i], this.foundSeedsIsRight[i]));
        }

        return foundSeedsResults;
    }

    public boolean hasOrientation() {
        return !this.noOrientation;
    }
}



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

