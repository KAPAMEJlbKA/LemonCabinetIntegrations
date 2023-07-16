package simplecabinet.fabric;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import simplecabinet.api.dto.ItemDeliveryDto;

import java.util.Optional;

public class ItemDeliveryHelper {
    public static ParsedItemInfo parse(ItemDeliveryDto dto) {
        Item item = getItem(dto.itemName);
        if(item == null) {
            throw new IllegalArgumentException(String.format("item %s not found", dto.itemName));
        }
        int count = Math.min((int)dto.part, item.getMaxCount());
        try {
            Optional<NbtCompound> nbt = makeNbt(dto);
            return new ParsedItemInfo(item, nbt);
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static int delivery(ParsedItemInfo info, ServerPlayerEntity player, int part) {
        ItemStack megaStack = info.makeStack(part);
        while (!megaStack.isEmpty()) {
            int inserted = miniOffer(player.getInventory(), megaStack);
            if (inserted <= 0) {
                break;
            }
        }
        return part - megaStack.getCount();
    }

    public static int miniOffer(PlayerInventory inventory, ItemStack stack) {
        if(stack.isEmpty()) {
            return 0;
        }
        int i = inventory.getOccupiedSlotWithRoomForStack(stack);
        if (i == -1) {
            i = inventory.getEmptySlot();
        }

        if (i != -1) {
            int j = stack.getMaxCount() - inventory.getStack(i).getCount();
            if (inventory.insertStack(i, stack.split(j)) && inventory.player instanceof ServerPlayerEntity) {
                ((ServerPlayerEntity)inventory.player).networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(-2, 0, i, inventory.getStack(i)));
            }
            return j;
        }
        return 0;
    }

    public record ParsedItemInfo(Item item, Optional<NbtCompound> nbt) {
        public ItemStack makeStack(int count) {
            var stack = new ItemStack(item, count);
            nbt.ifPresent(stack::setNbt);
            return stack;
        }
    }

    public static Optional<NbtCompound> makeNbt(ItemDeliveryDto dto) throws CommandSyntaxException {
        NbtCompound compound = dto.itemExtra == null ? null : StringNbtReader.parse(dto.itemNbt);
        if(dto.itemEnchants != null && !dto.itemEnchants.isEmpty()) {
            if(compound == null) {
                compound = new NbtCompound();
            }
            NbtList enchants = compound.getList("Enchantments", NbtElement.COMPOUND_TYPE);
            for(var e : dto.itemEnchants) {
                NbtCompound enchant = new NbtCompound();
                enchant.put("id", NbtString.of(e.name));
                enchant.put("lvl", NbtShort.of((short) e.value));
                enchants.add(enchant);
            }
            compound.put("Enchantments", enchants);
        }
        return Optional.ofNullable(compound);
    }

    public static Item getItem(String name) {
        try {
            int num = Integer.parseInt(name);
            return Registries.ITEM.get(num);
        } catch (NumberFormatException ignored) {

        }
        Identifier identifier = Identifier.tryParse(name);
        if(identifier == null) {
            return null;
        }
        return Registries.ITEM.get(identifier);
    }
}
