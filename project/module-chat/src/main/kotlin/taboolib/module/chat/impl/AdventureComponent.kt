package taboolib.module.chat.impl

import net.kyori.adventure.key.Key
import net.kyori.adventure.nbt.api.BinaryTagHolder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.chat.ComponentSerializer
import taboolib.common.UnsupportedVersionException
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.function.onlinePlayers
import taboolib.module.chat.*
import java.awt.Color
import java.util.*

class AdventureComponent() : ComponentText {

    constructor(from: Component) : this() {
        left.append(from)
    }

    val left: TextComponent.Builder = Component.text()
    var latest: TextComponent.Builder = Component.text()
    val component: Component
        get() = Component.text().append(left, latest).build()

    companion object {
        // 使用 MiniMessage.builder() 构建实例，确保包含所有标准标签（包括 head 等新标签）
        private val miniMessage: MiniMessage by lazy { 
            MiniMessage.builder()
                .tags(TagResolver.standard())
                .build()
        }
        private val legacySerializer: LegacyComponentSerializer by lazy { LegacyComponentSerializer.legacySection() }

        private operator fun TextComponent.Builder.plusAssign(other: Component) {
            append(other)
        }
    }

    override fun toRawMessage(): String {
        return GsonComponentSerializer.gson().serialize(component)
    }

    override fun toLegacyText(): String {
        return LegacyComponentSerializer.legacySection().serialize(component)
    }

    override fun toPlainText(): String {
        return PlainTextComponentSerializer.plainText().serialize(component)
    }

    override fun broadcast() {
        onlinePlayers().forEach { sendTo(it) }
    }

    override fun sendTo(sender: ProxyCommandSender) {
        if (sender is ProxyPlayer) {
            sender.sendRawMessage(toRawMessage())
        } else {
            sender.sendMessage(toLegacyText())
        }
    }

    override fun newLine(): ComponentText {
        return append("\n")
    }

    override fun plusAssign(text: String) {
        append(text)
    }

    override fun plusAssign(other: ComponentText) {
        append(other)
    }

    override fun append(text: String, color: Boolean): ComponentText {
        flush()
        
        // 始终尝试解析 MiniMessage 格式，因为它不仅包含颜色，还有 hover、click、font 等功能
        try {
            if (color) {
                // color = true: 转换旧格式 + 解析 MiniMessage
                val converted = convertLegacyToMiniMessage(text)
                val parsedComponent = miniMessage.deserialize(converted)
                latest += parsedComponent
            } else {
                // color = false: 只解析 MiniMessage，不转换旧格式
                // 这样可以保留 MiniMessage 的所有功能（hover、click、font 等）
                val parsedComponent = miniMessage.deserialize(text)
                latest += parsedComponent
            }
        } catch (e: Exception) {
            // 如果 MiniMessage 解析失败
            if (color) {
                // color = true: 尝试 legacy 格式
                try {
                    latest += legacySerializer.deserialize(text)
                } catch (e2: Exception) {
                    // 最后回退到纯文本
                    latest.content(text)
                }
            } else {
                // color = false: 直接使用纯文本
                latest.content(text)
            }
        }
        return this
    }
    
