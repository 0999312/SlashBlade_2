package mods.flammpfeil.slashblade.capability.slashblade;

import mods.flammpfeil.slashblade.client.renderer.CarryType;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.registry.SlashArtsRegistry;
import mods.flammpfeil.slashblade.util.EnumSetConverter;
import mods.flammpfeil.slashblade.util.NBTHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Furia on 2017/01/10.
 */
public class SimpleBladeStateCapabilityProvider implements ICapabilityProvider, INBTSerializable<Tag> {

    public static final Capability<ISlashBladeState> CAP = CapabilityManager.get(new CapabilityToken<>(){});

    protected LazyOptional<ISlashBladeState> state;

    public SimpleBladeStateCapabilityProvider(ResourceLocation model, ResourceLocation texture, float attack, int damage){
        state = LazyOptional.of( () -> new SimpleSlashBladeState(model, texture, attack, damage));
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return CAP.orEmpty(cap, state);
    }

    @Override
    public Tag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        state.ifPresent(instance -> {
            //action state
            tag.putLong("lastActionTime" , instance.getLastActionTime());
            tag.putInt("TargetEntity", instance.getTargetEntityId());
            tag.putBoolean("_onClick", instance.onClick());
            tag.putFloat("fallDecreaseRate", instance.getFallDecreaseRate());
            tag.putBoolean("isCharged", instance.isCharged());
            tag.putFloat("AttackAmplifier", instance.getAttackAmplifier());
            tag.putString("currentCombo", instance.getComboSeq().toString());
            tag.putInt("Damage", instance.getDamage());
//            tag.putInt("maxDamage", instance.getMaxDamage());
            tag.putBoolean("isBroken", instance.isBroken());

            //passive state
            tag.putBoolean("isSealed", instance.isSealed());

//            tag.putFloat("baseAttackModifier", instance.getBaseAttackModifier());

            tag.putInt("killCount", instance.getKillCount());
            tag.putInt("RepairCounter", instance.getRefine());

            UUID id = instance.getOwner();
            if(id != null)
                tag.putUUID("Owner", id);


            UUID bladeId = instance.getUniqueId();
            tag.putUUID("BladeUniqueId", bladeId);


            //performance setting

            tag.putString("SpecialAttackType", Optional.ofNullable(instance.getSlashArtsKey()).orElse(SlashArtsRegistry.JUDGEMENT_CUT.getId()).toString());
//            tag.putBoolean("isDestructable", instance.isDestructable());
//            tag.putBoolean("isDefaultBewitched", instance.isDefaultBewitched());
//            tag.putString("translationKey", instance.getTranslationKey());

            //render info
            tag.putByte("StandbyRenderType", (byte)instance.getCarryType().ordinal());
            tag.putInt("SummonedSwordColor", instance.getColorCode());
            tag.putBoolean("SummonedSwordColorInverse", instance.isEffectColorInverse());
            tag.put("adjustXYZ" , NBTHelper.newDoubleNBTList(instance.getAdjust()));

//            instance.getTexture()
//                    .ifPresent(loc ->  tag.putString("TextureName", loc.toString()));
//            instance.getModel()
//                    .ifPresent(loc ->  tag.putString("ModelName", loc.toString()));

            tag.putString("ComboRoot", Optional.ofNullable(instance.getComboRoot()).orElse(ComboStateRegistry.STANDBY.getId()).toString());
            
        });

        return tag;
    }


    @Deprecated
    private final String tagState = "State";

    @Override
    public void deserializeNBT(Tag inTag) {

        Tag baseTag;
        if(inTag instanceof CompoundTag && ((CompoundTag) inTag).contains(tagState)){
            //old
            baseTag = ((CompoundTag) inTag).get(tagState);
        }else{
            baseTag = inTag;
        }

        state.ifPresent(instance->{
            CompoundTag tag = (CompoundTag)baseTag;

            //action state
            instance.setLastActionTime(tag.getLong("lastActionTime"));
            instance.setTargetEntityId(tag.getInt("TargetEntity"));
            instance.setOnClick(tag.getBoolean("_onClick"));
            instance.setFallDecreaseRate(tag.getFloat("fallDecreaseRate"));
            instance.setCharged(tag.getBoolean("isCharged"));
            instance.setAttackAmplifier(tag.getFloat("AttackAmplifier"));
            instance.setComboSeq(ResourceLocation.tryParse(tag.getString("currentCombo")));
            instance.setDamage(tag.getInt("Damage"));
//            instance.setMaxDamage(tag.getInt("maxDamage"));

            instance.setBroken(tag.getBoolean("isBroken"));

            instance.setHasChangedActiveState(true);


            //passive state
            instance.setSealed(tag.getBoolean("isSealed"));

//            instance.setBaseAttackModifier(tag.getFloat("baseAttackModifier"));

            instance.setKillCount(tag.getInt("killCount"));
            instance.setRefine(tag.getInt("RepairCounter"));

            instance.setOwner(tag.hasUUID("Owner") ? tag.getUUID("Owner") : null);

            instance.setUniqueId(tag.hasUUID("BladeUniqueId") ? tag.getUUID("BladeUniqueId") : UUID.randomUUID());

            //performance setting


//            instance.setSlashArtsKey(ResourceLocation.tryParse(tag.getString("SpecialAttackType")));
//            instance.setDestructable(tag.getBoolean("isDestructable"));
//            instance.setDefaultBewitched(tag.getBoolean("isDefaultBewitched"));

//            instance.setTranslationKey(tag.getString("translationKey"));

            //render info
            instance.setCarryType(EnumSetConverter.fromOrdinal(CarryType.values(), tag.getByte("StandbyRenderType"), CarryType.DEFAULT));
            instance.setColorCode(tag.getInt("SummonedSwordColor"));
            instance.setEffectColorInverse(tag.getBoolean("SummonedSwordColorInverse"));
            instance.setAdjust(NBTHelper.getVector3d(tag, "adjustXYZ"));

//            if(tag.contains("TextureName"))
//                instance.setTexture(new ResourceLocation(tag.getString("TextureName")));
//            else
//                instance.setTexture(null);
//
//            if(tag.contains("ModelName"))
//                instance.setModel(new ResourceLocation(tag.getString("ModelName")));
//            else
//                instance.setModel(null);

            instance.setComboRoot(ResourceLocation.tryParse(tag.getString("ComboRoot")));
        });
    }
}