package me.arasple.mc.trchat.module.display.format.obj

import me.arasple.mc.trchat.module.internal.hook.hookItemsAdder
import me.arasple.mc.trchat.module.internal.script.Condition
import me.arasple.mc.trchat.module.internal.script.kether.KetherHandler
import me.arasple.mc.trchat.util.color.MiniMessageUtil
import me.arasple.mc.trchat.util.color.parseMiniMessage
import me.arasple.mc.trchat.util.isDragonCoreHooked
import me.arasple.mc.trchat.util.papiRegex
import me.arasple.mc.trchat.util.setPlaceholders
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.util.replaceWithOrder
import taboolib.module.chat.ComponentText
import taboolib.module.chat.Components
import taboolib.module.chat.impl.AdventureComponent

/**
 * @author ItsFlicker
 * @since 2022/1/21 23:21
 */
class Text(val content: String, val condition: Condition?) {

    val dynamic = papiRegex.containsMatchIn(content)

    fun process(sender: CommandSender, vararg vars: String): ComponentText {
        var text = KetherHandler.parseInline(content, sender)
        if (sender is Player) {
            if (dynamic) {
                text = text.setPlaceholders(sender)
            }
            text = hookItemsAdder.replaceFontImages(text, null)
        }
        text = text.replaceWithOrder(*vars)
        
        // 只使用MiniMessage，不接受传统代码
        // 不调用colorify()，避免破坏MiniMessage标签
        return if (Components.useAdventure) {
            // 使用MiniMessage解析
            val parsedComponent = text.parseMiniMessage()
            AdventureComponent(parsedComponent)
        } else if (isDragonCoreHooked) {
            Components.text(text, color = false)
        } else {
            // 如果不使用Adventure，尝试使用MiniMessage（可能通过反射）
            try {
                val minimessageUtilClass = Class.forName("me.arasple.mc.trchat.util.color.MiniMessageUtil")
                val parseMethod = minimessageUtilClass.getMethod("parseMixedFormat", String::class.java)
                val parsedComponent = parseMethod.invoke(null, text) as net.kyori.adventure.text.Component
                AdventureComponent(parsedComponent)
            } catch (e: Exception) {
                Components.text(text)
            }
        }
    }
}