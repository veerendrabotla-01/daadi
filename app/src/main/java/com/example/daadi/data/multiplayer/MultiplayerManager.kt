package com.example.daadi.data.multiplayer


import android.content.Context
import com.example.daadi.util.SecureLog as Log
import com.example.daadi.audio.SoundManager
import com.example.daadi.model.Player
import com.example.daadi.model.AIDifficulty
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import java.util.concurrent.TimeUnit
import java.util.UUID
import java.util.Collections
import java.util.LinkedHashSet

enum class MultiplayerStatus {
    DISCONNECTED,
    CONNECTING,
    LOBBY_WAITING,
    MATCHMAKING,
    CONNECTED,
    ERROR
}

data class ChatMessage(
    val sender: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class BotProfile(
    val name: String,
    val title: String,
    val rating: Int,
    val winRate: String,
    val statusText: String,
    val introMessage: String,
    val matchesPlayed: Int
)

val botProfiles = listOf(
    BotProfile("Grandmaster_Sage", "Grandmaster", 2850, "98%", "Online", "Let the battle of minds begin! I have been waiting for a worthy strategist.", 25000),
    BotProfile("Mystic_Weaver", "Master", 2420, "88%", "In Match", "Victory is woven from the threads of patience.", 8500),
    BotProfile("Shadow_Blade", "Elite", 2150, "78%", "Idle", "Strike from the shadows. Good luck!", 4200),
    BotProfile("Crimson_Viper", "Elite", 1980, "72%", "Active Now", "My defense is as sharp as a viper's fangs.", 3800),
    BotProfile("Storm_Bringer", "Tactician", 1850, "70%", "Online", "I bring the thunder to the Daadi arena!", 3100),
    BotProfile("Iron_Grip", "Expert", 1720, "65%", "In Match", "Can you break my legendary defense?", 2500),
    BotProfile("Silent_Owl", "Expert", 1680, "63%", "Idle", "Watching your every move. Ready?", 2200),
    BotProfile("Lunar_Wolf", "Warrior", 1590, "60%", "Active Now", "I play better under pressure. Let's go!", 1900),
    BotProfile("Ember_Phoenix", "Warrior", 1510, "58%", "Looking for Match", "Rising from the ashes of every defeat.", 1750),
    BotProfile("Swift_Falcon", "Adept", 1420, "55%", "Online", "Speed and strategy are my weapons.", 1500)
)

@JsonClass(generateAdapter = true)
data class MultiplayerMsg(
    val type: String, // "HOST", "JOIN", "START", "MOVE", "CHAT", "FORFEIT", "DRAW_OFFER", "DRAW_ACCEPT", "UNDO_REQ", "UNDO_ACK", "SYNC_REQ", "SYNC_ACK"
    val roomCode: String,
    val sender: String,
    val text: String? = null,
    val nodeId: Int? = null,
    val fromNodeId: Int? = null,
    val actionType: String? = null, // "PLACE", "MOVE", "CAPTURE"
    val actionStatus: Boolean? = null,
    val role: String? = null,
    val stateJson: String? = null, // For SYNC_ACK
    val msgId: String? = null, // For deduplication
    val seq: Int? = null, // Replay protection sequence
    val timestamp: Long? = null, // Expiry timestamp
    val signature: String? = null // Cryptographic checksum
)

class MultiplayerManager(
    private val context: Context,
    private val soundManager: SoundManager
) {
    private val tag = "MultiplayerManager"
    private val client = OkHttpClient.Builder()
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS) // Native heartbeat keepalive
        .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
        .build()

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val msgAdapter = moshi.adapter(MultiplayerMsg::class.java)

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val receivedMessageIds = Collections.synchronizedSet(LinkedHashSet<String>())
    private val maxSavedMessageIds = 200

    // Anti-Cheat and Infrastructure parameters
    private var localSequenceNumber = 0
    private val expectedRemoteSequences = java.util.concurrent.ConcurrentHashMap<String, Int>()
    
    private var lastRateLimitWindowTime = 0L
    private var messageCountInCurrentWindow = 0
    private val RATE_LIMIT_WINDOW_MS = 1000L
    private val MAX_MESSAGES_PER_WINDOW = 8 // Reject flood attempts
    private val PACKET_SIGNATURE_SECRET by lazy {
        try { com.example.BuildConfig.MULTIPLAYER_SIGNATURE_SECRET } catch (e: Exception) { "DaadiFallbackSecret" }
    }

    // State Flows
    private val _status = MutableStateFlow(MultiplayerStatus.DISCONNECTED)
    val status: StateFlow<MultiplayerStatus> = _status.asStateFlow()

    private val _matchmakingCountdown = MutableStateFlow<Int?>(null)
    val matchmakingCountdown: StateFlow<Int?> = _matchmakingCountdown.asStateFlow()

    private var isQuickMatch = false

    private val _reconnectionCountdown = MutableStateFlow<Int?>(null)
    val reconnectionCountdown: StateFlow<Int?> = _reconnectionCountdown.asStateFlow()

    private val _isLobbyEmpty = MutableStateFlow(false)
    val isLobbyEmpty: StateFlow<Boolean> = _isLobbyEmpty.asStateFlow()

    private val _roomCode = MutableStateFlow("")
    val roomCode: StateFlow<String> = _roomCode.asStateFlow()

    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()

    private val _localPlayerName = MutableStateFlow("King_" + (1000..9999).random())
    val localPlayerName: StateFlow<String> = _localPlayerName.asStateFlow()

    private val _opponentPlayerName = MutableStateFlow("")
    val opponentPlayerName: StateFlow<String> = _opponentPlayerName.asStateFlow()

    private val _opponentProfile = MutableStateFlow<BotProfile?>(null)
    val opponentProfile: StateFlow<BotProfile?> = _opponentProfile.asStateFlow()

    private val _localRole = MutableStateFlow(Player.PLAYER_1)
    val localRole: StateFlow<Player> = _localRole.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _reconnecting = MutableStateFlow(false)
    val reconnecting: StateFlow<Boolean> = _reconnecting.asStateFlow()

    // Handlers
    var onRemoteMoveReceived: ((nodeId: Int, fromNodeId: Int?, actionType: String) -> Unit)? = null
    var onRemoteDrawReceived: (() -> Unit)? = null
    var onRemoteDrawAccepted: (() -> Unit)? = null
    var onRemoteForfeitReceived: (() -> Unit)? = null
    var onRemoteUndoRequested: (() -> Unit)? = null
    var onRemoteUndoAcknowledged: ((accepted: Boolean) -> Unit)? = null
    var onGameStarted: (() -> Unit)? = null
    var onSyncRequested: (() -> String?)? = null
    var onSyncReceived: ((String) -> Unit)? = null
    var onConnectionRestored: (() -> Unit)? = null
    var onAbandonRequired: (() -> Unit)? = null
    var onSimulatedMoveNeeded: (() -> Unit)? = null

    // Fallback Simulator (If real websockets are rates-capped / offline)
    private var useSimulatorFallback = false
    private var simulatorJob: Job? = null
    var simulatorDifficulty = AIDifficulty.MEDIUM

    // PieSocket dedicated cluster websocket url
    private val webSocketUrl: String by lazy {
        val apiKey = try { com.example.BuildConfig.PIESOCKET_API_KEY } catch (e: Exception) { "" }
        val clusterId = try { com.example.BuildConfig.PIESOCKET_CLUSTER_ID } catch (e: Exception) { "free" }
        
        if (apiKey.isNotBlank() && apiKey != "PIESOCKET_API_KEY_PLACEHOLDER") {
            // Use dedicated cluster and v3/1 endpoint as requested
            "wss://$clusterId.piesocket.com/v3/1?api_key=$apiKey&notify_self=1"
        } else {
            // Public demo fallback
            "wss://free.piesocket.com/v3/demo?api_key=VCX6SpY8ZoZ6Gwz17K0QLW6v8b7X63007077&notify_self=0"
        }
    }

    fun setLocalPlayerName(name: String) {
        if (name.isNotBlank()) {
            _localPlayerName.value = name.trim()
        }
    }

    private fun connectWebSocket(onConnected: () -> Unit, onError: (String) -> Unit) {
        _errorMessage.value = null

        val request = Request.Builder().url(webSocketUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(tag, "WebSocket connection opened successfully.")
                scope.launch {
                    if (_reconnecting.value) {
                        _reconnecting.value = false
                        _status.value = MultiplayerStatus.CONNECTED
                        sendSyncRequest()
                        onConnectionRestored?.invoke()
                    }
                    onConnected()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(tag, "Message received: $text")
                handleIncomingMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(tag, "WebSocket failure: ${t.message}")
                scope.launch {
                    if (_status.value == MultiplayerStatus.CONNECTING) {
                        activateSimulatorFallback()
                    } else if (_status.value == MultiplayerStatus.CONNECTED || _status.value == MultiplayerStatus.LOBBY_WAITING) {
                        startReconnectionGracePeriod()
                    } else {
                        _status.value = MultiplayerStatus.ERROR
                        _errorMessage.value = "Connection severed: ${t.localizedMessage ?: "Network Timeout"}"
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "WebSocket closing: $reason ($code)")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "WebSocket closed.")
                if (!_reconnecting.value) {
                    _status.value = MultiplayerStatus.DISCONNECTED
                }
            }
        })
    }

    private fun startReconnectionGracePeriod() {
        if (_reconnecting.value) return
        _reconnecting.value = true
        
        scope.launch {
            var delayMs = 1000L
            val maxDelayMs = 16000L
            val startSec = 30
            var elapsedSec = 0
            
            _reconnectionCountdown.value = startSec
            
            while (_reconnecting.value && elapsedSec < startSec) {
                // Safely close the old socket to prevent connection leaks
                try {
                    webSocket?.close(1000, "Reconnecting")
                } catch (e: Exception) {}
                webSocket = null
                
                Log.d(tag, "Attempting reconnect in ${delayMs}ms...")
                
                val request = Request.Builder().url(webSocketUrl).build()
                webSocket = client.newWebSocket(request, this@MultiplayerManager.webSocketListener)
                
                val stepDelay = 1000L
                val steps = (delayMs / stepDelay).coerceAtLeast(1)
                for (step in 1..steps) {
                    if (!_reconnecting.value) break
                    delay(stepDelay)
                    elapsedSec++
                    _reconnectionCountdown.value = (startSec - elapsedSec).coerceAtLeast(0)
                }
                
                // Exponential backoff with random jitter
                delayMs = (delayMs * 2).coerceAtMost(maxDelayMs)
                val jitter = (Math.random() * 500).toLong()
                delayMs += jitter
            }
            
            _reconnectionCountdown.value = null
            if (_reconnecting.value) {
                _reconnecting.value = false
                _status.value = MultiplayerStatus.DISCONNECTED
                onAbandonRequired?.invoke()
            }
        }
    }

    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            scope.launch {
                _reconnecting.value = false
                _status.value = MultiplayerStatus.CONNECTED
                sendSyncRequest()
                onConnectionRestored?.invoke()
            }
        }
        override fun onMessage(webSocket: WebSocket, text: String) = handleIncomingMessage(text)
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
             // Handled by outer loop
        }
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val currentTime = System.currentTimeMillis()
            
            // 1. Rate Limiting (WebSocket Flooding protection)
            synchronized(this) {
                if (currentTime - lastRateLimitWindowTime > RATE_LIMIT_WINDOW_MS) {
                    lastRateLimitWindowTime = currentTime
                    messageCountInCurrentWindow = 0
                }
                messageCountInCurrentWindow++
                if (messageCountInCurrentWindow > MAX_MESSAGES_PER_WINDOW) {
                    Log.e(tag, "Security Alert: WebSocket FLOODING detected. Excess message rate dropped.")
                    return
                }
            }

            val msg = msgAdapter.fromJson(text) ?: return
            if (msg.roomCode != _roomCode.value) return
            if (msg.sender == _localPlayerName.value) return

            // 2. Cryptographic Signature Validation (Anti-tampering & packet injection protection)
            val receivedSig = msg.signature
            if (receivedSig != null) {
                val raw = "${msg.roomCode}:${msg.sender}:${msg.type}:${msg.nodeId ?: 0}:${msg.fromNodeId ?: 0}:${msg.seq ?: 0}:${msg.timestamp ?: 0}:$PACKET_SIGNATURE_SECRET"
                val calculatedSig = try {
                    val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
                    bytes.joinToString("") { "%02x".format(it) }
                } catch (e: Exception) { "" }
                
                if (receivedSig != calculatedSig) {
                    Log.e(tag, "Security Alert: PACKET TAMPERING detected from ${msg.sender}! Invalid signature rejected.")
                    _errorMessage.value = "Security Error: Blocked corrupted/tampered packet."
                    com.example.daadi.DaadiApplication.instance.remoteGameRepository.logAntiCheatViolation(
                        matchId = _roomCode.value,
                        violationType = "PACKET_TAMPERING",
                        severity = "critical",
                        details = "Calculated: $calculatedSig, Received: $receivedSig"
                    )
                    return
                }
            }

            // 3. Replay attack & Out-of-order protection (Sequence Counter checks)
            val remoteSeq = msg.seq
            if (remoteSeq != null) {
                val lastSeq = expectedRemoteSequences[msg.sender] ?: 0
                if (remoteSeq <= lastSeq) {
                    Log.e(tag, "Security Alert: REPLAY ATTACK intercepted from ${msg.sender}. Last Seq: $lastSeq, Received Seq: $remoteSeq")
                    com.example.daadi.DaadiApplication.instance.remoteGameRepository.logAntiCheatViolation(
                        matchId = _roomCode.value,
                        violationType = "REPLAY_ATTACK",
                        severity = "high",
                        details = "Last Seq: $lastSeq, Received Seq: $remoteSeq"
                    )
                    return
                }
                expectedRemoteSequences[msg.sender] = remoteSeq
            }

            // 4. Clock Cheating & Packet Expiry checks (Timestamp validation)
            val remoteTs = msg.timestamp
            if (remoteTs != null) {
                val drift = Math.abs(currentTime - remoteTs)
                if (drift > 35000) { // 35 seconds threshold
                    Log.e(tag, "Security Alert: CLOCK CHEATING/EXPIRED packet from ${msg.sender}. Drift: ${drift}ms")
                    _errorMessage.value = "Security warning: Dismissed out-of-time request."
                    com.example.daadi.DaadiApplication.instance.remoteGameRepository.logAntiCheatViolation(
                        matchId = _roomCode.value,
                        violationType = "CLOCK_CHEATING",
                        severity = "medium",
                        details = "Drift: ${drift}ms"
                    )
                    return
                }
            }

            // 5. Deduplication (msgId)
            val mId = msg.msgId
            if (mId != null) {
                synchronized(receivedMessageIds) {
                    if (receivedMessageIds.contains(mId)) {
                        Log.d(tag, "Discarding duplicate WebSocket message with ID: $mId")
                        return
                    }
                    receivedMessageIds.add(mId)
                    if (receivedMessageIds.size > maxSavedMessageIds) {
                        val iterator = receivedMessageIds.iterator()
                        if (iterator.hasNext()) {
                            iterator.next()
                            iterator.remove()
                        }
                    }
                }
            }

            scope.launch {
                when (msg.type) {
                    "HOST" -> {}
                    "JOIN" -> {
                        if (_isHost.value && (_status.value == MultiplayerStatus.LOBBY_WAITING || _reconnecting.value)) {
                            _opponentPlayerName.value = msg.sender
                            _status.value = MultiplayerStatus.CONNECTED
                            _reconnecting.value = false
                            soundManager.playConnect()
                            sendMsg(MultiplayerMsg(type = "START", roomCode = _roomCode.value, sender = _localPlayerName.value, text = _opponentPlayerName.value, role = "PLAYER_1"))
                            onGameStarted?.invoke()
                        }
                    }
                    "START" -> {
                        if (!_isHost.value && (_status.value == MultiplayerStatus.CONNECTING || _reconnecting.value)) {
                            _opponentPlayerName.value = msg.sender
                            _status.value = MultiplayerStatus.CONNECTED
                            _reconnecting.value = false
                            _localRole.value = Player.PLAYER_2
                            soundManager.playConnect()
                            onGameStarted?.invoke()
                        }
                    }
                    "LEAVE" -> {
                        _status.value = MultiplayerStatus.DISCONNECTED
                        _errorMessage.value = "${msg.sender} has left the arena."
                        onAbandonRequired?.invoke()
                    }
                    "MOVE" -> {
                        if (msg.nodeId != null && msg.actionType != null) {
                            if (msg.nodeId in 0..23 && (msg.fromNodeId == null || msg.fromNodeId in 0..23)) {
                                onRemoteMoveReceived?.invoke(msg.nodeId, msg.fromNodeId, msg.actionType)
                            }
                        }
                    }
                    "CHAT" -> {
                        if (msg.text != null) {
                            val sanitizedText = msg.text.take(150).replace("<", "&lt;").replace(">", "&gt;")
                            addChatMessage(msg.sender, sanitizedText)
                        }
                    }
                    "FORFEIT" -> onRemoteForfeitReceived?.invoke()
                    "DRAW_OFFER" -> onRemoteDrawReceived?.invoke()
                    "DRAW_ACCEPT" -> onRemoteDrawAccepted?.invoke()
                    "UNDO_REQ" -> onRemoteUndoRequested?.invoke()
                    "UNDO_ACK" -> if (msg.actionStatus != null) onRemoteUndoAcknowledged?.invoke(msg.actionStatus)
                    "SYNC_REQ" -> {
                        val stateJson = onSyncRequested?.invoke()
                        if (stateJson != null) {
                            sendMsg(MultiplayerMsg(type = "SYNC_ACK", roomCode = _roomCode.value, sender = _localPlayerName.value, stateJson = stateJson))
                        }
                    }
                    "SYNC_ACK" -> {
                        if (msg.stateJson != null) onSyncReceived?.invoke(msg.stateJson)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendSyncRequest() {
        sendMsg(MultiplayerMsg(type = "SYNC_REQ", roomCode = _roomCode.value, sender = _localPlayerName.value))
    }

    private fun sendMsg(msg: MultiplayerMsg) {
        if (useSimulatorFallback) {
            processSimulatorOutgoing(msg)
            return
        }
        val socket = webSocket
        if (socket != null && _status.value != MultiplayerStatus.DISCONNECTED) {
            try {
                // Populate sequence, timestamp, unique msg ID, and SHA-256 HMAC integrity signature
                val nextSeq = ++localSequenceNumber
                val currentTs = System.currentTimeMillis()
                val mId = msg.msgId ?: UUID.randomUUID().toString()
                
                val raw = "${msg.roomCode}:${msg.sender}:${msg.type}:${msg.nodeId ?: 0}:${msg.fromNodeId ?: 0}:$nextSeq:$currentTs:$PACKET_SIGNATURE_SECRET"
                val calculatedSig = try {
                    val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
                    bytes.joinToString("") { "%02x".format(it) }
                } catch (e: Exception) {
                    ""
                }
                
                val securedMsg = msg.copy(
                    msgId = mId,
                    seq = nextSeq,
                    timestamp = currentTs,
                    signature = calculatedSig
                )
                
                val json = msgAdapter.toJson(securedMsg)
                socket.send(json)
                Log.d(tag, "Secured Outgoing message sent: $json")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun hostRoom(customCode: String? = null) {
        val code = customCode ?: (100000..999999).random().toString()
        _roomCode.value = code
        _isHost.value = true
        _localRole.value = Player.PLAYER_1
        _opponentPlayerName.value = ""
        _chatMessages.value = emptyList()
        useSimulatorFallback = false

        connectWebSocket(
            onConnected = {
                _status.value = MultiplayerStatus.LOBBY_WAITING
                sendMsg(MultiplayerMsg(type = "HOST", roomCode = code, sender = _localPlayerName.value))
            },
            onError = { err ->
                _status.value = MultiplayerStatus.ERROR
                _errorMessage.value = err
            }
        )
    }

    fun joinRoom(code: String) {
        if (code.length != 6) {
            _errorMessage.value = "Room code must be exactly 6 digits!"
            return
        }
        _roomCode.value = code
        _isHost.value = false
        _localRole.value = Player.PLAYER_2
        _opponentPlayerName.value = ""
        _chatMessages.value = emptyList()
        useSimulatorFallback = false

        connectWebSocket(
            onConnected = {
                sendMsg(MultiplayerMsg(type = "JOIN", roomCode = code, sender = _localPlayerName.value))
                scope.launch {
                    delay(5000)
                    if (_status.value == MultiplayerStatus.CONNECTING) {
                        _status.value = MultiplayerStatus.ERROR
                        _errorMessage.value = "Room #$code not found or host is offline. Verify the PIN."
                    }
                }
            },
            onError = { err ->
                _status.value = MultiplayerStatus.ERROR
                _errorMessage.value = "Connection failed: $err"
            }
        )
    }

    fun quickMatch(onFindMatch: (onJoin: (String) -> Unit, onHost: (String) -> Unit) -> Unit) {
        _status.value = MultiplayerStatus.MATCHMAKING
        _chatMessages.value = emptyList()
        _errorMessage.value = null
        isQuickMatch = true
        _isLobbyEmpty.value = false
        
        scope.launch {
            delay(1000)
            onFindMatch(
                { foundRoomCode -> joinRoom(foundRoomCode) },
                { newRoomCode ->
                    hostRoom(newRoomCode)
                    scope.launch {
                        val searchTime = (3..5).random()
                        for (i in searchTime downTo 1) {
                            if (_status.value != MultiplayerStatus.LOBBY_WAITING || _opponentPlayerName.value.isNotEmpty()) break
                            _matchmakingCountdown.value = i
                            delay(1000)
                        }
                        _matchmakingCountdown.value = null
                        if (_status.value == MultiplayerStatus.LOBBY_WAITING && _opponentPlayerName.value.isEmpty()) {
                            activateSimulatorFallback()
                        }
                    }
                }
            )
        }
    }

    fun startSimulatorNow() {
        if (_status.value == MultiplayerStatus.LOBBY_WAITING && isQuickMatch) {
            _matchmakingCountdown.value = null
            activateSimulatorFallback()
        }
    }

    fun setLobbyEmpty(isEmpty: Boolean) {
        _isLobbyEmpty.value = isEmpty
    }

    fun sendMove(nodeId: Int, fromNodeId: Int?, actionType: String) {
        sendMsg(MultiplayerMsg(type = "MOVE", roomCode = _roomCode.value, sender = _localPlayerName.value, nodeId = nodeId, fromNodeId = fromNodeId, actionType = actionType))
    }

    fun sendChat(text: String) {
        if (text.isBlank()) return
        addChatMessage(_localPlayerName.value, text)
        sendMsg(MultiplayerMsg(type = "CHAT", roomCode = _roomCode.value, sender = _localPlayerName.value, text = text))
    }

    fun sendForfeit() {
        sendMsg(MultiplayerMsg(type = "FORFEIT", roomCode = _roomCode.value, sender = _localPlayerName.value))
        disconnect()
    }

    fun sendDrawOffer() {
        sendMsg(MultiplayerMsg(type = "DRAW_OFFER", roomCode = _roomCode.value, sender = _localPlayerName.value))
    }

    fun sendDrawAccept() {
        sendMsg(MultiplayerMsg(type = "DRAW_ACCEPT", roomCode = _roomCode.value, sender = _localPlayerName.value))
        disconnect()
    }

    fun sendUndoRequest() {
        sendMsg(MultiplayerMsg(type = "UNDO_REQ", roomCode = _roomCode.value, sender = _localPlayerName.value))
    }

    fun sendUndoResponse(accepted: Boolean) {
        sendMsg(MultiplayerMsg(type = "UNDO_ACK", roomCode = _roomCode.value, sender = _localPlayerName.value, actionStatus = accepted))
    }

    private fun addChatMessage(sender: String, text: String) {
        val list = _chatMessages.value.toMutableList()
        list.add(ChatMessage(sender, text))
        _chatMessages.value = list
    }

    fun disconnect() {
        if (_status.value == MultiplayerStatus.CONNECTED) {
            sendMsg(MultiplayerMsg(type = "LEAVE", roomCode = _roomCode.value, sender = _localPlayerName.value))
        }
        webSocket?.close(1000, "User left")
        webSocket = null
        _status.value = MultiplayerStatus.DISCONNECTED
        _roomCode.value = ""
        _isHost.value = false
        _opponentPlayerName.value = ""
        _opponentProfile.value = null
        _chatMessages.value = emptyList()
        _reconnecting.value = false
        _isLobbyEmpty.value = false
        simulatorJob?.cancel()
        useSimulatorFallback = false
    }

    private fun activateSimulatorFallback() {
        Log.d(tag, "Activating advanced Network Simulator fallback.")
        useSimulatorFallback = true
        simulatorDifficulty = listOf(AIDifficulty.EASY, AIDifficulty.MEDIUM, AIDifficulty.HARD).random()
        setupSimulatedMatch()
    }

    private fun setupSimulatedMatch() {
        _status.value = MultiplayerStatus.CONNECTED
        val selected = botProfiles.random()
        _opponentPlayerName.value = selected.name
        _opponentProfile.value = selected
        soundManager.playConnect()
        onGameStarted?.invoke()

        scope.launch {
            delay(1500)
            addChatMessage(_opponentPlayerName.value, selected.introMessage)
        }
    }

    private fun processSimulatorOutgoing(msg: MultiplayerMsg) {
        when (msg.type) {
            "MOVE" -> triggerSimulatorOpponentAction()
            "CHAT" -> {
                if ((1..100).random() < 50) {
                    scope.launch {
                        delay((1500..4000).random().toLong())
                        val replies = listOf("Good move!", "Impressive play.", "You are a worthy opponent.", "I must rethink my strategy.", "Close game!")
                        addChatMessage(_opponentPlayerName.value, replies.random())
                    }
                }
            }
            "DRAW_OFFER" -> {
                scope.launch {
                    delay(2500)
                    if ((1..100).random() > 50) {
                        addChatMessage(_opponentPlayerName.value, "Handshake accepted. Peace on the battlefield.")
                        onRemoteDrawAccepted?.invoke()
                    } else {
                        addChatMessage(_opponentPlayerName.value, "I fight on! No draw today.")
                    }
                }
            }
            "UNDO_REQ" -> {
                scope.launch {
                    delay(1200)
                    onRemoteUndoAcknowledged?.invoke(true)
                    addChatMessage(_opponentPlayerName.value, "Granted. Correct your path.")
                }
            }
        }
    }

    private fun triggerSimulatorOpponentAction() {
        simulatorJob?.cancel()
        simulatorJob = scope.launch {
            val latency = (1000L..2500L).random()
            delay(latency)
            onSimulatedMoveNeeded?.invoke()
        }
    }
}
