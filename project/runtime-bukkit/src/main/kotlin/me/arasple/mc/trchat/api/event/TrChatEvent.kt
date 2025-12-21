package me.arasple.mc.trchat.api.event

import me.arasple.mc.trchat.module.display.ChatSession
import me.arasple.mc.trchat.module.display.channel.Channel
import me.arasple.mc.trchat.module.display.channel.PrivateChannel
import me.arasple.mc.trchat.util.pass
import taboolib.module.chat.ComponentText
import taboolib.module.chat.Components
import taboolib.platform.type.BukkitProxyEvent

/**
 * TrChatEvent
 * me.arasple.mc.trchat.api.event
 *
 * @author ItsFlicker
 * @since 2021/8/20 20:53
 */
class TrChatEvent(
    val channel: Channel,
    val session: ChatSession,
    var component: ComponentText,
    @Deprecated("Only for chat preview")
    val forward: Boolean = true
) : BukkitProxyEvent() {

    val player = session.player

    var message = component.toLegacyText()
        set(value) {
            field = value
            component = Components.text(value)
        }

    init {
        if (channel is PrivateChannel) {
            val event = TrChatPrivateEvent(channel, session, component)
            event.call()
            this.isCancelled = event.isCancelled
            this.component = event.component
        }
    }
    fun getFullMessage() : String{
        // 获取符合条件的格式
        val format = channel.formats.firstOrNull { it.condition.pass(player) }
        // 构建完整消息
        val fullComponent = Components.empty()

        // 添加前缀
        format?.prefix?.forEach { prefix ->
            prefix.value.firstOrNull { it.condition.pass(player) }?.content
                ?.toTextComponent(player)?.let { fullComponent.append(it) }
        }

        // 添加消息内容
        fullComponent.append(component)

        // 添加后缀
        format?.suffix?.forEach { suffix ->
            suffix.value.firstOrNull { it.condition.pass(player) }?.content
                ?.toTextComponent(player)?.let { fullComponent.append(it) }
        }

        val fullMessage = fullComponent.toLegacyText()
        return fullMessage
    }
}