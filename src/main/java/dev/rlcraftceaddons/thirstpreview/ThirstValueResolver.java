package dev.rlcraftceaddons.thirstpreview;

import com.charles445.simpledifficulty.api.SDPotions;
import com.charles445.simpledifficulty.api.config.JsonConfig;
import com.charles445.simpledifficulty.api.config.json.JsonConsumableThirst;
import com.charles445.simpledifficulty.api.item.IItemCanteen;
import com.charles445.simpledifficulty.api.thirst.ThirstEnum;
import com.charles445.simpledifficulty.item.ItemDrinkBase;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Reads SimpleDifficulty 0.3.9's loaded rules without consuming items or changing NBT.
 *
 * Native canteens are read per dose. This pack has no JSON canteen overrides;
 * externally added rules that stack with native canteen effects are unsupported.
 * Other custom effects applied directly by scripts or mods are not inferred.
 */
public final class ThirstValueResolver {
    private ThirstValueResolver() { }

    @Nullable
    public static ThirstValues resolve(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        Item item = stack.getItem();
        if (item instanceof IItemCanteen) {
            // SD canteen getters can initialize or repair tags, even during reads.
            ItemStack copy = stack.copy();
            IItemCanteen canteen = (IItemCanteen) item;
            return canteen.isCanteenEmpty(copy) ? null : fromEnum(canteen.getThirstEnum(copy));
        }

        ResourceLocation itemName = item.getRegistryName();
        List<JsonConsumableThirst> rules = itemName == null ? null
                : JsonConfig.consumableThirst.get(itemName.toString());
        if (rules != null) {
            for (JsonConsumableThirst rule : rules) {
                if (rule != null && rule.matches(stack)) {
                    // SD adds saturation as absolute points, unlike food modifiers.
                    return new ThirstValues(rule.amount, rule.saturation, rule.thirstyChance);
                }
            }
        }

        if (item instanceof ItemDrinkBase) {
            ItemDrinkBase drink = (ItemDrinkBase) item;
            ItemStack copy = stack.copy();
            return new ThirstValues(drink.getThirstLevel(copy), drink.getSaturationLevel(copy),
                    drink.getDirtyChance(copy));
        }

        // ThirstHandler only supplies fallback values for the drinkable potion item.
        if (item == Items.POTIONITEM) {
            PotionType type = PotionUtils.getPotionFromItem(stack);
            ResourceLocation typeName = type.getRegistryName();
            if (typeName == null) {
                return null;
            }
            if ("minecraft".equals(typeName.getNamespace())) {
                if (type == PotionTypes.WATER || type == PotionTypes.AWKWARD
                        || type == PotionTypes.MUNDANE || type == PotionTypes.THICK) {
                    return fromEnum(ThirstEnum.NORMAL);
                }
                return type == PotionTypes.EMPTY ? null : fromEnum(ThirstEnum.POTION);
            }
            if (SDPotions.potionTypes.containsValue(type)) {
                return fromEnum(ThirstEnum.POTION);
            }
        }
        return null;
    }

    @Nullable
    private static ThirstValues fromEnum(@Nullable ThirstEnum type) {
        return type == null ? null
                : new ThirstValues(type.getThirst(), type.getSaturation(), type.getThirstyChance());
    }
}
