package com.ericdebouwer.enderdragonNMS;

import java.lang.reflect.Field;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_16_R2.CraftWorld;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.util.Vector;

import com.ericdebouwer.petdragon.Message;
import com.ericdebouwer.petdragon.PetDragon;

import net.minecraft.server.v1_16_R2.BlockPosition;
import net.minecraft.server.v1_16_R2.DamageSource;
import net.minecraft.server.v1_16_R2.DragonControllerPhase;
import net.minecraft.server.v1_16_R2.EntityComplexPart;
import net.minecraft.server.v1_16_R2.EntityHuman;
import net.minecraft.server.v1_16_R2.EntityLiving;
import net.minecraft.server.v1_16_R2.EntityPlayer;
import net.minecraft.server.v1_16_R2.ItemStack;
import net.minecraft.server.v1_16_R2.MinecraftServer;
import net.minecraft.server.v1_16_R2.PacketPlayOutWorldEvent;
import net.minecraft.server.v1_16_R2.Particles;
import net.minecraft.server.v1_16_R2.WorldServer;
import net.minecraft.server.v1_16_R2.EntityEnderDragon;
import net.minecraft.server.v1_16_R2.EntityTypes;
import net.minecraft.server.v1_16_R2.World;

public class PetEnderDragon_v1_16_R2 extends EntityEnderDragon  implements PetEnderDragon {
	
	Location loc;
	private boolean canShoot = true;
	private PetDragon plugin;

	public PetEnderDragon_v1_16_R2(EntityTypes<? extends EntityEnderDragon> entitytypes, World world) {
		super(EntityTypes.ENDER_DRAGON, world);
	}
	
	public PetEnderDragon_v1_16_R2(Location loc, PetDragon plugin){
		super(null, ((CraftWorld)loc.getWorld()).getHandle());
		this.plugin = plugin;
		this.loc = loc;
		EnderDragon bukkitDragon = (EnderDragon) this.getBukkitEntity();
		bukkitDragon.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(MAX_HEALTH);
		bukkitDragon.setHealth(MAX_HEALTH);
		bukkitDragon.getScoreboardTags().add(PetEnderDragon.DRAGON_ID);
		
		this.setPosition(loc.getX(), loc.getY(), loc.getZ());
	}

	@Override
	public void spawn() {
		((CraftWorld)loc.getWorld()).getHandle().addEntity(this, SpawnReason.CUSTOM);
	}
	
	@Override
	public EnderDragon getEntity() {
		return (EnderDragon) this.getBukkitEntity();
	}
	
	@Override
	protected boolean cS() { //affected by fluids
		return false;
	};
	
	@Override
	public boolean bs() { //ridable in water
		return true;
	};
		
	
	@Override
	public boolean a(EntityComplexPart entitycomplexpart, DamageSource damagesource, float f) {
		if (getDragonControllerManager().a().getControllerPhase() == DragonControllerPhase.DYING) {
			return false;
		} 
		if (plugin.getConfigManager().leftClickRide){
			if (damagesource.getEntity() instanceof EntityHuman) {
				EntityHuman human = (EntityHuman) damagesource.getEntity();
				ItemStack item = human.inventory.getItemInHand();
					// lege hand
				if (item.isEmpty()){
					if (human.getBukkitEntity().hasPermission("petdragon.ride")){
						human.startRiding(this);
					}
					else plugin.getConfigManager().sendMessage((Player) human.getBukkitEntity(), Message.NO_RIDE_PERMISSION, null);
					return false;
				}
			}
		}
		
		f = getDragonControllerManager().a().a(damagesource, f);
		f = f / (200.0F / MAX_HEALTH);
		
		//head 4x as much damage
		if (entitycomplexpart != this.bo) {
			f = f / 4.0F + Math.min(f, 1.0F);
		}
		
		if (f < 0.01F) {
			return false;
		} else {
			if (damagesource.getEntity() instanceof EntityHuman || damagesource.isExplosion()) {
				this.dealDamage(damagesource, f);
				if (this.dk() && !getDragonControllerManager().a().a()) {
					this.setHealth(1.0F);
				}
			}
			return true;
		}
	}
	
