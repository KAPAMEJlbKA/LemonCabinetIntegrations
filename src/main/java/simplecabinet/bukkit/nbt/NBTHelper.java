package simplecabinet.bukkit.nbt;

import org.bukkit.inventory.ItemStack;

public interface NBTHelper {
    ItemStack addNBTTag(ItemStack source, String nbtString);

    static boolean isPowerNBTLoaded() {
        try {
            Class.forName("me.dpohvar.powernbt.nbt.NBTContainerItem");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    static boolean isModernNBTLoaded() {
        try {
            Class.forName("de.tr7zw.nbtapi.NBTItem");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static NBTHelper newInstance() {
        if(isModernNBTLoaded()) {
            return new ModernNBTHelper();
        }
        return new ItemMetaNBTHelper();
    }
}