    /**
     * 转换旧格式到 MiniMessage（简化版本，用于 module-chat）
     */
    private fun convertLegacyToMiniMessage(text: String): String {
        if (!text.contains('&') && !text.contains('§')) {
            return text
        }
        
        var result = text
        
        // 转换 hex 颜色: &#FFFFFF -> <#FFFFFF>
        result = result.replace(Regex("&#([A-Fa-f0-9]{6})")) { "<#${it.groupValues[1]}>" }
        result = result.replace(Regex("&\\{#([A-Fa-f0-9]{6})}")) { "<#${it.groupValues[1]}>" }
        
        // 转换颜色代码
        val colorMap = mapOf(
            '0' to "<black>", '1' to "<dark_blue>", '2' to "<dark_green>", '3' to "<dark_aqua>",
            '4' to "<dark_red>", '5' to "<dark_purple>", '6' to "<gold>", '7' to "<gray>",
            '8' to "<dark_gray>", '9' to "<blue>", 'a' to "<green>", 'b' to "<aqua>",
            'c' to "<red>", 'd' to "<light_purple>", 'e' to "<yellow>", 'f' to "<white>",
            'r' to "<reset>", 'k' to "<obfuscated>", 'l' to "<bold>", 'm' to "<strikethrough>",
            'n' to "<underline>", 'o' to "<italic>"
        )
        
        val builder = StringBuilder(result.length + 50)
        var i = 0
        while (i < result.length) {
            if (i < result.length - 1 && (result[i] == '&' || result[i] == '§')) {
                val code = result[i + 1].lowercaseChar()
                if (colorMap.containsKey(code)) {
                    builder.append(colorMap[code])
                    i += 2
                    continue
                }
            }
            builder.append(result[i])
            i++
        }
        
        return builder.toString()
    }

    override fun append(other: ComponentText): ComponentText {
        other as? AdventureComponent ?: throw UnsupportedVersionException()
        flush()
        latest += other.component
        return this
    }

    override fun appendTranslation(text: String, vararg obj: Any): ComponentText {
        return appendTranslation(text, obj.toList())
    }

    override fun appendTranslation(text: String, obj: List<Any>): ComponentText {
        flush()
        latest += Component.translatable(text, obj.map { if (it is AdventureComponent) it.component else it as? ComponentLike })
        return this
    }

    override fun appendKeybind(key: String): ComponentText {
        flush()
        latest += Component.keybind(key)
        return this
    }

    override fun appendScore(name: String, objective: String): ComponentText {
        flush()
        latest += Component.score(name, objective)
        return this
    }

    override fun appendSelector(selector: String): ComponentText {
        flush()
        latest += Component.selector(selector)
        return this
    }

    override fun hoverText(text: String): ComponentText {
        return hoverText(ComponentText.of(text))
    }

    override fun hoverText(text: List<String>): ComponentText {
        val component = ComponentText.empty()
        text.forEachIndexed { index, s ->
            component.append(s)
            if (index != text.size - 1) {
                component.newLine()
            }
        }
        return hoverText(component)
    }

    override fun hoverText(text: ComponentText): ComponentText {
        text as? AdventureComponent ?: error("Unsupported component type.")
        // 先 flush，确保所有内容都在 left 中
        flush()
        // 在 left 上设置 hover 事件
        left.hoverEvent(HoverEvent.showText(text.component))
        return this
    }

    override fun hoverItem(id: String, nbt: String): ComponentText {
        latest.hoverEvent(HoverEvent.showItem(Key.key(id), 1, BinaryTagHolder.binaryTagHolder(nbt)))
        return this
    }

    override fun hoverEntity(id: String, type: String?, name: String?): ComponentText {
        return hoverEntity(id, type, name?.let { Components.text(name) })
    }

    override fun hoverEntity(id: String, type: String?, name: ComponentText?): ComponentText {
        val component = if (name is AdventureComponent) name.component else null
        latest.hoverEvent(HoverEvent.showEntity(Key.key(type!!), UUID.fromString(id), component))
        return this
    }

