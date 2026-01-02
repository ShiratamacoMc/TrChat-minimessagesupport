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
open class Text(val content: String, val condition: Condition?) {

    val dynamic = papiRegex.containsMatchIn(content)

    open fun process(sender: CommandSender, vararg vars: String): ComponentText {
        var text = KetherHandler.parseInline(content, sender)
        if (sender is Player && dynamic) {
            text = text.setPlaceholders(sender)
        }
        text = hookItemsAdder.replaceFontImages(text, null)
        text = text.replaceWithOrder(*vars)
        
        // 使用 MiniMessage 解析
        return if (Components.useAdventure) {
            val parsedComponent = text.parseMiniMessage()
            AdventureComponent(parsedComponent)
        } else if (isDragonCoreHooked) {
            Components.text(text, color = false)
        } else {
            // 非 Adventure 环境，使用 Components.text
            Components.text(text)
        }
    }
}