package com.github.xenon.race

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.util.BoundingBox
import java.io.File
import java.io.FileInputStream
import java.security.DigestInputStream
import java.security.MessageDigest

class Level {
    val name: String

    private val file: File

    val region: CuboidRegion

    var challenge: Challenge? = null
        private set

    private var valid = true

    constructor(name: String, region: CuboidRegion) {
        checkNotNull(region.world) { "Region must have region!" }

        this.name = name
        this.region = region.clone()
        file = File(BoatRace.levelFolder, "$name.yml")
    }
    constructor(file: File) {
        name = file.name.removeSuffix(".yml")
        this.file = file

        YamlConfiguration.loadConfiguration(file).run {
            region = CuboidRegion(
                BukkitAdapter.adapt(Bukkit.getWorlds().filter { it.name == get("world").toString() }.first() ),
                getBlockVector3("min"),
                getBlockVector3("max")
            )
        }
    }

    fun startChallenge(): Challenge {
        checkState()
        check(challenge == null) { "Challenge is already in progress." }

        val challenge = Challenge(this).apply {
            parseBlocks()
        }
        this.challenge = challenge
        return challenge
    }

    fun stopChallenge() {
        checkState()

        challenge.let { challenge ->
            checkNotNull(challenge) { "Challenge is not in progress." }
            this.challenge = null
            challenge.destroy()

            val world = BukkitAdapter.asBukkitWorld(region.world).world
            val min = region.minimumPoint.run { world.getBlockAt(x, y, z) }
            val max = region.maximumPoint.run { world.getBlockAt(x, y, z) }
            val box = BoundingBox.of(min, max)
            world.getNearbyEntities(box) { it !is Player }.forEach { it.remove() }
        }
    }

    fun save() {
        checkState()

        val config = YamlConfiguration()
        region.let { region ->
            config.set("world", region.world!!.name)
            config.setPoint("min", region.minimumPoint)
            config.setPoint("max", region.maximumPoint)
        }

        file.parentFile.mkdirs()
        config.save(file)
    }

    fun remove() {
        challenge?.run { stopChallenge() }
        valid = false
        file.delete()
        BoatRace.removeLevel(this)
    }


    private fun checkState() {
        require(valid) { "Invalid $this" }
    }

    private val File.md5Digest: ByteArray
        get() {
            val md = MessageDigest.getInstance("MD5")
            DigestInputStream(FileInputStream(this).buffered(), md).use {
                while (true)
                    if (it.read() == -1)
                        break
            }
            return md.digest()
        }

    private fun ConfigurationSection.getBlockVector3(path: String): BlockVector3 {
        return getConfigurationSection(path)!!.run {
            BlockVector3.at(
                getInt("x"),
                getInt("y"),
                getInt("z")
            )
        }
    }

    private fun ConfigurationSection.setPoint(path: String, point: BlockVector3) {
        createSection(path).apply {
            this["x"] = point.blockX
            this["y"] = point.blockY
            this["z"] = point.blockZ
        }
    }
}