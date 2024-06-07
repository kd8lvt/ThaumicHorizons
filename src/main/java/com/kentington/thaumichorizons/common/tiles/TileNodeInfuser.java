package com.kentington.thaumichorizons.common.tiles;

import java.util.Arrays;
import java.util.Objects;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.common.network.NetworkRegistry;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.TileThaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXBlockZap;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileNodeStabilizer;

public class TileNodeInfuser extends TileThaumcraft implements IAspectContainer, IEssentiaTransport {

    AspectList essentia = new AspectList();
    int timer = 0;
    boolean hasMadeConnection = false;

    public TileNodeInfuser() {}

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (worldObj.isRemote) return; // Don't do things on the client. It doesn't end well.
        this.tryDrawEssentia();
        TileEntity tile = this.worldObj.getTileEntity(this.xCoord, this.yCoord - 1, this.zCoord);
        if (!(tile instanceof TileNode node)) {
            this.hasMadeConnection = false;
            return;
        }
        if (this.hasMadeConnection) PacketHandler.INSTANCE.sendToAllAround(
                new PacketFXBlockZap(
                        (float) (this.xCoord) + 0.5F,
                        (float) (this.yCoord - 1) + 0.5F,
                        (float) (this.zCoord) + 0.5F,
                        (float) this.xCoord + 0.5F,
                        (float) this.yCoord,
                        (float) this.zCoord + 0.5F),
                new NetworkRegistry.TargetPoint(
                        this.worldObj.provider.dimensionId,
                        (double) this.xCoord,
                        (double) this.yCoord,
                        (double) this.zCoord,
                        32.0D));
        this.timer++; // Increment timer.
        if (this.timer >= 300) {
            if (worldObj.getBlockPowerInput(xCoord, yCoord, zCoord) > 0) {
                this.hasMadeConnection = false;
            } else if (this.essentia.visSize() > 0) {
                this.hasMadeConnection = true;
            }
            this.timer = 0;
        } else if (this.hasMadeConnection && this.timer >= 20) {
            this.timer = 0;
            if (node.getNodeType().equals(NodeType.UNSTABLE)) return;
            tryInfuseNode(node); // Add aspects
            tryDowngradeNode(
                    node,
                    this.worldObj
                            .getTileEntity(this.xCoord, this.yCoord - 2, this.zCoord) instanceof TileNodeStabilizer);
        }
    }

    void tryDrawEssentia() {
        TileEntity te = null;
        IEssentiaTransport ic = null;
        ForgeDirection dir = ForgeDirection.UP;
        te = ThaumcraftApiHelper.getConnectableTile(this.worldObj, this.xCoord, this.yCoord, this.zCoord, dir);
        if (te != null) {
            ic = (IEssentiaTransport) te;
            if (ic.getEssentiaAmount(dir.getOpposite()) > 0
                    && ic.getSuctionAmount(dir.getOpposite()) < this.getSuctionAmount(null)
                    && this.getSuctionAmount(null) >= ic.getMinimumSuction()) {
                for (final Aspect asp : ThaumcraftApiHelper.getAllAspects(1).getAspects()) {
                    final int ess = ic.takeEssentia(asp, 1, dir.getOpposite());
                    if (ess > 0) {
                        this.addToContainer(asp, ess);
                        return;
                    }
                }
            }
        }
    }

    public void tryDowngradeNode(TileNode node, boolean stabilized) {
        float chance = 0.01f; // 1% every second
        if (stabilized) chance *= 0.1; // or 0.1% every second when stabilized
        if (chance > this.worldObj.rand.nextFloat()) return;
        NodeModifier curMod = node.getNodeModifier();
        if (curMod == NodeModifier.BRIGHT) node.setNodeModifier((NodeModifier) null);
        else if (curMod == null) node.setNodeModifier(NodeModifier.PALE);
        else if (curMod.equals(NodeModifier.PALE) && !stabilized) node.setNodeModifier(NodeModifier.FADING);
        else if (curMod.equals(NodeModifier.FADING) && !stabilized) node.setNodeType(NodeType.UNSTABLE);
        else return;
        node.markDirty();
    }

    public void tryInfuseNode(TileNode node) {
        if (this.essentia.visSize() > 0) {
            Aspect asp = this.essentia.getAspects()[this.worldObj.rand.nextInt(essentia.size())];
            int complexity = getAspectComplexity(asp);
            if (!this.essentia.reduce(asp, complexity)) return;
            this.markDirty();
            this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
            if (Arrays.asList(node.getAspectsBase().getAspects()).contains(asp)) {
                float nodeAspSaturation = (float) node.getNodeVisBase(asp) / node.getAspectsBase().visSize();
                if (nodeAspSaturation > 0.75) return;
                if (this.worldObj.rand.nextFloat() > nodeAspSaturation) {
                    node.setNodeVisBase(asp, (short) (node.getNodeVisBase(asp) + 1));
                    node.setAspects(node.getAspects().add(asp, 1));
                }
            } else {
                node.setNodeVisBase(asp, (short) (node.getNodeVisBase(asp) + 1));
                node.setAspects(node.getAspects().add(asp, 1));
            }
            node.markDirty();
            this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord - 1, this.zCoord);
        }
    }

    public int getAspectComplexity(Aspect asp) {
        int complexity = 0;
        if (asp.isPrimal()) complexity += 1;
        else {
            Aspect[] comps = asp.getComponents();
            for (Aspect comp : comps) complexity += getAspectComplexity(comp);
        }
        return complexity;
    }

    public void cleanupEssentia() {
        for (Aspect asp : essentia.getAspects()) if (essentia.getAmount(asp) <= 0) {
            essentia.remove(asp);
            this.markDirty();
            this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
        }
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbttagcompound) {
        NBTTagList tlist = new NBTTagList();
        nbttagcompound.setTag("essentia", tlist);
        for (final Aspect aspect : this.essentia.getAspects()) {
            if (aspect != null) {
                final NBTTagCompound f = new NBTTagCompound();
                f.setString("key", aspect.getTag());
                f.setInteger("amount", this.essentia.getAmount(aspect));
                tlist.appendTag(f);
            }
        }
        nbttagcompound.setByte("connected", this.hasMadeConnection ? (byte) 1 : (byte) 0);
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbttagcompound) {
        AspectList al = new AspectList();
        NBTTagList tlist = nbttagcompound.getTagList("essentia", 10);
        for (int j = 0; j < tlist.tagCount(); ++j) {
            final NBTTagCompound rs = tlist.getCompoundTagAt(j);
            if (rs.hasKey("key")) {
                if (Aspect.getAspect(rs.getString("key")) == null) continue;
                al.add(Aspect.getAspect(rs.getString("key")), rs.getInteger("amount"));
            }
        }
        if (Arrays.stream(al.getAspects()).anyMatch(Objects::isNull)) return;
        this.essentia = al.copy();

        this.hasMadeConnection = nbttagcompound.getByte("connected") != 0;
    }

    @Override
    public AspectList getAspects() {
        this.cleanupEssentia();
        return this.essentia;
    }

    @Override
    public void setAspects(AspectList var1) {}

    @Override
    public boolean doesContainerAccept(Aspect var1) {
        return var1 != null;
    }

    @Override
    public int addToContainer(Aspect var1, int var2) {
        if (var1 == null) return 0;
        essentia.add(var1, var2);
        this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
        this.markDirty();
        return var2;
    }

    @Override
    public boolean takeFromContainer(Aspect var1, int var2) {
        return false;
    }

    @Override
    public boolean takeFromContainer(AspectList var1) {
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect var1, int var2) {
        return this.containerContains(var1) >= var2;
    }

    @Override
    public boolean doesContainerContain(AspectList var1) {
        return false;
    }

    @Override
    public int containerContains(Aspect var1) {
        return essentia.getAmount(var1);
    }

    @Override
    public boolean isConnectable(ForgeDirection var1) {
        return var1.equals(ForgeDirection.UP);
    }

    @Override
    public boolean canInputFrom(ForgeDirection var1) {
        return var1.equals(ForgeDirection.UP);
    }

    @Override
    public boolean canOutputTo(ForgeDirection var1) {
        return false;
    }

    @Override
    public void setSuction(Aspect var1, int var2) {}

    @Override
    public Aspect getSuctionType(ForgeDirection var1) {
        return null;
    }

    @Override
    public int getSuctionAmount(ForgeDirection var1) {
        return 65;
    }

    @Override
    public int takeEssentia(Aspect var1, int var2, ForgeDirection var3) {
        return 0;
    }

    @Override
    public int addEssentia(Aspect var1, int var2, ForgeDirection var3) {
        return this.canInputFrom(var3) ? (var2 - this.addToContainer(var1, var2)) : 0;
    }

    @Override
    public Aspect getEssentiaType(ForgeDirection var1) {
        return null;
    }

    @Override
    public int getEssentiaAmount(ForgeDirection var1) {
        return 0;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }
}
