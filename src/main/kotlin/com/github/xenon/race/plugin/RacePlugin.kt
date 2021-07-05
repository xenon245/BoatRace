package com.github.xenon.race.plugin

import com.github.monun.kommand.kommand
import com.github.xenon.race.BoatRace
import com.github.xenon.race.RaceConfig
import com.github.xenon.race.RaceListener
import com.github.xenon.race.command.KommandRace
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class RacePlugin : JavaPlugin() {
    override fun onEnable() {
        val configFile = File(dataFolder, "config.yml")
        RaceConfig.load(configFile)
        BoatRace.initialize(this)

        server.apply {
            pluginManager.registerEvents(RaceListener(), this@RacePlugin)
        }

        kommand {
            register("race") {
                KommandRace.register(this)
            }
        }
    }

    override fun onDisable() {
        val configFile = File(dataFolder, "config.yml")
        RaceConfig.save(configFile)

        BoatRace.levels.values.forEach {
            it.save()
            it.challenge?.run {
                it.stopChallenge()
            }
        }
    }
}