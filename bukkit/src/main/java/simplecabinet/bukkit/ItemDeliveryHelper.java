package simplecabinet.bukkit;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import simplecabinet.api.dto.ItemDeliveryDto;
import simplecabinet.bukkit.nbt.NBTHelper;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class ItemDeliveryHelper {
    private static final Logger logger = Logger.getLogger("ItemDelivery");

    public static int deliveryItemToPlayer(Player player, ItemDeliveryDto orderSystemInfo, int part)
    {
        ItemStack stack = createItemStackFromInfo(orderSystemInfo, part);
        AtomicInteger rejectedPart = new AtomicInteger();
        player.getInventory().addItem(stack).forEach((k,v) -> {
            rejectedPart.addAndGet(v.getAmount());
        });
        return rejectedPart.get();
    }

    @SuppressWarnings("deprecation")
    public static ItemStack createItemStackFromInfo(ItemDeliveryDto info, int part)
    {
        ItemStack itemStack;
        Material material = getMaterial(info.itemName);
        try {
            itemStack = new ItemStack(material, part, info.itemExtra != null ? Short.parseShort(info.itemExtra) : 0);
        } catch (Throwable e) {
            itemStack = new ItemStack(material);
            itemStack.setAmount(part);
        }
        itemStack.setAmount(part);
        if(info.itemNbt != null) {
            NBTHelper nbtHelper = NBTHelper.newInstance();
            itemStack = nbtHelper.addNBTTag(itemStack, info.itemNbt);
        }
        if(info.itemEnchants != null) {
            for(ItemDeliveryDto.ItemEnchantDto e : info.itemEnchants) {
                Enchantment ench = getEnchantment(e.name);
                if(ench == null) continue;
                itemStack.addUnsafeEnchantment(ench, e.value);
            }
        }
        return itemStack;
    }

    public static Material getMaterial(String name) {
        String[] space = name.split(":");
        String namespace;
        String key;
        if(space.length < 2) {
            namespace = "minecraft";
            key = name;
        } else {
            namespace = space[0];
            key = space[1];
        }
        key = key.toUpperCase();
        try {
            int id = Integer.parseInt(key);
            Material material = null;
            try {
                 material = (Material) MethodHandles.lookup().findStatic(Material.class, "getMaterial", MethodType.methodType(Material.class, int.class)).invoke(id);
                 if(material != null) {
                     logger.fine( String.format("For name %s: use private getMaterial(int) method", name));
                 }
            } catch (Throwable ignored) {
            }
            if(material == null) {
                Constructor<Material> constructor = Material.class.getDeclaredConstructor(int.class);
                constructor.setAccessible(true);
                material = constructor.newInstance(id);
                logger.fine( String.format("For name %s: use private constructor", name));
            }
            return material;
        } catch (Throwable i) {
            if (!(i instanceof NumberFormatException)) {
                //
            }
        }
        if(namespace != null) {
            Material material = Material.getMaterial(namespace.toUpperCase()+"_"+key);
            if(material != null) return material;
        }
        Material material = Material.getMaterial(key);
        if(material == null) {
            throw new RuntimeException(String.format("Item Material %s not found", name));
        }
        return material;
    }

    @SuppressWarnings("deprecation")
    public static Enchantment getEnchantment(String name) {
        String[] space = name.split(":");
        String namespace;
        String key;
        if(space.length < 2) {
            namespace = "minecraft";
            key = name;
        } else {
            namespace = space[0];
            key = space[1];
        }
        key = key.toUpperCase();
        Enchantment ench;
        try {
            int id = Integer.parseInt(key);
            ench = (Enchantment) MethodHandles.lookup().findStatic(Enchantment.class, "getById", MethodType.methodType(Enchantment.class, int.class)).invoke(id);
        } catch (Throwable i) {
            if (!(i instanceof NumberFormatException)) {
                //
            }
        }
        try {
            Class.forName("org.bukkit.NamespacedKey");
            try {
                ench = Enchantment.getByKey(new NamespacedKey(namespace, key));
            } catch (Throwable ex) {
                try {
                    ench = (Enchantment) MethodHandles.lookup().findStaticGetter(Enchantment.class, key, Enchantment.class).invoke();
                } catch (Throwable ignored) {
                    ench = null;
                }
            }
        } catch (ClassNotFoundException e) {
            ench = Enchantment.getByName(key);
        }
        if(ench == null) {
            logger.warning( String.format("Ench %s:%s not found", namespace, key));
        }
        return ench;
    }
}
