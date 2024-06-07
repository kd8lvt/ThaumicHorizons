//
// Decompiled by Procyon v0.5.30
//

package com.kentington.thaumichorizons.client.renderer.tile;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.kentington.thaumichorizons.client.renderer.model.ModelNodeInfuser;
import com.kentington.thaumichorizons.common.tiles.TileNodeInfuser;

import thaumcraft.client.lib.UtilsFX;
import thaumcraft.client.renderers.tile.TileNodeRenderer;

public class TileNodeInfuserRender extends TileEntitySpecialRenderer {

    private final ModelNodeInfuser model;
    private final TileNodeRenderer nodeRenderer = new TileNodeRenderer();
    static String tx1;

    public TileNodeInfuserRender() {
        this.model = new ModelNodeInfuser();
    }

    public void renderTileEntityAt(final TileEntity p_147500_1_, final double x, final double y, final double z,
            final float p_147500_8_) {
        final TileNodeInfuser te = (TileNodeInfuser) p_147500_1_;
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glTranslatef((float) x + 0.5f, (float) y, (float) z + 0.5f);
        GL11.glTranslatef(0.0f, -0.5f, 0.0f);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glPopMatrix();
        UtilsFX.bindTexture("thaumichorizons", TileNodeInfuserRender.tx1);
        this.model.render(null, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0625f);
        GL11.glPopMatrix();
    }

    static {
        TileNodeInfuserRender.tx1 = "textures/models/nodeInfuser.png";
    }
}
