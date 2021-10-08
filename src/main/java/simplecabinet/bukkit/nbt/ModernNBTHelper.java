package simplecabinet.bukkit.nbt;

import ca.momothereal.mojangson.ex.MojangsonParseException;
import ca.momothereal.mojangson.value.MojangsonArray;
import ca.momothereal.mojangson.value.MojangsonCompound;
import ca.momothereal.mojangson.value.MojangsonValue;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTCompoundList;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTList;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public class ModernNBTHelper implements NBTHelper {
    @Override
    public ItemStack addNBTTag(ItemStack source, String nbtString) {
        NBTItem nbti = new NBTItem(source);
        MojangsonCompound compound = new MojangsonCompound();
        try {
            compound.read(nbtString);
            mojangsonToNBT(nbti, null, compound);
        } catch (MojangsonParseException e) {
            e.printStackTrace();
        }
        return nbti.getItem();
    }

    @SuppressWarnings("unchecked")
    public void mojangsonToNBT(NBTCompound parent, String name, MojangsonValue<?> value) {
        if(value instanceof MojangsonCompound) {
            NBTCompound compound = name == null ? parent : parent.addCompound(name);
            ((MojangsonCompound) value).forEach((k,v) -> {
                mojangsonToNBT(compound, k, v);
            });
        }
        else if(value instanceof MojangsonArray<?>) {
            Class<?> type = getListType((MojangsonArray<?>) value);
            if(String.class.isAssignableFrom(type)) {
                NBTList<String> list = parent.getStringList(name);
                list.addAll((Collection<? extends String>) value.getValue());
            } else if(Integer.class.isAssignableFrom(type)) {
                NBTList<Integer> list = parent.getIntegerList(name);
                list.addAll((Collection<? extends Integer>) value.getValue());
            } else if(Long.class.isAssignableFrom(type)) {
                NBTList<Long> list = parent.getLongList(name);
                list.addAll((Collection<? extends Long>) value.getValue());
            } else if(Byte.class.isAssignableFrom(type)) {
                parent.setByteArray(name, collectByteArray((Collection<? extends Byte>) value));
            } else if(MojangsonCompound.class.isAssignableFrom(type)) {
                NBTCompoundList list = parent.getCompoundList(name);
                Collection<? extends MojangsonCompound> source = (Collection<? extends MojangsonCompound>) ((MojangsonArray<?>) value).getValue();
                for(MojangsonCompound c : source) {
                    NBTCompound node = list.addCompound();
                    mojangsonToNBT(node, null, c);
                }
            } else {
                throw new RuntimeException(String.format("Mojangson to NBTList unknown type: %s", type.getName()));
            }
        }
        else {
            Object o = value.getValue();
            if(o instanceof String) {
                parent.setString(name, (String) o);
            }
            else if(o instanceof Integer) {
                parent.setInteger(name, (Integer) o);
            }
            else if(o instanceof Long) {
                parent.setLong(name, (Long) o);
            }
            else if(o instanceof Short) {
                parent.setShort(name, (Short) o);
            }
            else if(o instanceof Byte) {
                parent.setByte(name, (Byte) o);
            }
            else if(o instanceof Float) {
                parent.setFloat(name, (Float) o);
            }
            else if(o instanceof Double) {
                parent.setDouble(name, (Double) o);
            }
            else {
                throw new RuntimeException(String.format("Mojangson to NBT unknown type: %s", o.getClass().getName()));
            }
        }
    }

    private Class<?> getListType(MojangsonArray<?> array) {
        return array.getValueClass();
    }

    private byte[] collectByteArray(Collection<? extends Byte> collection) {
        byte[] array = new byte[collection.size()];
        int i=0;
        for(Byte b : collection) {
            array[i] = b;
            i++;
        }
        return array;
    }
}
