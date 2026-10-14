package com.hdlee73.dailyhabit.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hdlee73.dailyhabit.BuildConfig
import com.hdlee73.dailyhabit.sync.DeviceStore
import com.hdlee73.dailyhabit.sync.PairedDevice
import com.hdlee73.dailyhabit.sync.PairingCode
import com.hdlee73.dailyhabit.sync.PhotoStore
import com.hdlee73.dailyhabit.sync.SyncHttpServer
import com.hdlee73.dailyhabit.sync.SyncProtocol
import com.hdlee73.dailyhabit.sync.SyncServerCore
import com.hdlee73.dailyhabit.sync.SyncDb
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

/**
 * 휴대폰과 같은 와이파이에서 직접 동기화 (PC 쪽).
 *
 * PC 앱이 작은 서버를 열고, 휴대폰이 PC를 찾아 와서 변경된 기록만 주고받는다. 중간 서버나 계정은 없다.
 * 짝 맺은 휴대폰 목록과 비밀 키는 앱 데이터 폴더의 sync.json에 둔다.
 */
object PcSync {
    private val file = File(AppDirs.root, "sync.json")
    private var json: JSONObject = runCatching { JSONObject(file.readText(Charsets.UTF_8)) }.getOrDefault(JSONObject())

    /** 화면에 보이는 상태 */
    var running by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var devices by mutableStateOf(loadDevices())
        private set
    /** 짝 맺기 QR을 띄운 동안의 코드 */
    var pairing by mutableStateOf<PairingCode?>(null)
        private set
    /** 방금 짝을 맺은 휴대폰 이름 (화면이 알림을 띄우고 지운다) */
    var justPaired by mutableStateOf<String?>(null)

    private var pendingKey: ByteArray? = null
    private var pendingUntil = 0L
    private var server: SyncHttpServer? = null
    private val mdns = ArrayList<JmDNS>()

    @Synchronized
    private fun saveJson() {
        runCatching { file.writeText(json.toString(2), Charsets.UTF_8) }
    }

    val serverId: String
        get() = json.optString("serverId").ifBlank { SyncProtocol.newId().also { json.put("serverId", it); saveJson() } }

    private val port: Int get() = json.optInt("port", SyncProtocol.DEFAULT_PORT)

    /** 동기화를 켜 두었는지 (휴대폰과 한 번이라도 짝을 맺으면 켜진다) */
    var enabled: Boolean
        get() = json.optBoolean("enabled", false)
        set(v) { json.put("enabled", v); saveJson() }

    private val pcName: String
        get() = (System.getenv("COMPUTERNAME") ?: runCatching { InetAddress.getLocalHost().hostName }.getOrNull() ?: "PC").take(40)

    private fun loadDevices(): List<PairedDevice> {
        val a = json.optJSONArray("devices") ?: return emptyList()
        return List(a.length()) { a.getJSONObject(it) }.mapNotNull { o ->
            runCatching {
                PairedDevice(o.getString("id"), o.optString("name", "휴대폰"), SyncProtocol.unb64(o.getString("key")), o.optLong("pairedAt"), o.optLong("lastSync"))
            }.getOrNull()
        }
    }

    @Synchronized
    private fun storeDevices(list: List<PairedDevice>) {
        json.put("devices", JSONArray(list.map {
            JSONObject().put("id", it.id).put("name", it.name).put("key", SyncProtocol.b64(it.key)).put("pairedAt", it.pairedAt).put("lastSync", it.lastSync)
        }))
        saveJson()
        devices = list
    }

    private val deviceStore = object : DeviceStore {
        override fun find(id: String) = devices.firstOrNull { it.id == id }
        override fun save(device: PairedDevice) = storeDevices(devices.filter { it.id != device.id } + device)
    }

    private val photoStore = object : PhotoStore {
        private fun f(name: String) = File(File(AppDirs.root, "photos").apply { mkdirs() }, name)
        override fun has(name: String) = f(name).exists()
        override fun read(name: String) = f(name).takeIf { it.exists() }?.readBytes()
        override fun write(name: String, bytes: ByteArray) {
            val tmp = f("$name.part")
            tmp.writeBytes(bytes)
            tmp.renameTo(f(name))
        }
    }

