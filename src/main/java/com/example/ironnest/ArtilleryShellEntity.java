package com.example.ironnest;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class ArtilleryShellEntity extends ThrowableItemProjectile {
    public static final float EXPLOSION_POWER = 4.0F;

    public ArtilleryShellEntity(EntityType<? extends ArtilleryShellEntity> type, Level level) {
        super(type, level);
    }

    public ArtilleryShellEntity(Level level, double x, double y, double z) {
        super(IronNestMod.SHELL.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return IronNestMod.ARTILLERY_SHELL.get();
    }

    @Override
    protected float getGravity() {
        return 0.04F; // тяжёлый снаряд: падает быстрее обычного броска
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            level().explode(this, getX(), getY(), getZ(), EXPLOSION_POWER, Level.ExplosionInteraction.TNT);
            discard();
        }
    }
}
