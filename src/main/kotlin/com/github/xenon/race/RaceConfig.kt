package com.github.xenon.race

import com.github.monun.tap.config.Config
import com.github.monun.tap.config.computeConfig
import org.bukkit.Bukkit
import org.bukkit.WorldCreator
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object RaceConfig {
    @Config
    var worlds = arrayListOf<String>()

    fun load(configFile: File) {
        computeConfig(configFile)

        worlds.forEach { name ->
            val creator = WorldCreator.name(name)
            val world = creator.createWorld()
            Bukkit.getWorlds().add(world)
        }
    }

    fun save(configFile: File) {
        val yaml = YamlConfiguration()
        yaml.set("worlds", Bukkit.getWorlds().map { it.name })
        yaml.save(configFile)
    }
}