    /** 이 PC의 와이파이·유선 랜 주소 (가상 어댑터 제외) */
    fun localAddresses(): List<String> = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback && !it.isVirtual && !it.isPointToPoint }
            .filterNot { n -> listOf("vmware", "virtualbox", "hyper-v", "vethernet", "wsl", "docker", "loopback", "bluetooth").any { n.displayName.lowercase().contains(it) } }
            .flatMap { n -> n.inetAddresses.toList().filterIsInstance<Inet4Address>().filter { it.isSiteLocalAddress }.map { it.hostAddress } }
            .distinct()
    }.getOrDefault(emptyList())

    /** 앱을 켤 때: 짝 맺은 휴대폰이 있으면 서버를 연다 */
    fun startIfEnabled() {
        if (enabled && devices.isNotEmpty()) start()
    }

    @Synchronized
    fun start() {
        if (server != null) return
        error = null
        val core = SyncServerCore(
            db = ConnectionDb,
            edition = SyncProtocol.edition(BuildConfig.CATHOLIC),
            serverId = serverId,
            serverName = pcName,
            devices = deviceStore,
            photos = photoStore,
            pendingKey = { pendingKey?.takeIf { System.currentTimeMillis() < pendingUntil } },
            onPaired = { d ->
                pendingKey = null
                pairing = null
                enabled = true
                justPaired = d.name
            },
        )
        val s = runCatching { SyncHttpServer(core, port).apply { start() } }.recoverCatching {
            // 포트를 다른 프로그램이 쓰고 있으면 빈 포트로
            SyncHttpServer(core, 0).apply { start() }.also { json.put("port", it.boundPort); saveJson() }
        }.getOrElse {
            error = "동기화 서버를 열지 못했어요 (${it.message})"
            return
        }
        server = s
        running = true
        Thread({ registerMdns(s.boundPort) }, "sync-mdns").apply { isDaemon = true }.start()
    }

    private fun registerMdns(port: Int) {
        for (addr in localAddresses()) {
            runCatching {
                val j = JmDNS.create(InetAddress.getByName(addr), "DailyHabit-$serverId")
                j.registerService(
                    ServiceInfo.create(
                        "${SyncProtocol.SERVICE_TYPE}.local.", "DH-$serverId", port, 0, 0,
                        mapOf("id" to serverId, "ed" to SyncProtocol.edition(BuildConfig.CATHOLIC)),
                    )
                )
                synchronized(mdns) { mdns += j }
            }
        }
    }

    @Synchronized
    fun stop() {
        server?.stop()
        server = null
        running = false
        val list = synchronized(mdns) { mdns.toList().also { mdns.clear() } }
        Thread({ list.forEach { runCatching { it.close() } } }, "sync-mdns-close").apply { isDaemon = true }.start()
    }

    /** 휴대폰으로 찍을 짝 맺기 QR을 만든다 (10분 동안 유효) */
    fun beginPairing(): PairingCode? {
        start()
        val s = server ?: return null
        val key = SyncProtocol.newKey()
        pendingKey = key
        pendingUntil = System.currentTimeMillis() + 10 * 60 * 1000L
        return PairingCode(serverId, SyncProtocol.edition(BuildConfig.CATHOLIC), localAddresses(), s.boundPort, key, pcName)
            .also { pairing = it }
    }

    fun cancelPairing() {
        pendingKey = null
        pairing = null
        if (devices.isEmpty()) stop()
    }

    fun unpair(id: String) {
        storeDevices(devices.filter { it.id != id })
        if (devices.isEmpty()) { enabled = false; stop() }
    }

    /** 동기화를 끈다 (짝 맺은 휴대폰 목록은 남긴다) */
    fun turnOff() {
        enabled = false
        stop()
    }

    fun turnOn() {
        enabled = true
        start()
    }

    /** 요청마다 새 SQLite 연결 하나를 열어 트랜잭션 동안 쓴다 (윈도우 앱의 다른 DAO와 같은 방식). 끝나면 화면을 새로 고친다 */
    private object ConnectionDb : SyncDb {
        private val current = ThreadLocal<SyncDb?>()
        override fun query(sql: String, vararg args: Any?): List<Map<String, Any?>> = current.get()?.query(sql, *args) ?: tx { query(sql, *args) }
        override fun exec(sql: String, vararg args: Any?) { current.get()?.exec(sql, *args) ?: tx { exec(sql, *args) } }
        override fun <T> tx(block: () -> T): T {
            current.get()?.let { return it.tx(block) }
            return AppDatabase.get().withSyncDb { inner ->
                current.set(inner)
                try { inner.tx(block) } finally { current.remove() }
            }
        }
    }
}
