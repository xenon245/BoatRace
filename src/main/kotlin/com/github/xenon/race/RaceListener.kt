package com.github.xenon.race

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent
import net.kyori.adventure.text.Component.text
import org.bukkit.Bukkit
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.player.*
import org.bukkit.event.vehicle.VehicleDestroyEvent
import org.bukkit.util.NumberConversions.floor

class RaceListener : Listener {
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        BoatRace.registerPlayer(event.player)
    }
    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        event.player.traceur.apply {
            player = null
        }
    }
    @EventHandler
    fun onPickUpItemEvent(event: PlayerPickupItemEvent) {
        if(event.player.traceur.challenge != null) {
            event.isCancelled = true
        }
    }
    @EventHandler
    fun onEntityKnockBack(event: EntityKnockbackByEntityEvent) {
        if(event.entityType == EntityType.PLAYER) {
            if((event.entity as Player).traceur.challenge != null) {
                event.isCancelled = true
            }
        }
    }
    @EventHandler
    fun onEntityDamageEvent(event: EntityDamageByEntityEvent) {
        if(event.entityType == EntityType.PLAYER) {
            if((event.entity as Player).traceur.challenge != null) {
                event.isCancelled = true
            }
        }
        if(event.damager.type == EntityType.PLAYER) {
            if((event.damager as Player).traceur.challenge != null) {
                event.isCancelled = true
            }
        }
    }
    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if(event.player.traceur.challenge != null) {
            if(event.action == Action.LEFT_CLICK_AIR || event.action == Action.LEFT_CLICK_BLOCK) {
                event.isCancelled = true
            }
        }
    }
    @EventHandler
    fun onVehicleDestroy(event: VehicleDestroyEvent) {
        if((event.attacker as Player).traceur.challenge != null) {
            event.isCancelled = true
        }
    }
    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        event.player.let { player ->
            val traceur = player.traceur
            traceur.challenge?.let { challenge ->
                val passBlock = event.to.block
                challenge.dataByBlock[passBlock]?.run {
                    onPass(challenge, traceur, event)
                }

                val box = player.boundingBox
                val y = floor(box.minY)
                val stepY = floor(box.minY - 0.000001)

                if(stepY != y) {
                    val world = player.world
                    val minX: Int = floor(box.minX)
                    val minZ: Int = floor(box.minZ)
                    val maxX: Int = floor(box.maxX)
                    val maxZ: Int = floor(box.maxZ)

                    for(x in minX..maxX) {
                        for(z in minZ..maxZ) {
                            val stepBlock = world.getBlockAt(x, stepY, z)

                            challenge.dataByBlock[stepBlock]?.run {
                                onStep(challenge, traceur, event)
                            }
                        }
                    }
                }
            }
        }
    }
}