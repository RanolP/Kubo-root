package io.github.ranolp.kubo.telegram.bot

import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import io.github.ranolp.kubo.KuboAdapter
import io.github.ranolp.kubo.general.command.CommandData
import io.github.ranolp.kubo.general.command.CommandParser
import io.github.ranolp.kubo.general.objects.User
import io.github.ranolp.kubo.telegram.Telegram
import io.github.ranolp.kubo.telegram.bot.event.TelegramBotHearEvent
import io.github.ranolp.kubo.telegram.bot.event.TelegramUpdateEvent
import io.github.ranolp.kubo.telegram.bot.functions.TelegramFunction
import io.github.ranolp.kubo.telegram.bot.objects.TelegramUser
import io.github.ranolp.kubo.telegram.errors.NotOkError

class TelegramBotAdapter(option: TelegramBotOption) : KuboAdapter<TelegramBotOption>(option, Telegram.BOT_SIDE) {
    private object getMe : TelegramFunction<TelegramUser>("getMe") {
        operator fun invoke() = request()
        override fun parse(request: Request, response: Response, result: String): TelegramUser {
            return workObject(result, ::TelegramUser) ?: throw NotOkError
        }
    }

    internal lateinit var hearer: UpdateHearer
    override val myself: User by lazy {
        getMe()
    }

    override fun login() {
        hearer = when (option.hearerType) {
            HearerType.LONG_POLLING -> {
                if (option is LongPollingTelegramBotOption) {
                    LongPollingUpdateHearer.daemon = option.daemon
                    LongPollingUpdateHearer.timeout = option.timeout
                }
                LongPollingUpdateHearer
            }
            else -> null!!
        }
        hearer.receive = { fireEvent(TelegramUpdateEvent(it)) }
        hearer.start()
        watch<TelegramUpdateEvent> {
            it.update.let {
                it.message?.let {
                    fireEvent(TelegramBotHearEvent(it))
                }
            }
        }
        commandParser {
            val raw = it.text!!
            if (raw[0] != '/') {
                CommandParser.Result.FAIL
            } else {
                // get space's offset
                val space = raw.indexOf(' ')
                val at = when (space) {
                    -1 -> raw
                    else -> raw.substring(0..space)
                }.indexOf('@', 1)
                val name = when {
                    at == -1 && space == -1 -> raw.substring(1)
                    at == -1 && space != -1 -> raw.substring(1..space - 1)
                    else -> raw.substring(1..at - 1)
                }
                if (!when {
                    at == -1 -> ""
                    space == -1 -> raw.substring(at + 1)
                    else -> raw.substring(at + 1..space - 1)
                }.let { it.isEmpty() || it.equals(Telegram.getAdapter().getUsername(), true) }) {
                    CommandParser.Result.FAIL
                } else {
                    val arguments = when (space) {
                        -1 -> emptyList<String>()
                        else -> raw.substring(space + 1).split(' ')
                    }
                    CommandParser.Result(true, CommandData(name, it, arguments))
                }
            }
        }
    }

    override fun logout() {
        // Do nothing, Add some if required
    }

    fun getUsername() = option.botName

    fun getToken() = option.token
}