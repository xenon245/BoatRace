package com.github.xenon.race

import com.github.monun.tap.effect.playFirework
import com.google.common.collect.ImmutableList
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Directional
import org.bukkit.entity.EntityType
import org.bukkit.entity.Firework
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.util.Vector

object RaceBlocks {
    val SPAWN = SpawnBlock()
    val CHECKPOINT = CheckPointBlock()
    val BOAT = BoatBlock()
    val CLEAR = ClearBlock()
    val RETURN = ReturnBlock()
    val UP = UpBlock()

    val list = ImmutableList.of(
        SPAWN,
        CHECKPOINT,
        BOAT,
        CLEAR,
        RETURN,
        UP
    )

    fun getBlock(block: Block): RaceBlock? {
        val state = block.state
        val data = block.blockData
        val type = data.material

        if(type != Material.AIR) {
            if(type == Material.EMERALD_BLOCK) {
                return SPAWN
            } else if(type == Material.GOLD_BLOCK) {
                return CHECKPOINT
            } else if(type == Material.LAPIS_BLOCK) {
                return BOAT
            } else if(type == Material.BLACK_WOOL || type == Material.WHITE_WOOL) {
                return CLEAR
            } else if(type == Material.POLISHED_DIORITE) {
                return UP
            } else if(type != Material.EMERALD_BLOCK
                && type != Material.GOLD_BLOCK
                && type != Material.LAPIS_BLOCK
                && type != Material.BLACK_WOOL
                && type != Material.WHITE_WOOL
                && type != Material.ICE
                && type != Material.BLUE_ICE
                && type != Material.FROSTED_ICE
                && type != Material.PACKED_ICE) {
                return RETURN
            }
        }
        return null
    }
}

abstract class RaceBlock {
    fun createBlockData(block: Block): RaceBlockData {
        return newBlockData(block).apply {
            this.block = block
            this.racwBlock = this@RaceBlock
        }
    }
    protected abstract fun newBlockData(block: Block): RaceBlockData
}

abstract class RaceBlockData {
    lateinit var block: Block

    lateinit var racwBlock: RaceBlock
        internal set

    open fun onInitialize(challenge: Challenge) {}

    open fun onPass(challenge: Challenge, traceur: Traceur, event: PlayerMoveEvent) {}

    open fun onStep(challenge: Challenge, traceur: Traceur, event: PlayerMoveEvent) {}

    open fun destroy() {}
}

interface Respawnable {
    val respawn: Location
}

class SpawnBlock : RaceBlock() {
    override fun newBlockData(block: Block): RaceBlockData {
        return SpawnData()
    }

    class SpawnData : RaceBlockData(), Respawnable {
        override val respawn: Location
            get() {
                val loc = block.location.add(0.5, 1.0, 0.5)
                block.getRelative(BlockFace.DOWN).let { down ->
                    if(down.type == Material.MAGENTA_GLAZED_TERRACOTTA) {
                        val blockData = down.blockData
                        blockData as Directional

                        loc.yaw = when (blockData.facing) {
                            BlockFace.EAST -> 90.0F
                            BlockFace.SOUTH -> 180.0F
                            BlockFace.WEST -> 270.0F
                            else -> 0.0F
                        }
                    }
                }
                return loc
            }
    }
}
class CheckPointBlock : RaceBlock() {
    override fun newBlockData(block: Block): RaceBlockData {
        return CheckPointData()
    }
    class CheckPointData : RaceBlockData(), Respawnable {
        override val respawn: Location
            get() = block.location.add(0.5, 1.0, 0.5)

        override fun onStep(challenge: Challenge, traceur: Traceur, event: PlayerMoveEvent) {
            if(challenge.setSpawn(traceur, this) != this) {
                val loc = respawn.add(0.0, 4.0, 0.0)
                loc.world.spawn(loc, Firework::class.java).apply {
                    fireworkMeta = fireworkMeta.apply {
                        addEffect(FireworkEffect.builder().apply {
                            with(FireworkEffect.Type.BALL_LARGE)
                            withColor(Color.RED)
                        }.build())
                        power = 1
                    }
                }
            }
        }
    }
}

class BoatBlock : RaceBlock() {
    override fun newBlockData(block: Block): RaceBlockData {
        return BoatBlockData()
    }
    class BoatBlockData : RaceBlockData() {
        override fun onInitialize(challenge: Challenge) {
            block.world.spawnEntity(block.getRelative(BlockFace.UP).location, EntityType.BOAT)
        }
    }
}

class ClearBlock : RaceBlock() {
    override fun newBlockData(block: Block): RaceBlockData {
        return ClearData()
    }
    class ClearData() : RaceBlockData() {
        override fun onStep(challenge: Challenge, traceur: Traceur, event: PlayerMoveEvent) {
            if(traceur.player?.vehicle == null) return
            val level = challenge.level
            level.stopChallenge()

            Bukkit.getOnlinePlayers().forEach {
                it.sendTitle(
                    "${ChatColor.AQUA}${ChatColor.BOLD}COURSE CLEAR",
                    "${ChatColor.RED}${ChatColor.BOLD}${traceur.name}${ChatColor.RESET}님이 ${ChatColor.GOLD}${level.name} ${ChatColor.RESET}레벨을 클리어!",
                    5,
                    90,
                    5
                )
            }

            val loc = block.location.add(0.5, 1.0, 0.5)
            loc.world.spawn(loc, Firework::class.java).apply {
                fireworkMeta = fireworkMeta.apply {
                    addEffect(FireworkEffect.builder().apply {
                        flicker(true)
                        trail(true)
                        with(FireworkEffect.Type.STAR)
                        withColor(Color.AQUA)
                        withColor(Color.RED)
                        withColor(Color.GREEN)
                        withColor(Color.YELLOW)
                        withFade(Color.WHITE)
                    }.build())
                    power = 1
                }
            }
        }
    }
}

class ReturnBlock : RaceBlock() {
    companion object {
        val firework = FireworkEffect.builder().with(FireworkEffect.Type.STAR).withColor(Color.AQUA).build()
    }

    override fun newBlockData(block: Block): RaceBlockData {
        return ReturnBlockData()
    }
    class ReturnBlockData : RaceBlockData() {
        override fun onStep(challenge: Challenge, traceur: Traceur, event: PlayerMoveEvent) {
            traceur.player?.let { player ->
                val loc = player.location.add(0.0, 0.9, 0.0)
                loc.world.playFirework(loc, firework)

                player.health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
                player.foodLevel = 20
                player.saturation = 4.0F
                challenge.respawns[traceur]?.let {
                    player.vehicle?.remove()
                    player.vehicle?.teleport(it.respawn)
                    player.teleport(it.respawn)
                    challenge.spawnBoat()
                }
            }
        }
    }
}

class UpBlock : RaceBlock() {
    override fun newBlockData(block: Block): RaceBlockData {
        return UpBlockData()
    }
    class UpBlockData : RaceBlockData() {
        override fun onStep(challenge: Challenge, traceur: Traceur, event: PlayerMoveEvent) {
            traceur.player?.vehicle?.velocity = Vector(0.0, 0.5, 0.0)
        }
    }
}
