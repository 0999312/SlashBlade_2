/*
 * Minecraft Forge
 * Copyright (c) 2016-2019.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package mods.flammpfeil.slashblade.capability.slashblade;

import mods.flammpfeil.slashblade.client.renderer.CarryType;
import mods.flammpfeil.slashblade.network.ActiveStateSyncMessage;
import mods.flammpfeil.slashblade.network.NetworkManager;
import mods.flammpfeil.slashblade.specialattack.SlashArts;
import mods.flammpfeil.slashblade.util.NBTHelper;
import mods.flammpfeil.slashblade.util.TimeValueHelper;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.stats.Stats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * An slashblade state is the unit of interaction with Energy inventories.
 * <p>
 * A reference implementation can be found at {@link SlashBladeState}.
 *
 * Derived from the Redstone Flux power system designed by King Lemming and originally utilized in Thermal Expansion and related mods.
 * Created with consent and permission of King Lemming and Team CoFH. Released with permission under LGPL 2.1 when bundled with Forge.
 *
 */
public interface ISlashBladeState {

    long getLastActionTime();
    void setLastActionTime(long lastActionTime);
    default long getElapsedTime(LivingEntity user){
        return (Math.max(0, user.world.getGameTime() - this.getLastActionTime()));
    }

    boolean onClick();
	void setOnClick(boolean onClick);

    float getFallDecreaseRate();
	void setFallDecreaseRate(float fallDecreaseRate);

    boolean isCharged();
	void setCharged(boolean charged);

    float getAttackAmplifier();
	void setAttackAmplifier(float attackAmplifier);

	@Nonnull
    ComboState getComboSeq();
	void setComboSeq(ComboState comboSeq);

    String getLastPosHash();
	void setLastPosHash(String lastPosHash);

    boolean hasShield();
	void setHasShield(boolean hasShield);

    boolean isBroken();
	void setBroken(boolean broken);

    boolean isNoScabbard();
	void setNoScabbard(boolean noScabbard);

    boolean isSealed();
	void setSealed(boolean sealed);

    float getBaseAttackModifier();
	void setBaseAttackModifier(float baseAttackModifier);

    int getKillCount();
	void setKillCount(int killCount);

    int getRefine();
	void setRefine(int refine);

    UUID getOwner();
    void setOwner(UUID owner);

    UUID getUniqueId();
    void setUniqueId(UUID id);

    @Nonnull
    RangeAttack getRangeAttackType();
	void setRangeAttackType(RangeAttack rangeAttackType);

    @Nonnull
    SlashArts getSlashArts();
	void setSlashArts(SlashArts slashArts);

    boolean isDestructable();
	void setDestructable(boolean destructable);

    boolean isDefaultBewitched();
	void setDefaultBewitched(boolean defaultBewitched);

	@Nonnull
    Rarity getRarity();
	void setRarity(Rarity rarity);

    @Nonnull
	String getTranslationKey();
	void setTranslationKey(String translationKey);

    @Nonnull
    CarryType getCarryType();
	void setCarryType(CarryType carryType);

    @Nonnull
    Color getEffectColor();
	void setEffectColor(Color effectColor);

    boolean isEffectColorInverse();
	void setEffectColorInverse(boolean effectColorInverse);

	default void setColorCode(int colorCode){
        setEffectColor(new Color(colorCode));
    }

    default int getColorCode(){
        return getEffectColor().getRGB();
    }

    @Nonnull
    Vec3d getAdjust();
	void setAdjust(Vec3d adjust);

    @Nonnull
    Optional<ResourceLocation> getTexture();
	void setTexture(ResourceLocation texture);

    @Nonnull
    Optional<ResourceLocation> getModel();
	void setModel(ResourceLocation model);

    int getTargetEntityId();
	void setTargetEntityId(int id);

    @Nullable
    default Entity getTargetEntity(World world) {
        int id = getTargetEntityId();
        if (id < 0)
            return null;
        else
            return world.getEntityByID(id);
    }

	default void setTargetEntityId(Entity target) {
        if (target != null)
            this.setTargetEntityId(target.getEntityId());
        else
            this.setTargetEntityId(-1);
    }

    default int getFullChargeTicks(LivingEntity user){
        return 20;
    }

    default boolean isCharged(LivingEntity user){
        int elapsed = user.getItemInUseMaxCount();
        return getFullChargeTicks(user) < elapsed;
    }

    default ComboState progressCombo(LivingEntity user){
        ComboState current = resolvCurrentComboState(user);

        ComboState next = current.getNext(user);
        ComboState rootNext = getComboRoot().getNext(user);

        ComboState resolved = next.getPriority() <= rootNext.getPriority()
                ? next : rootNext;

        this.setComboSeq(resolved);

        return resolved;
    }

