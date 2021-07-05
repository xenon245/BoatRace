package com.github.xenon.race

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.regions.Region
import org.bukkit.block.Block

class Challenge(val level: Level) {
    lateinit var dataMap: Map<RaceBlock, Set<RaceBlockData>>

    lateinit var dataByBlock: Map<Block, RaceBlockData>

    private lateinit var startLocs: List<SpawnBlock.SpawnData>

    private lateinit var boatLocs: List<BoatBlock.BoatBlockData>

    private var _traceurs = HashSet<Traceur>()

    private var _respawns = HashMap<Traceur, Respawnable>()

    val traceurs: Set<Traceur>
        get() = _traceurs

    val respawns: Map<Traceur, Respawnable>
        get() = _respawns

    private var valid = true

    internal fun parseBlocks() {
        checkState()

        val dataMap = HashMap<RaceBlock, HashSet<RaceBlockData>>()
        val dataByBlock = HashMap<Block, RaceBlockData>()
        val startLocs = ArrayList<SpawnBlock.SpawnData>()
        val boatLocs = ArrayList<BoatBlock.BoatBlockData>()

        level.region.forEachBlocks { block ->
            RaceBlocks.getBlock(block)?.let { raceBlock ->
                val data = raceBlock.createBlockData(block).apply {
                    onInitialize(this@Challenge)
                }
                dataMap.computeIfAbsent(raceBlock) { HashSet() } += data
                dataByBlock[block] = data

                if(data is SpawnBlock.SpawnData) {
                    startLocs.add(data)
                } else if(data is BoatBlock.BoatBlockData) {
                    boatLocs.add(data)
                }
            }
        }

        this.dataMap = ImmutableMap.copyOf(dataMap)
        this.dataByBlock = ImmutableMap.copyOf(dataByBlock)
        this.startLocs = ImmutableList.copyOf(startLocs)
        this.boatLocs = ImmutableList.copyOf(boatLocs)
    }

    fun addTraceur(traceur: Traceur) {
        checkState()

        traceur.challenge?.let {
            if(this == this) return

            it.removeTraceur(traceur)
        }

        if(_traceurs.add(traceur)) {
            startLocs.let { locs ->
                if(locs.isNotEmpty()) {
                    _respawns[traceur] = locs.random()
                }
            }

            traceur.challenge = this
        }
    }

    fun removeTraceur(traceur: Traceur) {
        checkState()

        if(_traceurs.remove(traceur)) {
            _respawns.remove(traceur)
            traceur.challenge = null
        }
    }

    internal fun destroy() {
        checkState()

        valid = false

        _traceurs.apply {
            forEach { it.challenge = null }
            clear()
        }
        _respawns.clear()

        dataByBlock.values.forEach {
            it.destroy()
        }
    }

    private fun checkState() {
        check(this.valid) { "Invalid $this" }
    }

    fun spawnBoat() {
        boatLocs.random().onInitialize(this)
    }

    private fun Region.forEachBlocks(action: (Block) -> Unit) {
        val world = BukkitAdapter.asBukkitWorld(world).world

        forEach {
            action.invoke(world.getBlockAt(it.x, it.y, it.z))
        }
    }

    internal fun setSpawn(traceur: Traceur, respawnable: Respawnable): Respawnable? {
        return _respawns.put(traceur, respawnable)
    }
}