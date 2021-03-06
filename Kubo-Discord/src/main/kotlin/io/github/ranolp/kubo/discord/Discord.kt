package io.github.ranolp.kubo.discord

import io.github.ranolp.kubo.Adapters
import io.github.ranolp.kubo.general.side.Side

object Discord {
    val SIDE = Side("Discord", true, true)

    fun getAdapter() = Adapters.getTypedAdapter<DiscordAdapter>(SIDE)!!
}