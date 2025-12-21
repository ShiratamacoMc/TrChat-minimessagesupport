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
    private val miniMessage: MiniMessage by lazy {
        MiniMessage.builder()
            .tags(
                TagResolver.builder()
                    .resolver(StandardTags.color())
                    .resolver(StandardTags.decorations())
                    .resolver(StandardTags.gradient())
                    .resolver(StandardTags.rainbow())
                    .resolver(StandardTags.reset())
                    .resolver(StandardTags.clickEvent())
                    .resolver(StandardTags.hoverEvent())
                    .resolver(StandardTags.keybind())
                    .resolver(StandardTags.translatable())
                    .resolver(StandardTags.insertion())
                    .resolver(StandardTags.font())
                    .resolver(StandardTags.newline())
                    .resolver(StandardTags.selector())
                    .resolver(StandardTags.transition())
                    .build()
            )
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
        'n' to "<underline>",
        'o' to "<italic>"
    )

    // 检测是否包含MiniMessage格式标签（排除自定义的rainbow和gradient）
    private val minimessageTagPattern = Pattern.compile("<(?!/?(?:rainbow|gradient|r|g)[#>])[^>]+>")

    /**
     * 检测字符串是否包含MiniMessage格式
     */
    fun containsMiniMessageFormat(text: String): Boolean {
        return minimessageTagPattern.matcher(text).find()
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
     */
    private fun convertHexToMiniMessage(text: String): String {
        var result = text
        
        // 处理 &#FFFFFF 格式
        result = result.replace(Regex("&#([A-Fa-f0-9]{6})")) { matchResult ->
            "<#${matchResult.groupValues[1]}>"
        }
        
        // 处理 &{#FFFFFF} 格式
        result = result.replace(Regex("&\\{#([A-Fa-f0-9]{6})}")) { matchResult ->
            "<#${matchResult.groupValues[1]}>"
        }
        
        return result
    }

    /**
     * 转换legacy颜色代码到MiniMessage
     * &a -> <green>
     * &l -> <bold>
     * 优化：使用 StringBuilder 提升性能
     */
    private fun convertLegacyCodesToMiniMessage(text: String): String {
        if (text.length < 2) return text
        
        val result = StringBuilder(text.length + 50) // 预分配空间
        var i = 0
        
        while (i < text.length) {
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
}

