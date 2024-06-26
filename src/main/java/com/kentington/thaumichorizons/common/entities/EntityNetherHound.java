//
// Decompiled by Procyon v0.5.30
//

package com.kentington.thaumichorizons.common.entities;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.world.World;

import com.kentington.thaumichorizons.common.items.ItemFocusContainment;

import thaumcraft.common.entities.projectile.EntityEmber;

public class EntityNetherHound extends EntityWolf {

    long soundDelay;

    public EntityNetherHound(final World p_i1696_1_) {
        super(p_i1696_1_);
        this.soundDelay = 0L;
        this.isImmuneToFire = true;
    }

    public void onLivingUpdate() {
        super.onLivingUpdate();
        EntityLivingBase target = null;
        if (this.getAITarget() != null) {
            target = this.getAITarget();
        }
        if (this.getAttackTarget() != null) {
            target = this.getAttackTarget();
        }
        if (target != null && ItemFocusContainment.getPointedEntity(this.worldObj, this, 7.0) == target) {
            if (!this.worldObj.isRemote && this.soundDelay < System.currentTimeMillis()) {
                this.worldObj.playSoundAtEntity(this, "thaumcraft:fireloop", 0.33f, 2.0f);
                this.soundDelay = System.currentTimeMillis() + 500L;
            }
            final float scatter = 8.0f;
            final EntityEmber orb = new EntityEmber(this.worldObj, this, scatter);
            orb.damage = 1.0f;
            orb.firey = 1;
            orb.posX += orb.motionX;
            orb.posY += orb.motionY;
            orb.posZ += orb.motionZ;
            this.worldObj.spawnEntityInWorld(orb);
        }
    }
}
