package com.cibernet.splatcraft.init;

import com.cibernet.splatcraft.Splatcraft;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class SplatcraftSoundEvents {
    public static SoundEvent SQUID_TRANSFORM = register("squid_transform");
    public static SoundEvent SQUID_REVERT = register("squid_revert");
    public static SoundEvent ENTITY_INK_SQUID_SUBMERGE = register("entity.ink_squid.submerge");
    public static SoundEvent ENTITY_INK_SQUID_UNSUBMERGE = register("entity.ink_squid.unsubmerge");
    public static SoundEvent INK_SURFACE = register("ink_surface");
    public static SoundEvent NO_INK = register("no_ink");
    public static SoundEvent NO_INK_SUB = register("no_ink_sub");
    public static SoundEvent SHOOTER_FIRING = register("shooter_firing");
    public static SoundEvent BLASTER_FIRING = register("blaster_firing");
    public static SoundEvent BLASTER_EXPLOSION = register("blaster_explosion");
    public static SoundEvent ROLLER_FLING = register("roller_fling");
    public static SoundEvent ROLLER_ROLL = register("roller_roll");
    public static SoundEvent CHARGER_CHARGE = register("charger_charge");
    public static SoundEvent CHARGER_READY = register("charger_ready");
    public static SoundEvent CHARGER_SHOT = register("charger_shot");
    public static SoundEvent DUALIE_FIRING = register("dualie_firing");
    public static SoundEvent DUALIE_DODGE = register("dualie_dodge");
    public static SoundEvent SLOSHER_SHOT = register("slosher_shot");
    public static SoundEvent SUB_THROW = register("sub_throw");
    public static SoundEvent SUB_DETONATING = register("sub_detonating");
    public static SoundEvent SUB_DETONATE = register("sub_detonate");
    public static SoundEvent REMOTE_USE = register("remote_use");

    public static SoundEvent BLOCK_INKED_BLOCK_WAX_ON = register("block.inked_block.wax_on");
    public static SoundEvent BLOCK_INKED_BLOCK_WAX_OFF = register("block.inked_block.wax_off");

    private static SoundEvent register(String id) {
        Identifier identifier = new Identifier(Splatcraft.MOD_ID, id);
        return Registry.register(Registry.SOUND_EVENT, identifier, new SoundEvent(identifier));
    }
}