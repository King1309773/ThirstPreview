package dev.rlcraftceaddons.thirstpreview;

import com.charles445.simpledifficulty.api.config.JsonConfig;
import com.charles445.simpledifficulty.api.config.ServerConfig;
import com.charles445.simpledifficulty.api.config.ServerOptions;
import com.charles445.simpledifficulty.api.config.json.JsonConsumableThirst;
import com.charles445.simpledifficulty.api.thirst.ThirstEnum;
import com.charles445.simpledifficulty.item.ItemCanteen;
import com.charles445.simpledifficulty.item.ItemDrinkBase;
import com.charles445.simpledifficulty.item.ItemJuice;
import com.charles445.simpledifficulty.item.ItemPurifiedWaterBottle;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Assume;
import org.junit.Test;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ThirstValueResolverTest {
    private Map<String, List<JsonConsumableThirst>> originalRules;
    private Map<String, String> originalSettings;

    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Before
    public void useIsolatedRules() {
        originalRules = JsonConfig.consumableThirst;
        JsonConfig.consumableThirst = new HashMap<>();
        originalSettings = new HashMap<>(ServerConfig.instance.values);
        ServerConfig.instance.put(ServerOptions.CANTEEN_DOSES, 3);
    }

    @After
    public void restoreRules() {
        JsonConfig.consumableThirst = originalRules;
        ServerConfig.instance.values.clear();
        ServerConfig.instance.values.putAll(originalSettings);
    }

    @Test
    public void unsupportedItemsAndEmptyStacksHaveNoPreview() {
        assertNull(ThirstValueResolver.resolve(ItemStack.EMPTY));
        assertNull(ThirstValueResolver.resolve(new ItemStack(Items.STICK)));
        // Milk has no built-in SD fallback: the modpack supplies its rule.
        assertNull(ThirstValueResolver.resolve(new ItemStack(Items.MILK_BUCKET)));
    }

    @Test
    public void readsPackRuleWithAbsoluteSaturation() {
        JsonConfig.consumableThirst.put("minecraft:apple", Arrays.asList(rule(1, 0.5F, -1)));
        assertValues(1, 0.5F, 0, ThirstValueResolver.resolve(new ItemStack(Items.APPLE)));
    }

    @Test
    public void matchesMetadataAndStopsAtFirstMatchingRule() {
        Item item = new Item().setHasSubtypes(true).setRegistryName("thirstpreview", "test_food");
        JsonConfig.consumableThirst.put("thirstpreview:test_food", Arrays.asList(
                rule(8, 7, 1), rule(3, 1.5F, 2), rule(20, 20, -1)));
        assertValues(3, 1.5F, 0, ThirstValueResolver.resolve(new ItemStack(item, 1, 2)));
    }

    @Test
    public void matchesPartialNestedNbtWithoutChangingTheStack() {
        Item item = new Item().setRegistryName("thirstpreview", "fluid_bottle");
        ItemStack stack = new ItemStack(item);
        NBTTagCompound fluid = new NBTTagCompound();
        fluid.setString("FluidName", "applejuice");
        fluid.setInteger("Amount", 1000);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("Fluid", fluid);
        tag.setString("Unrelated", "kept");
        stack.setTagCompound(tag);
        NBTTagCompound originalTag = tag.copy();
        JsonConfig.consumableThirst.put("thirstpreview:fluid_bottle", Arrays.asList(
                new JsonConsumableThirst(2, 0.1F, 0, -1, "{Fluid:{FluidName:\"oliveoil\"}}"),
                new JsonConsumableThirst(6, 5, 0, -1, "{Fluid:{FluidName:\"applejuice\"}}")));
        assertValues(6, 5, 0, ThirstValueResolver.resolve(stack));
        assertEquals(originalTag, stack.getTagCompound());
    }

    @Test
    public void preservesDirtyChanceFromThePack() {
        JsonConfig.consumableThirst.put("minecraft:apple", Arrays.asList(
                new JsonConsumableThirst(4, 1, 0.35F, -1)));
        assertValues(4, 1, 0.35F, ThirstValueResolver.resolve(new ItemStack(Items.APPLE)));
    }

    @Test
    public void nativeDrinkUsesItsOwnValuesUnlessThePackOverridesIt() {
        ItemDrinkBase drink = new ItemDrinkBase() {
            @Override public int getThirstLevel(ItemStack stack) { return 5; }
            @Override public float getSaturationLevel(ItemStack stack) { return 2; }
            @Override public float getDirtyChance(ItemStack stack) { return 0.25F; }
        };
        drink.setRegistryName("thirstpreview", "test_drink");
        ItemStack stack = new ItemStack(drink);
        assertValues(5, 2, 0.25F, ThirstValueResolver.resolve(stack));
        JsonConfig.consumableThirst.put("thirstpreview:test_drink", Arrays.asList(rule(9, 7, -1)));
        assertValues(9, 7, 0, ThirstValueResolver.resolve(stack));
    }

    @Test
    public void purifiedWaterBottleUsesTheNativeEnum() {
        ItemPurifiedWaterBottle bottle = new ItemPurifiedWaterBottle();
        bottle.setRegistryName("thirstpreview", "purified_water_bottle");
        assertValues(6, 3, 0, ThirstValueResolver.resolve(new ItemStack(bottle)));
    }

    @Test
    public void nativeJuicePreservesItsMetadataSpecificAmounts() {
        ItemJuice juice = new ItemJuice();
        juice.setRegistryName("thirstpreview", "juice");
        assertValues(8, 6.4F, 0, ThirstValueResolver.resolve(new ItemStack(juice, 1, 0)));
        assertValues(14, 14, 0, ThirstValueResolver.resolve(new ItemStack(juice, 1, 6)));
    }

    @Test
    public void canteenPreviewIsPerDoseAndLeavesItsNbtUnchanged() {
        ItemCanteen canteen = new ItemCanteen();
        canteen.setRegistryName("thirstpreview", "canteen");
        ItemStack stack = new ItemStack(canteen);
        canteen.setDoses(stack, ThirstEnum.NORMAL, 2);
        NBTTagCompound before = stack.getTagCompound().copy();
        assertValues(3, 0.3F, 0.75F, ThirstValueResolver.resolve(stack));
        assertEquals(before, stack.getTagCompound());
    }

    @Test
    public void emptyCanteenDoesNotPreviewItsOldWaterType() {
        ItemCanteen canteen = new ItemCanteen();
        canteen.setRegistryName("thirstpreview", "empty_canteen");
        ItemStack stack = new ItemStack(canteen);
        canteen.setDoses(stack, ThirstEnum.PURIFIED, 0);
        assertNull(ThirstValueResolver.resolve(stack));
    }

    @Test
    public void inspectingUninitializedCanteenNeverCreatesRealNbt() {
        ItemCanteen canteen = new ItemCanteen();
        canteen.setRegistryName("thirstpreview", "new_canteen");
        ItemStack stack = new ItemStack(canteen);
        assertNull(stack.getTagCompound());
        assertNull(ThirstValueResolver.resolve(stack));
        assertNull(stack.getTagCompound());
    }

    @Test
    public void vanillaWaterAndPotionFallbacksMatchSimpleDifficulty() {
        assertValues(3, 0.3F, 0.75F, ThirstValueResolver.resolve(potion(Items.POTIONITEM, PotionTypes.WATER)));
        assertValues(3, 0.3F, 0.75F, ThirstValueResolver.resolve(potion(Items.POTIONITEM, PotionTypes.AWKWARD)));
        assertValues(2, 0.2F, 0, ThirstValueResolver.resolve(potion(Items.POTIONITEM, PotionTypes.HEALING)));
        assertNull(ThirstValueResolver.resolve(potion(Items.POTIONITEM, PotionTypes.EMPTY)));
        assertNull(ThirstValueResolver.resolve(potion(Items.SPLASH_POTION, PotionTypes.WATER)));
        assertNull(ThirstValueResolver.resolve(potion(Items.LINGERING_POTION, PotionTypes.WATER)));
    }

    @Test
    public void potionNbtRuleOverridesTheVanillaWaterFallback() {
        JsonConfig.consumableThirst.put("minecraft:potion", Arrays.asList(
                new JsonConsumableThirst(2, 0.2F, 0, -1, "{Potion:\"minecraft:awkward\"}")));
        assertValues(2, 0.2F, 0, ThirstValueResolver.resolve(potion(Items.POTIONITEM, PotionTypes.AWKWARD)));
        assertValues(3, 0.3F, 0.75F, ThirstValueResolver.resolve(potion(Items.POTIONITEM, PotionTypes.WATER)));
    }

    @Test
    public void readsTheRealModpackFileWhenExplicitlyProvided() throws Exception {
        String path = System.getProperty("thirstpreview.packRules");
        Assume.assumeNotNull(path);
        try (Reader reader = Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
            JsonConfig.consumableThirst = new Gson().fromJson(reader,
                    new TypeToken<Map<String, List<JsonConsumableThirst>>>() { }.getType());
        }
        assertEquals(119, JsonConfig.consumableThirst.size());
        assertValues(1, 0.5F, 0, ThirstValueResolver.resolve(new ItemStack(Items.APPLE)));
        assertValues(3, 1.5F, 0, ThirstValueResolver.resolve(new ItemStack(Items.MELON)));
        assertValues(4, 1, 0, ThirstValueResolver.resolve(new ItemStack(Items.MILK_BUCKET)));
        assertValues(2, 0.2F, 0, ThirstValueResolver.resolve(potion(Items.POTIONITEM, PotionTypes.AWKWARD)));
        Item bottle = new Item().setRegistryName("rustic", "fluid_bottle");
        ItemStack stack = new ItemStack(bottle);
        NBTTagCompound fluid = new NBTTagCompound();
        fluid.setString("FluidName", "applejuice");
        fluid.setInteger("Amount", 1000);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("Fluid", fluid);
        stack.setTagCompound(tag);
        assertValues(6, 5, 0, ThirstValueResolver.resolve(stack));
    }

    private static JsonConsumableThirst rule(int amount, float saturation, int metadata) {
        return new JsonConsumableThirst(amount, saturation, 0, metadata);
    }

    private static ItemStack potion(Item item, net.minecraft.potion.PotionType type) {
        return PotionUtils.addPotionToItemStack(new ItemStack(item), type);
    }

    private static void assertValues(int amount, float saturation, float dirtyChance, ThirstValues values) {
        assertNotNull("Expected a supported thirst restoration", values);
        assertEquals(amount, values.amount);
        assertEquals(saturation, values.saturation, 0.0001F);
        assertEquals(dirtyChance, values.dirtyChance, 0.0001F);
    }
}
