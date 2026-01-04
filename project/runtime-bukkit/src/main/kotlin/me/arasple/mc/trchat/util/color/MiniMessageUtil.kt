package me.arasple.mc.trchat.util.color

import me.arasple.mc.trchat.util.color.HexUtils.GRADIENT_PATTERN
import me.arasple.mc.trchat.util.color.HexUtils.RAINBOW_PATTERN
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.util.regex.Pattern

/**
 * MiniMessage工具类
 * 提供MiniMessage格式解析和旧格式转换功能
 * 支持MiniMessage格式和旧格式的混合使用
 * 完全支持所有 MiniMessage 标签，包括 hover、click、font 等
 * 
 * @author TrChat
 */
object MiniMessageUtil {

    // 使用单例模式，避免重复创建实例，提升性能
    // 使用 MiniMessage.builder() 构建实例，确保包含所有标准标签（包括 head 等新标签）
    private val miniMessage: MiniMessage by lazy {
        MiniMessage.builder()
            .tags(TagResolver.standard())
            .build()
    }
    
    private val legacySerializer = LegacyComponentSerializer.legacySection()

    // 旧格式颜色代码到MiniMessage的映射
    private val legacyColorMap = mapOf(
        '0' to "<black>",
        '1' to "<dark_blue>",
        '2' to "<dark_green>",
        '3' to "<dark_aqua>",
        '4' to "<dark_red>",
        '5' to "<dark_purple>",
        '6' to "<gold>",
        '7' to "<gray>",
        '8' to "<dark_gray>",
        '9' to "<blue>",
        'a' to "<green>",
        'b' to "<aqua>",
        'c' to "<red>",
        'd' to "<light_purple>",
        'e' to "<yellow>",
        'f' to "<white>",
        'r' to "<reset>"
    )

    // 旧格式装饰代码到MiniMessage的映射
    private val legacyDecorationMap = mapOf(
        'k' to "<obfuscated>",
        'l' to "<bold>",
        'm' to "<strikethrough>",
        'n' to "<underlined>",
        'o' to "<italic>"
    )

    // 检测是否包含MiniMessage格式标签（排除自定义的rainbow和gradient）
    private val minimessageTagPattern = Pattern.compile("<(?!/?(?:rainbow|gradient|r|g)[#>])[^>]+>")
    
    // 缓存 Regex 对象以提升性能
    private val hexPattern1 = Regex("&#[A-Fa-f0-9]{6}")
    private val hexPattern2 = Regex("&\\{#[A-Fa-f0-9]{6}}")

    /**
     * 检测字符串是否包含MiniMessage格式
     */
    fun containsMiniMessageFormat(text: String): Boolean {
        return minimessageTagPattern.matcher(text).find()
    }

    /**
     * 移除所有 MiniMessage 标签，保留标签内的文本内容
     * 例如：<red>Hello</red> -> Hello
     */
    fun stripMiniMessageTags(text: String): String {
        // 移除所有 MiniMessage 标签（包括开始和结束标签）
        return text.replace(Regex("<[^>]+>"), "")
    }

    /**
     * 将旧格式转换为MiniMessage格式
     * 支持：&颜色代码、&#HEX
     * 注意：rainbow和gradient保持原样，由HexUtils处理
     * 优化：避免不必要的字符串操作，提升性能
     */
    fun convertLegacyToMiniMessage(text: String): String {
        // 快速检查：如果没有 & 或 § 符号，直接返回
        if (!text.contains('&') && !text.contains('§')) {
            return text
        }

        var result = text

        // 处理hex颜色格式（转换为MiniMessage格式）
        result = convertHexToMiniMessage(result)

        // 处理legacy颜色代码（&颜色代码）
        result = convertLegacyCodesToMiniMessage(result)

        return result
    }


    /**
     * 转换hex颜色格式到MiniMessage
     * &#FFFFFF -> <#FFFFFF>
     * &{#FFFFFF} -> <#FFFFFF>
     * 优化：使用缓存的 Regex 对象
     */
    private fun convertHexToMiniMessage(text: String): String {
        var result = text
        
        // 处理 &#FFFFFF 格式
        result = hexPattern1.replace(result) { matchResult ->
            val hexCode = matchResult.value.substring(2) // 移除 &#
            "<#$hexCode>"
        }
        
        // 处理 &{#FFFFFF} 格式
        result = hexPattern2.replace(result) { matchResult ->
            val hexCode = matchResult.value.substring(3, matchResult.value.length - 1) // 移除 &{# 和 }
            "<#$hexCode>"
        }
        
        return result
    }

