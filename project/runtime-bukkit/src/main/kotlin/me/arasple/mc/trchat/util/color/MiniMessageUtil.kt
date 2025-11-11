package me.arasple.mc.trchat.util.color

import me.arasple.mc.trchat.util.color.HexUtils.GRADIENT_PATTERN
import me.arasple.mc.trchat.util.color.HexUtils.RAINBOW_PATTERN
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.util.regex.Pattern

/**
 * MiniMessage工具类
 * 提供MiniMessage格式解析和旧格式转换功能
 * 支持MiniMessage格式和旧格式的混合使用
 * 
 * @author TrChat
 */
object MiniMessageUtil {

    private val miniMessage = MiniMessage.miniMessage()
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
     */
    fun convertLegacyToMiniMessage(text: String): String {
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
     */
    private fun convertLegacyCodesToMiniMessage(text: String): String {
        var result = text
        var i = 0
        
        while (i < result.length - 1) {
            if (result[i] == '&' || result[i] == '§') {
                val code = result[i + 1].lowercaseChar()
                
                // 检查是否是颜色代码
                if (legacyColorMap.containsKey(code)) {
                    val minimessageTag = legacyColorMap[code]!!
                    result = result.substring(0, i) + minimessageTag + result.substring(i + 2)
                    i += minimessageTag.length - 2
                    continue
                }
                
                // 检查是否是装饰代码
                if (legacyDecorationMap.containsKey(code)) {
                    val minimessageTag = legacyDecorationMap[code]!!
                    result = result.substring(0, i) + minimessageTag + result.substring(i + 2)
                    i += minimessageTag.length - 2
                    continue
                }
            }
            i++
        }
        
        return result
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
     * 混合解析：只使用MiniMessage，不接受传统代码
     * 策略：
     * 1. 直接使用MiniMessage解析（不经过colorify，避免破坏标签）
     * 2. 转换简单的旧格式（&颜色代码、&#HEX）到MiniMessage
     * 3. 确保所有MiniMessage标签（如font、click、hover等）都能正确解析
     */
    fun parseMixedFormat(text: String): Component {
        // 直接转换旧格式到MiniMessage，然后解析
        // 不经过colorify，避免破坏MiniMessage标签
        val converted = convertLegacyToMiniMessage(text)
        return try {
            miniMessage.deserialize(converted)
        } catch (e: Exception) {
            // 如果解析失败，尝试直接解析（可能已经是纯MiniMessage格式）
            try {
                miniMessage.deserialize(text)
            } catch (e2: Exception) {
                // 如果还是失败，使用legacy格式作为最后的后备
                legacySerializer.deserialize(text)
            }
        }
    }
}

