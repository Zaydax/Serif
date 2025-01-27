package xyz.room409.serif.serif_shared
import kotlin.collections.*

sealed class MatrixState {
    val version: String
        get() {
            return "Serif Matrix client, pre-alpha on ${Platform().platform}"
        }
    abstract fun refresh(): MatrixState
}
class MatrixLogin(val login_message: String, val mclient: MatrixClient) : MatrixState() {
    constructor() : this(
        login_message = "Please enter your username and password\n",
        mclient = MatrixClient()
    )
    // No need to refresh
    override fun refresh(): MatrixState = this
    fun login(username: String, password: String, onSync: () -> Unit): MatrixState {
        when (val loginResult = mclient.login(username, password, onSync)) {
            is Success -> { return MatrixRooms(msession = loginResult.value, message = "Logged in! Waiting on sync...") }
            is Error -> {
                return MatrixLogin(
                    login_message = "${loginResult.message} - exception was ${loginResult.cause}, please login again...\n",
                    mclient = mclient
                )
            }
        }
    }
    fun loginFromSession(username: String, onSync: () -> Unit): MatrixState {
        when (val loginResult = mclient.loginFromSavedSession(username, onSync)) {
            is Success -> { return MatrixRooms(msession = loginResult.value, message = "Logged in! Maybe try syncing?") }
            is Error -> {
                return MatrixLogin(
                    login_message = "${loginResult.message} - exception was ${loginResult.cause}, please login again...\n",
                    mclient = mclient
                )
            }
        }
    }

    fun getSessions(): List<String> {
        return mclient.getStoredSessions()
    }
}
data class SharedUiRoom(val id: String, val name: String, val unreadCount: Int, val highlightCount: Int, val lastMessage: SharedUiMessage?)
fun determineRoomName(room: Room, id: String): String {
    return room.state.events.firstStateEventContentOfType<RoomNameContent>()?.name
        ?: room.state.events.firstStateEventContentOfType<RoomCanonicalAliasContent>()?.alias
        ?: room.summary.heroes?.joinToString(", ")
        ?: "<no room name - $id>"
}
class MatrixRooms(private val msession: MatrixSession, val message: String) : MatrixState() {
    val rooms: List<SharedUiRoom> = msession.mapRooms { id, room ->
        SharedUiRoom(
            id,
            determineRoomName(room, id),
            room.unread_notifications?.notification_count ?: 0,
            room.unread_notifications?.highlight_count ?: 0,
            room.timeline.events.findLast { it as? RoomMessageEvent != null }?.let {
                val it = it as RoomMessageEvent
                SharedUiMessage(it.sender, it.content.body, it.event_id, it.origin_server_ts)
            }
        )
    }.sortedBy { -(it.lastMessage?.timestamp ?: 0) }
    override fun refresh(): MatrixState = MatrixRooms(
        msession = msession,
        message = "updated...\n"
    )
    fun getRoom(id: String): MatrixState {
        return MatrixChatRoom(
            msession,
            id,
            this.rooms.find({ room -> room.id == id })!!.name
        )
    }
    fun fake_logout(): MatrixState {
        msession.closeSession()
        return MatrixLogin("Closing session, returning to the login prompt for now\n", MatrixClient())
    }
}
open class SharedUiMessage(
    open val sender: String,
    open val message: String,
    open val id: String,
    open val timestamp: Long,
)
class SharedUiImgMessage(
    override val sender: String,
    override val message: String,
    override val id: String,
    override val timestamp: Long,
    val url: String
) : SharedUiMessage(sender,message,id,timestamp)
class SharedUiAudioMessage(
    override val sender: String,
    override val message: String,
    override val id: String,
    override val timestamp: Long,
    val url: String
) : SharedUiMessage(sender,message,id,timestamp)

class MatrixChatRoom(private val msession: MatrixSession, val room_id: String, val name: String) : MatrixState() {
    val messages: List<SharedUiMessage> = msession.getRoomEvents(room_id).map {
        if (it as? RoomMessageEvent != null) {
            val msg_content = it.content
            var generate_media_msg = { url: String, func: (String,String,String,Long,String) -> SharedUiMessage ->
                when (val url_local = msession.getLocalMediaPathFromUrl(url)) {
                    is Success -> {
                        func(
                            it.sender, it.content.body, it.event_id,
                            it.origin_server_ts, url_local.value
                        )
                    }
                    is Error -> {
                        SharedUiMessage(
                            it.sender, "Failed to load media ${url}",
                            it.event_id, it.origin_server_ts
                        )
                    }
                }
            }
            when (msg_content) {
                is TextRMEC -> SharedUiMessage(it.sender, it.content.body, it.event_id, it.origin_server_ts)
                is ImageRMEC -> generate_media_msg(msg_content.url, ::SharedUiImgMessage)
                is AudioRMEC -> generate_media_msg(msg_content.url, ::SharedUiAudioMessage)
                else -> SharedUiMessage(it.sender, "UNHANDLED EVENT!!! ${it.content.body}", it.event_id, it.origin_server_ts)
            }
        } else { null }
    }.filterNotNull()
    fun sendMessage(msg: String): MatrixState {
        when (val sendMessageResult = msession.sendMessage(msg, room_id)) {
            is Success -> { println("${sendMessageResult.value}") }
            is Error -> { println("${sendMessageResult.message} - exception was ${sendMessageResult.cause}") }
        }
        return this
    }
    fun sendImageMessage(msg: String): MatrixState {
        when (val sendMessageResult = msession.sendImageMessage(msg, room_id)) {
            is Success -> { println("${sendMessageResult.value}") }
            is Error -> { println("${sendMessageResult.message} - exception was ${sendMessageResult.cause}") }
        }
        return this
    }
    fun requestBackfill() {
        msession.requestBackfill(room_id)
    }
    fun sendReceipt(eventID: String) {
        when (val readReceiptResult = msession.sendReadReceipt(eventID, room_id)){
            is Success -> println("read receipt sent")
            is Error -> println("read receipt failed because ${readReceiptResult.cause}")
        }
    }
    override fun refresh(): MatrixState = MatrixChatRoom(
        msession,
        room_id,
        msession.mapRoom(room_id, { determineRoomName(it, room_id) }) ?: "<room gone?>"
    )
    fun exitRoom(): MatrixState {
        return MatrixRooms(msession, "Back to rooms.")
    }
}
