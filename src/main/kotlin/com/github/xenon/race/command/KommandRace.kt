package com.github.xenon.race.command

import com.github.monun.kommand.KommandBuilder
import com.github.monun.kommand.KommandContext
import com.github.monun.kommand.argument.KommandArgument
import com.github.monun.kommand.argument.playerTarget
import com.github.monun.kommand.argument.string
import com.github.monun.kommand.argument.world
import com.github.monun.kommand.sendFeedback
import com.github.xenon.race.BoatRace
import com.github.xenon.race.Level
import com.github.xenon.race.traceur
import com.github.xenon.race.util.selection
import com.sk89q.worldedit.regions.CuboidRegion
import net.kyori.adventure.text.Component.text
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object KommandRace {
    fun register(builder: KommandBuilder) {
        builder.apply {
            then("create") {
                then("name" to string()) {
                    require { this is Player }
                    executes {
                        val sender = it.sender as Player
                        sender.selection?.let { region ->
                            if(region !is CuboidRegion) {
                                sender.sendFeedback("레이스 코스로 지원하지 않는 구역입니다.")
                            } else {
                                BoatRace.runCatching {
                                    createLevel(it.parseArgument("name"), region)
                                }.onSuccess {
                                    sender.sendFeedback("${it.name} 레이스 레벨을 생성했습니다.")
                                }.onFailure {
                                    sender.sendFeedback("$name 레이스 레벨 생성을 실패했습니다. ${it.message}")
                                }
                            }
                        } ?: sender.sendFeedback("레이스 레벨을 생성할 구역을 WorldEdit의 Wand로 지정해주세요")
                    }
                }
            }
            then("remove") {
                then("level" to LevelArgument) {
                    executes {
                        val level = it.parseArgument<Level>("level")
                        val sender = it.sender
                        level.remove()
                        sender.sendFeedback("${level.name} 레이스 레벨을 제거했습니다.")
                    }
                }
            }
            then("start") {
                then("level" to LevelArgument) {
                    then("player" to playerTarget()) {
                        executes {
                            val player = it.parseArgument<ArrayList<Player>>("player")
                            val level = it.parseArgument<Level>("level")
                            val challenge = level.startChallenge()
                            player.forEach { p ->
                                p.run {
                                    inventory.clear()
                                    health = requireNotNull(getAttribute(Attribute.GENERIC_MAX_HEALTH)).value
                                    foodLevel = 20
                                    saturation = 4.0F
                                    challenge.addTraceur(traceur)
                                    gameMode = GameMode.ADVENTURE
                                    challenge.respawns[traceur]?.let { teleport(it.respawn) }
                                }
                            }
                        }
                    }
                    require {
                        this is Player
                    }
                    executes {
                        val level = it.parseArgument<Level>("level")
                        val player = it.sender as Player
                        val challenge = level.startChallenge()
                        player.run {
                            health = requireNotNull(getAttribute(Attribute.GENERIC_MAX_HEALTH)).value
                            foodLevel = 20
                            saturation = 4.0F
                            challenge.addTraceur(traceur)
                            gameMode = GameMode.ADVENTURE
                            challenge.respawns[traceur]?.let { teleport(it.respawn) }
                        }
                    }
                }
            }
            then("quit") {
                require { this is Player }
                executes {
                    val player = it.sender as Player
                    val sender = it.sender
                    player.traceur.apply {
                        challenge?.let {
                            it.removeTraceur(this)
                            sender.sendFeedback("${player.name}(은)는 ${it.level.name} 레벨 도전을 포기했습니다.")
                        } ?: sender.sendFeedback("${player.name}(은)는 도전 중인 레벨이 없습니다.")
                    }
                }
                then("player" to playerTarget()) {
                    executes {
                        val player = it.parseArgument<ArrayList<Player>>("player")
                        val sender = it.sender
                        player.forEach { player ->
                            player.traceur.apply {
                                challenge?.let {
                                    it.removeTraceur(this)
                                    sender.sendFeedback("${player.name}(은)는 ${it.level.name} 레벨 도전을 포기했습니다.")
                                } ?: sender.sendFeedback("${player.name}(은)는 도전 중인 레벨이 없습니다.")
                            }
                        }
                    }
                }
            }
            then("stop") {
                then("level" to LevelArgument) {
                    executes {
                        val sender = it.sender
                        val level = it.parseArgument<Level>("level")
                        if(level.challenge == null) {
                            sender.sendFeedback("도전 진행중이 아닙니다.")
                        } else {
                            level.stopChallenge()
                            sender.sendFeedback("${level.name} 도전을 종료했습니다.")
                        }
                    }
                }
            }
            then("world") {
                then("create") {
                    then("name" to string()) {
                        executes {
                            val creator = WorldCreator.name(it.parseArgument("name"))
                            creator.environment(World.Environment.NORMAL)
                            creator.type(WorldType.FLAT)
                            val world = creator.createWorld()
                            Bukkit.getWorlds().add(world)
                        }
                    }
                }
                then("remove") {
                    then("world" to world()) {
                        executes {
                            val world: World = it.parseArgument("world")
                            Bukkit.unloadWorld(world, true)
                            Bukkit.getWorlds().remove(world)
                        }
                    }
                }
                then("move") {
                    then("world" to world()) {
                        executes {
                            (it.sender as Player).teleport(Location(it.parseArgument("world"), 0.0, 10.0, 0.0))
                        }
                    }
                }
            }
        }
    }
}
object LevelArgument : KommandArgument<Level> {
    override fun parse(context: KommandContext, param: String): Level? {
        return BoatRace.levels[param]
    }

    override fun listSuggestion(context: KommandContext, target: String): Collection<String> {
        return BoatRace.levels.keys.filter { it.startsWith(target, true) }
    }
}