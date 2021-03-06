package com.direwolf20.logisticslasers.common.network.packets;

import com.direwolf20.logisticslasers.common.container.cards.PolyFilterContainer;
import com.direwolf20.logisticslasers.common.container.cards.TagFilterContainer;
import com.direwolf20.logisticslasers.common.items.logiccards.CardInserterTag;
import com.direwolf20.logisticslasers.common.items.logiccards.CardPolymorph;
import com.direwolf20.logisticslasers.common.tiles.InventoryNodeTile;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.items.ItemStackHandler;

import java.util.function.Supplier;

public class PacketButtonSetOrRemove {
    private BlockPos sourcePos;
    private String tag;

    public PacketButtonSetOrRemove(BlockPos pos, String tag) {
        this.sourcePos = pos;
        this.tag = tag;
    }

    public static void encode(PacketButtonSetOrRemove msg, PacketBuffer buffer) {
        buffer.writeBlockPos(msg.sourcePos);
        buffer.writeString(msg.tag);
    }

    public static PacketButtonSetOrRemove decode(PacketBuffer buffer) {
        return new PacketButtonSetOrRemove(buffer.readBlockPos(), buffer.readString(255));

    }

    public static class Handler {
        public static void handle(PacketButtonSetOrRemove msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayerEntity sender = ctx.get().getSender();
                if (sender == null)
                    return;

                Container container = sender.openContainer;

                ItemStack itemStack;

                if (container instanceof TagFilterContainer) {
                    itemStack = ((TagFilterContainer) container).filterItemStack;
                    if (itemStack.getItem() instanceof CardInserterTag) {
                        CardInserterTag.removeTag(itemStack, msg.tag);
                    }
                } else if (container instanceof PolyFilterContainer) {
                    itemStack = ((PolyFilterContainer) container).filterItemStack;
                    if (itemStack.getItem() instanceof CardPolymorph) {
                        World world = sender.getServerWorld();
                        TileEntity te = world.getTileEntity(msg.sourcePos);
                        if (te instanceof InventoryNodeTile) {
                            CardPolymorph.setListFromContainer(itemStack, ((InventoryNodeTile) te).getHandler().orElse(new ItemStackHandler(0)));
                            ((InventoryNodeTile) te).markDirtyClient();
                        }
                    }
                } else {
                    return;
                }
            });

            ctx.get().setPacketHandled(true);
        }
    }
}