	@Override
	public boolean canPortal(){
		return true;
	}
	
    
	@Override
	// elke bewegings update
	public void movementTick(){
		
		super.movementTick();
		
		if (this.passengers.isEmpty() || !(this.passengers.get(0) instanceof EntityHuman)){
			return;
		}
		EntityHuman rider = (EntityHuman) this.passengers.get(0);
		Vector forwardDir = rider.getBukkitEntity().getLocation().getDirection();
		
		if (rider.getBukkitEntity().hasPermission("petdragon.shoot")){
	    		try {
				Field jumping = EntityLiving.class.getDeclaredField("jumping");
				jumping.setAccessible(true);
				boolean jumped = jumping.getBoolean(rider);
				if (jumped){
					if (canShoot){
						Location loc = this.getBukkitEntity().getLocation();
						loc.add(forwardDir.clone().multiply(10).setY(-1));
						
						DragonFireball fireball = loc.getWorld().spawn(loc, DragonFireball.class);
						fireball.setDirection(forwardDir);
						fireball.setShooter(rider.getBukkitEntity());
						canShoot = false;
					}
				}
				else {
					canShoot = true;
				}
			} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e ){
			}
    		}
		
		this.setYawPitch(180 + rider.yaw, rider.pitch);
		this.setHeadRotation(rider.pitch);
		
		double speeder = plugin.getConfigManager().speedMultiplier;
		double fwSpeed = rider.aT * speeder;
		double sideSpeed = -1 * rider.aR * speeder;
		
    		Vector sideways = forwardDir.clone().crossProduct(new Vector(0,1,0));
    
    		Vector total = forwardDir.multiply(fwSpeed).add(sideways.multiply(sideSpeed));
    
    		Location newLoc = this.getBukkitEntity().getLocation().add(total);
    		this.setPosition(newLoc.getX(), newLoc.getY(), newLoc.getZ());
        
	}	
	
	
	@Override
	public void cT(){
		++this.deathAnimationTicks;
		
		if (this.deathAnimationTicks == 1){
			
			//revoke advancement
			if (this.getKillingEntity() instanceof EntityHuman){
				Player p = (Player) this.getKillingEntity().getBukkitEntity();
				Advancement freeEnd = Bukkit.getAdvancement(NamespacedKey.minecraft("end/kill_dragon"));
				p.getAdvancementProgress(freeEnd).revokeCriteria("killed_dragon");
			}
			// make players nearby aware of his death 
			
			if (this.deathAnimationTicks == 1 && !this.isSilent()) {
				int viewDistance = ((WorldServer) this.world).getServer()
						.getViewDistance() * 16;
				@SuppressWarnings("deprecation")
				Iterator<EntityPlayer> var5 = MinecraftServer.getServer().getPlayerList().players
						.iterator();

				label59 : while (true) {
					EntityPlayer player;
					double deltaX;
					double deltaZ;
					double distanceSquared;
					do {
						if (!var5.hasNext()) {
							break label59;
						}

						player = (EntityPlayer) var5.next();
						deltaX = this.locX() - player.locX();
						deltaZ = this.locZ() - player.locZ();
						distanceSquared = deltaX * deltaX + deltaZ * deltaZ;
					} while (this.world.spigotConfig.dragonDeathSoundRadius > 0
							&& distanceSquared > (double) (this.world.spigotConfig.dragonDeathSoundRadius * this.world.spigotConfig.dragonDeathSoundRadius));

					if (distanceSquared > (double) (viewDistance * viewDistance)) {
						double deltaLength = Math.sqrt(distanceSquared);
						double relativeX = player.locX() + deltaX / deltaLength
								* (double) viewDistance;
						double relativeZ = player.locZ() + deltaZ / deltaLength
								* (double) viewDistance;
						player.playerConnection
								.sendPacket(new PacketPlayOutWorldEvent(1028,
										new BlockPosition((int) relativeX,
												(int) this.locY(),
												(int) relativeZ), 0, true));
					} else {
						player.playerConnection
								.sendPacket(new PacketPlayOutWorldEvent(1028,
										new BlockPosition((int) this.locX(),
												(int) this.locY(), (int) this
														.locZ()), 0, true));
					}
				}
			}
		}
		
		if (this.deathAnimationTicks <= 100) {
			// particle stuff
			float f = (this.random.nextFloat() - 0.5F) * 8.0F;
			float f1 = (this.random.nextFloat() - 0.5F) * 4.0F;
			float f2 = (this.random.nextFloat() - 0.5F) * 8.0F;
			this.world.addParticle(Particles.EXPLOSION_EMITTER, this.locX()
					+ (double) f, this.locY() + 2.0D + (double) f1, this.locZ()
					+ (double) f2, 0.0D, 0.0D, 0.0D);
		}
		else {
			this.die();
		}
		
	}

}
