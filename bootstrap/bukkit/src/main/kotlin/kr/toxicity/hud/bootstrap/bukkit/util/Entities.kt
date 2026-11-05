package kr.toxicity.hud.bootstrap.bukkit.util

import kr.toxicity.hud.api.bukkit.nms.NMS
import kr.toxicity.hud.util.BOOTSTRAP
import kr.toxicity.hud.util.VOLATILE_CODE
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.plugin.Plugin

val LivingEntity.maximumHealth
    get() = getAttribute(Attribute.MAX_HEALTH)!!.value

inline val <reified T : Entity> T.adapt
    get() = this