    override fun click(action: ClickAction, value: String): ComponentText {
        when (action) {
            ClickAction.OPEN_URL,
            ClickAction.OPEN_FILE,
            ClickAction.RUN_COMMAND,
            ClickAction.SUGGEST_COMMAND,
            ClickAction.CHANGE_PAGE,
            ClickAction.COPY_TO_CLIPBOARD -> latest.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.valueOf(action.name), value))
            // 插入文本
            ClickAction.INSERTION -> clickInsertText(value)
        }

        return this
    }

    override fun clickOpenURL(url: String): ComponentText {
        return click(ClickAction.OPEN_URL, url)
    }

    override fun clickOpenFile(file: String): ComponentText {
        return click(ClickAction.OPEN_FILE, file)
    }

    override fun clickRunCommand(command: String): ComponentText {
        return click(ClickAction.RUN_COMMAND, command)
    }

    override fun clickSuggestCommand(command: String): ComponentText {
        return click(ClickAction.SUGGEST_COMMAND, command)
    }

    override fun clickChangePage(page: Int): ComponentText {
        return click(ClickAction.CHANGE_PAGE, page.toString())
    }

    override fun clickCopyToClipboard(text: String): ComponentText {
        return click(ClickAction.COPY_TO_CLIPBOARD, text)
    }

    override fun clickInsertText(text: String): ComponentText {
        latest.insertion(text)
        return this
    }

    override fun decoration(decoration: Decoration): ComponentText {
        when (decoration) {
            Decoration.BOLD -> bold()
            Decoration.ITALIC -> italic()
            Decoration.UNDERLINE -> underline()
            Decoration.STRIKETHROUGH -> strikethrough()
            Decoration.OBFUSCATED -> obfuscated()
        }
        return this
    }

    override fun undecoration(decoration: Decoration): ComponentText {
        when (decoration) {
            Decoration.BOLD -> unbold()
            Decoration.ITALIC -> unitalic()
            Decoration.UNDERLINE -> ununderline()
            Decoration.STRIKETHROUGH -> unstrikethrough()
            Decoration.OBFUSCATED -> unobfuscated()
        }
        return this
    }

    override fun undecoration(): ComponentText {
        unbold()
        unitalic()
        ununderline()
        unstrikethrough()
        unobfuscated()
        return this
    }

    override fun bold(): ComponentText {
        latest.style { it.decoration(TextDecoration.BOLD, true) }
        return this
    }

    override fun unbold(): ComponentText {
        latest.style { it.decoration(TextDecoration.BOLD, false) }
        return this
    }

    override fun italic(): ComponentText {
        latest.style { it.decoration(TextDecoration.ITALIC, true) }
        return this
    }

    override fun unitalic(): ComponentText {
        latest.style { it.decoration(TextDecoration.ITALIC, false) }
        return this
    }

    override fun underline(): ComponentText {
        latest.style { it.decoration(TextDecoration.UNDERLINED, true) }
        return this
    }

    override fun ununderline(): ComponentText {
        latest.style { it.decoration(TextDecoration.UNDERLINED, false) }
        return this
    }

    override fun strikethrough(): ComponentText {
        latest.style { it.decoration(TextDecoration.STRIKETHROUGH, true) }
        return this
    }

    override fun unstrikethrough(): ComponentText {
        latest.style { it.decoration(TextDecoration.STRIKETHROUGH, false) }
        return this
    }

    override fun obfuscated(): ComponentText {
        latest.style { it.decoration(TextDecoration.OBFUSCATED, true) }
        return this
    }

    override fun unobfuscated(): ComponentText {
        latest.style { it.decoration(TextDecoration.OBFUSCATED, false) }
        return this
    }

    override fun font(font: String): ComponentText {
        latest.font(Key.key(font))
        return this
    }

    override fun unfont(): ComponentText {
        latest.font(null)
        return this
    }

    override fun color(color: StandardColors): ComponentText {
        return color(color.toChatColor().color)
    }

    override fun color(color: Color): ComponentText {
        latest.color(TextColor.color(color.rgb))
        return this
    }

    override fun uncolor(): ComponentText {
        latest.color(null)
        return this
    }

    override fun toSpigotObject(): BaseComponent {
        return net.md_5.bungee.api.chat.TextComponent(*ComponentSerializer.parse(toRawMessage()))
    }

    override fun toAdventureObject(): Component {
        return component
    }

    override fun toLegacyRawMessage(): RawMessage {
        return RawMessage(this)
    }

    /** 释放缓冲区 */
    fun flush() {
        left.append(latest)
        latest = Component.text()
    }

    override fun toString(): String {
        return toRawMessage()
    }
}