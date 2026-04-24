package systems.kinau.fishingbot.utils;

import systems.kinau.fishingbot.FishingBot;
import systems.kinau.fishingbot.bot.Enchantment;
import systems.kinau.fishingbot.bot.Inventory;
import systems.kinau.fishingbot.bot.Item;
import systems.kinau.fishingbot.bot.Slot;
import systems.kinau.fishingbot.bot.registry.Registries;
import systems.kinau.fishingbot.bot.registry.Registry;
import systems.kinau.fishingbot.bot.registry.legacy.LegacyMaterial;
import systems.kinau.fishingbot.network.item.ComponentItemData;
import systems.kinau.fishingbot.network.item.NBTItemData;
import systems.kinau.fishingbot.network.item.datacomponent.components.NBTComponent;
import systems.kinau.fishingbot.network.protocol.ProtocolConstants;
import systems.kinau.fishingbot.utils.nbt.NBTTag;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ItemUtils {

    private static final Map<Integer, Integer> rodId = new HashMap<>();
    private static final Map<Integer, Set<Integer>> fishIds = new HashMap<>();
    private static final Map<Integer, Integer> enchantedBookId = new HashMap<>();

    public static int getRodId(int protocolId) {
        if (rodId.containsKey(protocolId))
            return rodId.get(protocolId);

        Registry<Integer, String> itemRegistry = Registries.ITEM.getRegistry(protocolId);
        if (itemRegistry.containsValue("minecraft:fishing_rod")) {
            int id = itemRegistry.findKey("minecraft:fishing_rod");
            rodId.put(protocolId, id);
            return id;
        }
        return 563;
    }

    public static boolean isFishingRod(Slot slot) {
        if (slot == null)
            return false;
        if (!slot.isPresent())
            return false;
        int protocol = ProtocolConstants.getLatest();
        if (FishingBot.getInstance().getCurrentBot() != null)
            protocol = FishingBot.getInstance().getCurrentBot().getServerProtocol();

        if (protocol < ProtocolConstants.MC_1_13)
            return LegacyMaterial.getMaterial(slot.getItemId()) == LegacyMaterial.FISHING_ROD;
        else
            return getRodId(protocol) == slot.getItemId();
    }

    public static boolean isFish(int protocol, int itemId) {
        if (protocol < ProtocolConstants.MC_1_13)
            return itemId == LegacyMaterial.RAW_FISH.getId();
        if (fishIds.containsKey(protocol))
            return fishIds.get(protocol).contains(itemId);
        Set<Integer> ids = new HashSet<>();
        Registry<Integer, String> registry = Registries.ITEM.getRegistry(protocol);
        ids.add(registry.findKey("minecraft:cod"));
        ids.add(registry.findKey("minecraft:salmon"));
        ids.add(registry.findKey("minecraft:tropical_fish"));
        ids.add(registry.findKey("minecraft:pufferfish"));
        fishIds.put(protocol, ids);
        return ids.contains(itemId);
    }

    public static boolean isEnchantedBook(int protocol, int itemId) {
        if (protocol < ProtocolConstants.MC_1_13)
            return itemId == LegacyMaterial.ENCHANTED_BOOK.getId();
        if (enchantedBookId.containsKey(protocol))
            return enchantedBookId.get(protocol) == itemId;
        int ebId = Registries.ITEM.findKey("minecraft:enchanted_book", protocol);
        enchantedBookId.put(protocol, ebId);
        return ebId == itemId;
    }

    public static List<Enchantment> getEnchantments(Slot slot) {
        List<Enchantment> enchantmentList = new ArrayList<>();
        if (slot.getItemData() == null) return enchantmentList;
        return slot.getItemData().getEnchantments();
    }

    public static int getDamage(Slot slot) {
        if (slot == null)
            return -1;
        return slot.getItemDamage();
    }

    public static int getFishingRodValue(Slot slot) {
        if (!isFishingRod(slot))
            return -100;
        if (FishingBot.getInstance().getCurrentBot().getConfig().isPreventRodBreaking() && ItemUtils.getDamage(slot) >= 63)
            return -100;
        List<Enchantment> enchantments = getEnchantments(slot);
        int luckOfTheSeaLevel = enchantments.stream()
                .filter(enchantment -> enchantment.getEnchantmentType().equals("minecraft:luck_of_the_sea"))
                .map(Enchantment::getLevel).findAny().orElse(0);
        int lureLevel = enchantments.stream()
                .filter(enchantment -> enchantment.getEnchantmentType().equals("minecraft:lure"))
                .map(Enchantment::getLevel).findAny().orElse(0);
        int unbreakingLevel = enchantments.stream()
                .filter(enchantment -> enchantment.getEnchantmentType().equals("minecraft:unbreaking"))
                .map(Enchantment::getLevel).findAny().orElse(0);
        int mendingLevel = enchantments.stream()
                .filter(enchantment -> enchantment.getEnchantmentType().equals("minecraft:mending"))
                .map(Enchantment::getLevel).findAny().orElse(0);
        int vanishingCurseLevel = enchantments.stream()
                .filter(enchantment -> enchantment.getEnchantmentType().equals("minecraft:vanishing_curse"))
                .map(Enchantment::getLevel).findAny().orElse(0);
        return luckOfTheSeaLevel * 9 + lureLevel * 9 + unbreakingLevel * 2 + mendingLevel - vanishingCurseLevel;
    }

    public static int getBestFishingRod(Inventory inventory) {
        AtomicInteger currentBestSlotId = new AtomicInteger(-1);
        AtomicInteger currentBestValue = new AtomicInteger(-100);
        if (inventory == null)
            return -1;
        inventory.getContent().forEach((slotId, slot) -> {
            int currValue = getFishingRodValue(slot);
            if (currValue > currentBestValue.get()) {
                currentBestValue.set(currValue);
                currentBestSlotId.set(slotId);
            }
        });
        return currentBestSlotId.get();
    }

    public static String getItemName(Slot slot) {
        if (FishingBot.getInstance().getCurrentBot() == null || !slot.isPresent())
            return "N/A";
        int version = FishingBot.getInstance().getCurrentBot().getServerProtocol();
        if (version <= ProtocolConstants.MC_1_12_2) {
            return LegacyMaterial.getMaterialName(slot.getItemId(), Integer.valueOf(slot.getItemDamage()).shortValue());
        } else {
            return Registries.ITEM.getItemName(slot.getItemId(), version).replace("minecraft:", "");
        }
    }

    public static boolean matchesNbtFilter(Slot slot, List<String> nbtAllowList) {
        if (nbtAllowList == null || nbtAllowList.isEmpty())
            return true;
        if (slot == null || !slot.isPresent() || slot.getItemData() == null)
            return false;

        List<String> nbtCandidates = new ArrayList<>();
        if (slot.getItemData() instanceof NBTItemData) {
            NBTTag legacyTag = ((NBTItemData) slot.getItemData()).getNbtData();
            if (legacyTag != null && legacyTag.getTag() != null) {
                nbtCandidates.add(legacyTag.getTag().toJson().toString().toLowerCase(Locale.ROOT));
            }
        } else if (slot.getItemData() instanceof ComponentItemData) {
            ComponentItemData componentItemData = (ComponentItemData) slot.getItemData();
            componentItemData.getPresentComponents().stream()
                    .filter(dataComponent -> dataComponent instanceof NBTComponent)
                    .map(dataComponent -> (NBTComponent) dataComponent)
                    .map(NBTComponent::getTag)
                    .filter(Objects::nonNull)
                    .map(NBTTag::getTag)
                    .filter(Objects::nonNull)
                    .forEach(tag -> nbtCandidates.add(tag.toJson().toString().toLowerCase(Locale.ROOT)));
        }

        if (nbtCandidates.isEmpty())
            return false;
        for (String filter : nbtAllowList) {
            if (filter == null || filter.trim().isEmpty())
                continue;
            String loweredFilter = filter.toLowerCase(Locale.ROOT);
            if (nbtCandidates.stream().anyMatch(candidate -> candidate.contains(loweredFilter)))
                return true;
        }
        return false;
    }

    public static String getImageURL(Item item) {
        String fileType = (item.getEnchantments() == null || item.getEnchantments().isEmpty()) ? "png" : "gif";
        String itemName;
        if (item.getName() != null)
            itemName = item.getName().toLowerCase();
        else
            itemName = "air";
        return String.format("https://raw.githubusercontent.com/MrKinau/FishingBot/master/src/main/resources/img/items/%s." + fileType, itemName).replace(" ", "%20");
    }
}