    /**
     * 转换legacy颜色代码到MiniMessage
     * &a -> <green>
     * &l -> <bold>
     * 优化：使用 StringBuilder 提升性能
     * 注意：跳过已经转换的 hex 颜色代码（<#XXXXXX>）和 Minecraft 原生 RGB 格式（§x§R§R§G§G§B§B）
     */
    private fun convertLegacyCodesToMiniMessage(text: String): String {
        if (text.length < 2) return text
        
        val result = StringBuilder(text.length + 50) // 预分配空间
        var i = 0
        
        while (i < text.length) {
            // 跳过 Minecraft 原生 RGB 格式：§x§R§R§G§G§B§B (14个字符)
            if (i < text.length - 13 && 
                (text[i] == '§' || text[i] == '&') && 
                text[i + 1].lowercaseChar() == 'x') {
                // 检查后面是否跟着 12 个字符（§R§R§G§G§B§B）
                var isValidMinecraftRgb = true
                for (j in 2..13 step 2) {
                    if (i + j >= text.length || (text[i + j] != '§' && text[i + j] != '&')) {
                        isValidMinecraftRgb = false
                        break
                    }
                    if (i + j + 1 >= text.length) {
                        isValidMinecraftRgb = false
                        break
                    }
                    val hexChar = text[i + j + 1].lowercaseChar()
                    if (!hexChar.isDigit() && hexChar !in 'a'..'f') {
                        isValidMinecraftRgb = false
                        break
                    }
                }
                
                if (isValidMinecraftRgb) {
                    // 提取 RGB 值并转换为 MiniMessage 格式
                    val r1 = text[i + 3]
                    val r2 = text[i + 5]
                    val g1 = text[i + 7]
                    val g2 = text[i + 9]
                    val b1 = text[i + 11]
                    val b2 = text[i + 13]
                    val hexColor = "<#$r1$r2$g1$g2$b1$b2>"
                    result.append(hexColor)
                    i += 14
                    continue
                }
            }
            
            // 跳过已经转换的 MiniMessage hex 颜色标签 <#XXXXXX>
            if (text[i] == '<' && i + 8 < text.length && text[i + 1] == '#') {
                // 检查是否是有效的 hex 颜色标签
                var isValidHex = true
                for (j in 2..7) {
                    val c = text[i + j]
                    if (!c.isLetterOrDigit() || (c.isLetter() && !c.lowercaseChar().let { it in 'a'..'f' })) {
                        isValidHex = false
                        break
                    }
                }
                if (isValidHex && i + 8 < text.length && text[i + 8] == '>') {
                    // 这是一个有效的 hex 颜色标签，直接复制
                    result.append(text.substring(i, i + 9))
                    i += 9
                    continue
                }
            }
            
            if (i < text.length - 1 && (text[i] == '&' || text[i] == '§')) {
                val code = text[i + 1].lowercaseChar()
                
                // 检查是否是颜色代码
                if (legacyColorMap.containsKey(code)) {
                    result.append(legacyColorMap[code])
                    i += 2
                    continue
                }
                
                // 检查是否是装饰代码
                if (legacyDecorationMap.containsKey(code)) {
                    result.append(legacyDecorationMap[code])
                    i += 2
                    continue
                }
            }
            
            result.append(text[i])
            i++
        }
        
        return result.toString()
    }

    /**
     * 解析MiniMessage格式文本为Component
     * 如果包含旧格式，先转换再解析
     */
    fun parseMiniMessage(text: String, convertLegacy: Boolean = true): Component {
        val processedText = if (convertLegacy) {
            // 转换简单的旧格式（&颜色代码、&#HEX）到MiniMessage
            convertLegacyToMiniMessage(text)
        } else {
            text
        }
        
        return try {
            miniMessage.deserialize(processedText)
        } catch (e: Exception) {
            // 如果解析失败，尝试使用legacy格式
            legacySerializer.deserialize(text)
        }
    }

    /**
     * 解析MiniMessage格式文本为Component（不转换旧格式）
     */
    fun parseMiniMessageOnly(text: String): Component {
        return try {
            miniMessage.deserialize(text)
        } catch (e: Exception) {
            // 如果解析失败，返回空组件
            Component.empty()
        }
    }

    /**
     * 将Component序列化为MiniMessage格式
     */
    fun serializeToMiniMessage(component: Component): String {
        return miniMessage.serialize(component)
    }


    /**
     * 混合解析：支持 MiniMessage 和传统代码的混合使用
     * 策略：
     * 1. 先转换简单的旧格式（&颜色代码、&#HEX）到 MiniMessage
     * 2. 使用 MiniMessage 解析（完全支持所有标签：color、hover、click、font 等）
     * 3. 如果解析失败，回退到 legacy
     */
    fun parseMixedFormat(text: String): Component {
        if (text.isEmpty()) {
            return Component.empty()
        }
        
        return try {
            // 快速路径：如果文本中没有旧格式代码，直接解析
            if (!text.contains('&') && !text.contains('§')) {
                miniMessage.deserialize(text)
            } else {
                // 转换旧格式到 MiniMessage，然后解析
                val converted = convertLegacyToMiniMessage(text)
                miniMessage.deserialize(converted)
            }
        } catch (e: Exception) {
            // 如果解析失败，尝试直接解析原始文本（可能已经是纯 MiniMessage 格式）
            try {
                miniMessage.deserialize(text)
            } catch (e2: Exception) {
                // 最后的后备方案：使用 legacy 格式
                try {
                    legacySerializer.deserialize(text)
                } catch (e3: Exception) {
                    // 如果所有方法都失败，返回纯文本
                    Component.text(text)
                }
            }
        }
    }
    
    /**
     * 根据权限解析消息
     * @param text 要解析的文本
     * @param hasMiniMessagePermission 是否有MiniMessage权限
     * @param hasLegacyPermission 是否有Legacy权限
     * @return 解析后的Component
     */
    fun parseWithPermission(text: String, hasMiniMessagePermission: Boolean, hasLegacyPermission: Boolean): Component {
        if (text.isEmpty()) {
            return Component.empty()
        }
        
        return if (hasMiniMessagePermission) {
            // 有MiniMessage权限，解析MiniMessage格式
            parseMixedFormat(text)
        } else if (hasLegacyPermission) {
            // 只有Legacy权限，只解析Legacy格式
            // 移除MiniMessage标签，然后解析Legacy格式
            val strippedText = stripMiniMessageTags(text)
            parseMixedFormat(strippedText)
        } else {
            // 没有任何颜色权限，发送纯文本
            val strippedText = stripMiniMessageTags(text)
            Component.text(strippedText)
        }
    }
}