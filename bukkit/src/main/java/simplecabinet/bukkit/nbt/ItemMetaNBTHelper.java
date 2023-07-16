package simplecabinet.bukkit.nbt;

import ca.momothereal.mojangson.ex.MojangsonParseException;
import ca.momothereal.mojangson.value.MojangsonArray;
import ca.momothereal.mojangson.value.MojangsonCompound;
import ca.momothereal.mojangson.value.MojangsonValue;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

public class ItemMetaNBTHelper implements NBTHelper {
    @Override
    @SuppressWarnings("unchecked")
    public ItemStack addNBTTag(ItemStack source, String nbtString) {
        MojangsonCompound compound = new MojangsonCompound();
        try {
            compound.read(nbtString);
            ItemMeta meta = source.getItemMeta();
            if(meta == null) return source;
            if(compound.containsKey("display")) {
                MojangsonCompound display = (MojangsonCompound) compound.get("display");
                if(display.containsKey("Name")) {
                    meta.setDisplayName((String) display.get("Name").getValue());
                }
                if(display.containsKey("Lore")) {
                    List<String> lore = (List<String>) ((MojangsonArray<?>)display.get("Lore")).getValue().stream().map(MojangsonValue::getValue).collect(Collectors.toList());
                    meta.setLore(lore);
                }
                if(display.containsKey("LocalizedName")) {
                    meta.setLocalizedName((String) display.get("LocalizedName").getValue());
                }
            }
            source.setItemMeta(meta);
        } catch (MojangsonParseException e) {
            e.printStackTrace();
        }
        return source;
    }
}
