package com.cibernet.splatcraft.init;

import com.cibernet.splatcraft.Splatcraft;
import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class SplatcraftAttributes {
    public static final EntityAttribute INK_SWIM_SPEED = register("ink_swim_speed", new ClampedEntityAttribute(SplatcraftAttributes.createTranslationKey("ink_swim_speed"), 0.7f, 0.0d, 1024.0d).setTracked(true));

    private static EntityAttribute register(String id, EntityAttribute entityAttribute) {
        return Registry.register(Registry.ATTRIBUTE, new Identifier(Splatcraft.MOD_ID, id), entityAttribute);
    }
    private static String createTranslationKey(String id) {
        return "attribute." + Splatcraft.MOD_ID + "." + id;
    }
}