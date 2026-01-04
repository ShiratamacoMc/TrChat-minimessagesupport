package me.arasple.mc.trchat.util.color

import org.bukkit.ChatColor
import taboolib.module.nms.MinecraftVersion

fun isHigherOrEqual11600() = MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_16)

fun isFormat(char: Char): Boolean {
    val color = ChatColor.getByChar(char)
    return color != null && color.isFormat
}

/**
 * 颜色化字符串，支持旧格式（&颜色代码、&#HEX、<rainbow>、<gradient>）
 * MiniMessage格式会在Component构建时处理
 */
fun String.colorify() = HexUtils.colorify(this)

/**
 * 解析MiniMessage格式为Component
 * 支持MiniMessage格式和旧格式的混合使用
 */
fun String.parseMiniMessage() = MiniMessageUtil.parseMixedFormat(this)

/**
 * 根据权限解析消息
 * @param hasMiniMessagePermission 是否有MiniMessage权限
 * @param hasLegacyPermission 是否有Legacy权限
 * @return 解析后的Component
 */
fun String.parseWithPermission(hasMiniMessagePermission: Boolean, hasLegacyPermission: Boolean) = 
    MiniMessageUtil.parseWithPermission(this, hasMiniMessagePermission, hasLegacyPermission)

fun String.parseLegacy() = HexUtils.parseLegacy(this)

fun String.parseHex() = HexUtils.parseHex(this)

fun String.parseRainbow() = HexUtils.parseRainbow(this)

fun String.parseGradients() = HexUtils.parseGradients(this)