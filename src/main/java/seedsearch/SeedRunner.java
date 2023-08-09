package seedsearch;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.colorless.JAX;
import com.megacrit.cardcrawl.cards.colorless.Madness;
import com.megacrit.cardcrawl.cards.curses.*;
import com.megacrit.cardcrawl.cards.red.Strike_Red;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.characters.CharacterManager;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.*;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.events.beyond.*;
import com.megacrit.cardcrawl.events.city.*;
import com.megacrit.cardcrawl.events.exordium.*;
import com.megacrit.cardcrawl.events.shrines.*;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.EventHelper;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.helpers.PotionHelper;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.map.MapEdge;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.neow.NeowEvent;
import com.megacrit.cardcrawl.neow.NeowReward;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.relics.*;
import com.megacrit.cardcrawl.rewards.chests.AbstractChest;
import com.megacrit.cardcrawl.rewards.chests.BossChest;
import com.megacrit.cardcrawl.rooms.*;
import com.megacrit.cardcrawl.screens.CharSelectInfo;
import com.megacrit.cardcrawl.shop.Merchant;
import com.megacrit.cardcrawl.shop.ShopScreen;
import com.megacrit.cardcrawl.shop.StorePotion;
import com.megacrit.cardcrawl.shop.StoreRelic;
import seedsearch.patches.AbstractRoomPatch;
import seedsearch.patches.CardRewardScreenPatch;
import seedsearch.patches.EventHelperPatch;
import seedsearch.patches.ShowCardAndObtainEffectPatch;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static com.megacrit.cardcrawl.helpers.MonsterHelper.*;


public class SeedRunner {

    private AbstractPlayer player;
    private CardGroup rightMasterDeck;

    private SearchSettings settings;
    private long currentSeed;

    public SeedRunner(SearchSettings settings) {
        this.settings = settings;
        AbstractDungeon.fadeColor = Settings.SHADOW_COLOR;
        CharacterManager characterManager = new CharacterManager();
        CardCrawlGame.characterManager = characterManager;
        characterManager.setChosenCharacter(settings.playerClass);
        currentSeed = settings.startSeed;
        AbstractDungeon.ascensionLevel = settings.ascensionLevel;
        Settings.seedSet = true;
        this.settings.checkIds();
        this.rightMasterDeck = null;
    }

    private void setSeed(long seed) {
        Settings.seed = seed;
        currentSeed = seed;
        AbstractDungeon.generateSeeds();
        player = AbstractDungeon.player;
        AbstractDungeon.reset();
        resetCharacter();
        SeedResult.setSeed(currentSeed);
    }

    private void resetCharacter() {
        player.relics = new ArrayList<>();
        try {
            Method starterRelicsMethod = AbstractPlayer.class.getDeclaredMethod("initializeStarterRelics", AbstractPlayer.PlayerClass.class);
            starterRelicsMethod.setAccessible(true);
            starterRelicsMethod.invoke(player, settings.playerClass);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException("Reflection error when initializing player relics");
        }
        player.potions = new ArrayList<>();
        player.masterDeck = new CardGroup(CardGroup.CardGroupType.MASTER_DECK);
        CharSelectInfo info = player.getLoadout();
        player.maxHealth = info.maxHp;
        player.gold = info.gold;

        // Remove the MockMusic tracks that would otherwise pile up
        CardCrawlGame.music.dispose();
        CardCrawlGame.music.update();
    }

    private void addSpecializedCards() {
        AbstractCard specializedCard = AbstractDungeon.returnTrulyRandomCard();
        for (int i = 0; i < 5; i++) {
            AbstractCard specializedCardCopy = specializedCard.makeCopy();
            this.addCardHoarder(specializedCardCopy, player.masterDeck);
        }
    }

    private void addCardHoarder(AbstractCard card, CardGroup masterDeck) {
        masterDeck.addToTop(card);
        if (ModHelper.isModEnabled("Hoarder")) {
            masterDeck.addToTop(card.makeStatEquivalentCopy());
            masterDeck.addToTop(card.makeStatEquivalentCopy());
        } 
    }

    private void addDraftCards() {
        this.rightMasterDeck = new CardGroup(player.masterDeck, CardGroup.CardGroupType.MASTER_DECK);

        for (int i = 0; i < 15; i++) {
            ArrayList<AbstractCard> curDraftCardRewards = new ArrayList<>();
            while (curDraftCardRewards.size() != 3) {
                boolean dupe = false;
                AbstractCard potentialDraftCardReward = AbstractDungeon.returnRandomCard();
                for (AbstractCard draftCardReward : curDraftCardRewards) {
                    if (draftCardReward.cardID.equals(potentialDraftCardReward.cardID)) {
                        dupe = true;
                        break;
                    } 
                } 
                if (!dupe) {
                    curDraftCardRewards.add(potentialDraftCardReward.makeCopy());
                }
            }
            AbstractCard pickedCard;

            // pick middle on first pick
            if (i == 0) {
                pickedCard = curDraftCardRewards.get(1);
                this.addCardHoarder(pickedCard, player.masterDeck);
                this.addCardHoarder(pickedCard, rightMasterDeck);
            } else {
                this.addCardHoarder(curDraftCardRewards.get(0), player.masterDeck);
                this.addCardHoarder(curDraftCardRewards.get(2), rightMasterDeck);
            }
        }
    }
    /*
    private void testAddInsanityCards() {
        //Random myCardRng = new Random(Settings.seed);
        //StringBuilder output = new StringBuilder();
        //for (int i = 0; i < 50; i++) {
        //    output.append(AbstractDungeon.returnRand)
        //}
        ArrayList<AbstractCard> ironcladUnlockedCards = new ArrayList<>();
        CardLibrary.addRedCards(ironcladUnlockedCards);
        StringBuilder output = new StringBuilder();
        for (AbstractCard c : ironcladUnlockedCards) {
            output.append(c.toString() + ", ");
        }
        System.out.println(output.toString());
    }*/

    private boolean runSeed() {
        //testAddInsanityCards();

        //System.out.println("seed: " + Settings.seed +  ", cardRng.counter: " + AbstractDungeon.cardRng.counter + ", cardRandomRng.counter: " + AbstractDungeon.cardRandomRng.counter);
        
        AbstractDungeon exordium = new Exordium(player, new ArrayList<>());
        //System.out.println("after exordium gen: cardRng.counter: " + AbstractDungeon.cardRng.counter + ", cardRandomRng.counter: " + AbstractDungeon.cardRandomRng.counter);

        AbstractDungeon.getCurrMapNode().room = new EmptyRoom();

        this.addSpecializedCards();
        this.addDraftCards();
        return SeedResult.testFinalFiltersScore(player.masterDeck, rightMasterDeck, settings);
    }

    public boolean runSeed(long seed) {
        setSeed(seed);
        ModHelper.setMods(Arrays.asList(new String[]{"Draft", "Hoarder", "Insanity", "Specialized"}));

        return runSeed();
    }
}