    default ComboState progressCombo(LivingEntity user, int elapsed){
        float fullChargeTicks = getFullChargeTicks(user);

        float justReceptionSpan = 3; //todo:+ justTime Extension
        float quickReceptionSpan = 5; //todo:+ quickTime Extension

        float justChargePeriod = fullChargeTicks + justReceptionSpan;


        boolean isCharged = false;
        boolean isJust = false;

        if(this.isCharged(user)){
            isCharged = true;

            if(elapsed < justChargePeriod)
                isJust = true;

        }else{ //quick charge

            int ticks = elapsed;
            ComboState old = this.getComboSeq();
            ComboState current = resolvCurrentComboState(user);
            if(old != current){
                ticks -= TimeValueHelper.getTicksFromMSec(old.getTimeoutMS());
            }

            float quickChargeTicks = TimeValueHelper.getTicksFromFrames(current.getEndFrame() - current.getStartFrame()) * (1.0f / current.getSpeed());;
            float quickChargePeriod = quickChargeTicks + quickReceptionSpan;
            float quickJustChargePeriod = quickChargeTicks + quickReceptionSpan;

            if(quickChargeTicks < ticks && ticks < quickChargePeriod){
                isCharged = true;

                if(ticks < quickJustChargePeriod){
                    isJust = true;
                }
            }
        }

        ComboState resolved;
        if(isCharged){
            if(isJust){
                //todo: just SA
                resolved = this.getSlashArts().getComboState();
                this.setComboSeq(resolved);
            }else{
                //todo: SlashArts
                resolved = this.getSlashArts().getComboState();
                this.setComboSeq(resolved);
            }

        }else{
            resolved = ComboState.NONE;
        }

        return resolved;
    }

    default ComboState resolvCurrentComboState(LivingEntity user){
        ComboState current = getComboSeq();

        float time = TimeValueHelper.getMSecFromTicks(getElapsedTime(user));

        while(current != ComboState.NONE && current.getTimeoutMS() < time){
            time -= current.getTimeoutMS();

            current = current.getNextOfTimeout();
        }

        return current;
    }

    default boolean hasEnergy(){
        return true;
    }

    String getComboRootName();
    void setComboRootName(String comboRootName);

    default ComboState getComboRoot(){
        return Optional.ofNullable(ComboState.NONE.valueOf(this.getComboRootName())).orElseGet(()->ComboState.STANDBY);
    }

    CompoundNBT getShareTag();
    void setShareTag(CompoundNBT shareTag);

    float getDamage();
    void setDamage(float damage);

    default <T extends LivingEntity> void damageBlade(ItemStack stack, int amount, T entityIn, Consumer<T> onBroken){
        if(amount <= 0) return;

        boolean current = this.isBroken();

        stack.damageItem(1, entityIn, (s)->{});

        if(1.0f <= this.getDamage())
            this.setBroken(true);

        if(current != this.isBroken()){
            onBroken.accept(entityIn);

            if (entityIn instanceof ServerPlayerEntity) {
                CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayerEntity)entityIn, stack);
            }

            if(entityIn instanceof PlayerEntity)
                ((PlayerEntity)entityIn).addStat(Stats.ITEM_BROKEN.get(stack.getItem()));
        }


        if(this.isBroken() && this.isDestructable())
            stack.shrink(1);
    }

    default float getDurabilityForDisplay(){
        return Math.max(0,Math.min(getDamage(), 1.0f));
    }

    boolean hasChangedActiveState();
    void setHasChangedActiveState(boolean isChanged);

    default void sendChanges(Entity entityIn){
        if(!entityIn.world.isRemote && this.hasChangedActiveState()){
            ActiveStateSyncMessage msg = new ActiveStateSyncMessage();
            msg.activeTag = this.getActiveState();
            msg.id = entityIn.getEntityId();
            NetworkManager.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->entityIn), msg);

            this.setHasChangedActiveState(false);
        }
    }

    default CompoundNBT getActiveState(){
        CompoundNBT tag = new CompoundNBT();

        NBTHelper.getNBTCoupler(tag)
                .put("BladeUniqueId", this.getUniqueId())

                .put("lastActionTime" , this.getLastActionTime())
                .put("TargetEntity", this.getTargetEntityId())
                .put("_onClick", this.onClick())
                .put("fallDecreaseRate", this.getFallDecreaseRate())
                .put("isCharged", this.isCharged())
                .put("AttackAmplifier", this.getAttackAmplifier())
                .put("currentCombo", this.getComboSeq().getName())
                .put("lastPosHash", this.getLastPosHash())
                .put("HasShield", this.hasShield())

                .put("KillCount", this.getKillCount())

                .put("ShareTag",this.getShareTag())
                .put("Damage", this.getDamage())

                .put("isBroken", this.isBroken());

        return tag;
    }

    default void setActiveState(CompoundNBT tag){
        NBTHelper.getNBTCoupler(tag)
                //.get("BladeUniqueId", this::setUniqueId)

                .get("lastActionTime", this::setLastActionTime)
                .get("TargetEntity", ((Integer id) -> this.setTargetEntityId(id)))
                .get("_onClick", this::setOnClick)
                .get("fallDecreaseRate", this::setFallDecreaseRate)
                .get("isCharged", this::setCharged)
                .get("AttackAmplifier", this::setAttackAmplifier)
                .get("currentCombo", ((String s) -> this.setComboSeq(ComboState.NONE.valueOf(s))))
                .get("lastPosHash", this::setLastPosHash)
                .get("HasShield", this::setHasShield)

                .get("KillCount", this::setKillCount)

                .get("ShareTag",this::setShareTag)
                .get("Damage", this::setDamage)

                .get("isBroken", this::setBroken);

        this.setHasChangedActiveState(false);
    }
}