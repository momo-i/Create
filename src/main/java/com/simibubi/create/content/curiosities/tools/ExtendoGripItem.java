package com.simibubi.create.content.curiosities.tools;

import java.util.UUID;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.advancement.AllTriggers;
import com.simibubi.create.foundation.networking.AllPackets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.item.Item;
import net.minecraft.item.Rarity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent.ClickInputEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class ExtendoGripItem extends Item {

	static Multimap<String, AttributeModifier> rangeModifier;
	static Multimap<String, AttributeModifier> doubleRangeModifier;

	static {
		// Holding an ExtendoGrip
		rangeModifier = HashMultimap.create();
		rangeModifier.put(PlayerEntity.REACH_DISTANCE.getName(),
			new AttributeModifier(UUID.fromString("7f7dbdb2-0d0d-458a-aa40-ac7633691f66"), "Range modifier", 3,
				AttributeModifier.Operation.ADDITION));

		// Holding two ExtendoGrips o.O
		doubleRangeModifier = HashMultimap.create();
		doubleRangeModifier.put(PlayerEntity.REACH_DISTANCE.getName(),
			new AttributeModifier(UUID.fromString("8f7dbdb2-0d0d-458a-aa40-ac7633691f66"), "Range modifier", 5,
				AttributeModifier.Operation.ADDITION));
	}

	public ExtendoGripItem(Properties properties) {
		super(properties.maxStackSize(1)
			.rarity(Rarity.UNCOMMON));
	}

	@SubscribeEvent
	public static void holdingExtendoGripIncreasesRange(LivingUpdateEvent event) {
		if (!(event.getEntity() instanceof PlayerEntity))
			return;

		PlayerEntity player = (PlayerEntity) event.getEntityLiving();
		String marker = "createExtendo";
		String dualMarker = "createDualExtendo";

		CompoundNBT persistentData = player.getPersistentData();
		boolean inOff = AllItems.EXTENDO_GRIP.isIn(player.getHeldItemOffhand());
		boolean inMain = AllItems.EXTENDO_GRIP.isIn(player.getHeldItemMainhand());
		boolean holdingDualExtendo = inOff && inMain;
		boolean holdingExtendo = inOff ^ inMain;
		holdingExtendo &= !holdingDualExtendo;
		boolean wasHoldingExtendo = persistentData.contains(marker);
		boolean wasHoldingDualExtendo = persistentData.contains(dualMarker);

		if (holdingExtendo != wasHoldingExtendo) {
			if (!holdingExtendo) {
				player.getAttributes()
					.removeAttributeModifiers(rangeModifier);
				persistentData.remove(marker);
			} else {
				if (player instanceof ServerPlayerEntity)
					AllTriggers.EXTENDO.trigger((ServerPlayerEntity) player);
				player.getAttributes()
					.applyAttributeModifiers(rangeModifier);
				persistentData.putBoolean(marker, true);
			}
		}

		if (holdingDualExtendo != wasHoldingDualExtendo) {
			if (!holdingDualExtendo) {
				player.getAttributes()
					.removeAttributeModifiers(doubleRangeModifier);
				persistentData.remove(dualMarker);
			} else {
				if (player instanceof ServerPlayerEntity)
					AllTriggers.GIGA_EXTENDO.trigger((ServerPlayerEntity) player);
				player.getAttributes()
					.applyAttributeModifiers(doubleRangeModifier);
				persistentData.putBoolean(dualMarker, true);
			}
		}

	}

	@SubscribeEvent
	@OnlyIn(Dist.CLIENT)
	public static void dontMissEntitiesWhenYouHaveHighReachDistance(ClickInputEvent event) {
		Minecraft mc = Minecraft.getInstance();
		ClientPlayerEntity player = mc.player;
		if (mc.world == null || player == null)
			return;
		if (!isHoldingExtendoGrip(player))
			return;
		if (mc.objectMouseOver instanceof BlockRayTraceResult && mc.objectMouseOver.getType() != Type.MISS)
			return;

		// Modified version of GameRenderer#getMouseOver
		double d0 = player.getAttribute(PlayerEntity.REACH_DISTANCE)
			.getValue();
		if (!player.isCreative())
			d0 -= 0.5f;
		Vec3d vec3d = player.getEyePosition(mc.getRenderPartialTicks());
		Vec3d vec3d1 = player.getLook(1.0F);
		Vec3d vec3d2 = vec3d.add(vec3d1.x * d0, vec3d1.y * d0, vec3d1.z * d0);
		AxisAlignedBB axisalignedbb = player.getBoundingBox()
			.expand(vec3d1.scale(d0))
			.grow(1.0D, 1.0D, 1.0D);
		EntityRayTraceResult entityraytraceresult =
			ProjectileHelper.rayTraceEntities(player, vec3d, vec3d2, axisalignedbb, (e) -> {
				return !e.isSpectator() && e.canBeCollidedWith();
			}, d0 * d0);
		if (entityraytraceresult != null) {
			Entity entity1 = entityraytraceresult.getEntity();
			Vec3d vec3d3 = entityraytraceresult.getHitVec();
			double d2 = vec3d.squareDistanceTo(vec3d3);
			if (d2 < d0 * d0 || mc.objectMouseOver == null || mc.objectMouseOver.getType() == Type.MISS) {
				mc.objectMouseOver = entityraytraceresult;
				if (entity1 instanceof LivingEntity || entity1 instanceof ItemFrameEntity)
					mc.pointedEntity = entity1;
			}
		}

	}

	@SubscribeEvent
	public static void attacksByExtendoGripHaveMoreKnockback(LivingKnockBackEvent event) {
		Entity entity = event.getAttacker();
		if (!(entity instanceof PlayerEntity))
			return;
		PlayerEntity player = (PlayerEntity) entity;
		if (!isHoldingExtendoGrip(player))
			return;
		event.setStrength(event.getStrength() + 2);
	}

	private static boolean isUncaughtClientInteraction(Entity entity, Entity target) {
		// Server ignores entity interaction further than 6m
		if (entity.getDistanceSq(target) < 36)
			return false;
		if (!entity.world.isRemote)
			return false;
		if (!(entity instanceof PlayerEntity))
			return false;
		return true;
	}

	@SubscribeEvent
	@OnlyIn(Dist.CLIENT)
	public static void notifyServerOfLongRangeAttacks(AttackEntityEvent event) {
		Entity entity = event.getEntity();
		Entity target = event.getTarget();
		if (!isUncaughtClientInteraction(entity, target))
			return;
		PlayerEntity player = (PlayerEntity) entity;
		if (isHoldingExtendoGrip(player))
			AllPackets.channel.sendToServer(new ExtendoGripInteractionPacket(target));
	}

	@SubscribeEvent
	@OnlyIn(Dist.CLIENT)
	public static void notifyServerOfLongRangeInteractions(PlayerInteractEvent.EntityInteract event) {
		Entity entity = event.getEntity();
		Entity target = event.getTarget();
		if (!isUncaughtClientInteraction(entity, target))
			return;
		PlayerEntity player = (PlayerEntity) entity;
		if (isHoldingExtendoGrip(player))
			AllPackets.channel.sendToServer(new ExtendoGripInteractionPacket(target, event.getHand()));
	}

	@SubscribeEvent
	@OnlyIn(Dist.CLIENT)
	public static void notifyServerOfLongRangeSpecificInteractions(PlayerInteractEvent.EntityInteractSpecific event) {
		Entity entity = event.getEntity();
		Entity target = event.getTarget();
		if (!isUncaughtClientInteraction(entity, target))
			return;
		PlayerEntity player = (PlayerEntity) entity;
		if (isHoldingExtendoGrip(player))
			AllPackets.channel
				.sendToServer(new ExtendoGripInteractionPacket(target, event.getHand(), event.getLocalPos()));
	}

	public static boolean isHoldingExtendoGrip(PlayerEntity player) {
		boolean inOff = AllItems.EXTENDO_GRIP.isIn(player.getHeldItemOffhand());
		boolean inMain = AllItems.EXTENDO_GRIP.isIn(player.getHeldItemMainhand());
		boolean holdingGrip = inOff || inMain;
		return holdingGrip;
	}

}
