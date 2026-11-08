package kr.toxicity.hud.bootstrap.bukkit.module

import kr.toxicity.hud.bootstrap.bukkit.module.bukkit.BukkitEntityModule
import kr.toxicity.hud.bootstrap.bukkit.module.bukkit.BukkitItemModule
import kr.toxicity.hud.bootstrap.bukkit.module.bukkit.BukkitStandardModule
import kr.toxicity.hud.bootstrap.bukkit.module.kokonut.KokonutCurrencyModule

val MODULE_BUKKIT = mapOf(
    "standard" to {
        BukkitStandardModule()
    },
    "entity" to {
        BukkitEntityModule()
    },
    "currency" to {
        KokonutCurrencyModule()
    },
    "item" to {
        BukkitItemModule()
    }
)