package me.arasple.mc.trchat.module.display.format.obj

import me.arasple.mc.trchat.module.conf.file.Settings
import me.arasple.mc.trchat.module.internal.script.Condition
import me.arasple.mc.trchat.util.*
import me.arasple.mc.trchat.util.color.parseMiniMessage
import me.arasple.mc.trchat.util.color.parseToShadowColor
import net.kyori.adventure.text.format.ShadowColor
import org.bukkit.command.CommandSender
import taboolib.common.platform.function.warning
import taboolib.common.util.replaceWithOrder
import taboolib.module.chat.ComponentText
import taboolib.module.chat.Components
import taboolib.module.chat.impl.AdventureComponent
import taboolib.module.chat.impl.DefaultComponent

sealed interface Style {

    val contents: List<Pair<String, Condition?>>

    fun process(component: ComponentText, content: String)

    data class Font(override val contents: List<Pair<String, Condition?>>) : Style {
        override fun process(component: ComponentText, content: String) {
            component.font(content)
        }
    }

    data class Insertion(override val contents: List<Pair<String, Condition?>>) : Style {
        override fun process(component: ComponentText, content: String) {
            component.clickInsertText(content)
        }
    }

    data class Shadow(override val contents: List<Pair<String, Condition?>>) : Style {
        override fun process(component: ComponentText, content: String) {
            val color = content.parseToShadowColor()
            try {
                if (component is DefaultComponent) {
                    component.latest.forEach {
                        it.shadowColor = color.toJavaColor()
                    }
                } else if (component is AdventureComponent) {
                    component.latest.shadowColor(ShadowColor.shadowColor(color.toJavaColor().rgb))
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                warning("Shadow color is unsupported for this version.")
            }

        }
    }

    sealed interface Hover : Style {

        data class Text(override val contents: List<Pair<String, Condition?>>) : Hover {
            override fun process(component: ComponentText, content: String) {
                if (Settings.simpleHover) {
                    component.hoverText(content.parseSimple())
                } else {
                    // 使用 MiniMessage 解析，支持完整的 MiniMessage 标签
                    if (isDragonCoreHooked) {
                        component.hoverText(Components.text(content, color = false))
                    } else if (Components.useAdventure) {
                        // 直接使用 MiniMessage 解析 hover 内容
                        val parsedComponent = content.parseMiniMessage()
                        component.hoverText(AdventureComponent(parsedComponent))
                    } else {
                        // 回退到旧的解析方式
                        component.hoverText(Components.text(content))
                    }
                }
            }
        }
    }

    sealed interface Click : Style {

        data class Suggest(override val contents: List<Pair<String, Condition?>>) : Style {
            override fun process(component: ComponentText, content: String) {
                component.clickSuggestCommand(content)
            }
        }

        data class Command(override val contents: List<Pair<String, Condition?>>) : Style {
            override fun process(component: ComponentText, content: String) {
                component.clickRunCommand(content)
            }
        }

        data class Url(override val contents: List<Pair<String, Condition?>>) : Style {
            override fun process(component: ComponentText, content: String) {
                component.clickOpenURL(content)
            }
        }

        data class Copy(override val contents: List<Pair<String, Condition?>>) : Style {
            override fun process(component: ComponentText, content: String) {
                try {
                    component.clickCopyToClipboard(content)
                } catch (_: Throwable) {
                    component.clickSuggestCommand(content)
                }
            }
        }

        data class File(override val contents: List<Pair<String, Condition?>>) : Style {
            override fun process(component: ComponentText, content: String) {
                component.clickOpenFile(content)
            }
        }
    }

    companion object {

        fun Style.applyTo(component: ComponentText, sender: CommandSender, vararg vars: String) {
            val content = when (this) {
                is Font -> {
                    contents.firstOrNull { it.second.pass(sender) }?.first
                }
                is Hover.Text -> {
                    contents.filter { it.second.pass(sender) }.joinToString("\n") { it.first }
                        .parseInline(sender).setPlaceholders(sender).replaceWithOrder(*vars)
                }
                else -> {
                    contents.firstOrNull { it.second.pass(sender) }?.first
                        ?.parseInline(sender)?.setPlaceholders(sender)?.replaceWithOrder(*vars)
                }
            }
            if (content != null) {
                process(component, content)
            }
        }
    }
}
