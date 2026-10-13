package com.hdlee73.dailyhabit

import com.hdlee73.dailyhabit.data.AppDirs
import java.io.File
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileLock

/** 앱을 하나만 띄운다. 두 번째로 실행하면 먼저 켜진 앱의 창을 연다 */
object SingleInstance {
    private var lock: FileLock? = null
    var onShowRequest: (() -> Unit)? = null

    fun acquire(): Boolean {
        val portFile = File(AppDirs.root, "instance.port")
        val lockFile = RandomAccessFile(File(AppDirs.root, "instance.lock"), "rw")
        lock = runCatching { lockFile.channel.tryLock() }.getOrNull()
        if (lock == null) {
            // 이미 실행 중: 창을 열어 달라고 알린다
            runCatching {
                val port = portFile.readText().trim().toInt()
                Socket(InetAddress.getLoopbackAddress(), port).use { it.getOutputStream().write("show\n".toByteArray()) }
            }
            return false
        }
        runCatching {
            val server = ServerSocket(0, 5, InetAddress.getLoopbackAddress())
            portFile.writeText(server.localPort.toString())
            Thread {
                while (true) {
                    runCatching {
                        server.accept().use { s ->
                            if (s.getInputStream().bufferedReader().readLine() == "show") {
                                javax.swing.SwingUtilities.invokeLater { onShowRequest?.invoke() }
                            }
                        }
                    }
                }
            }.apply { isDaemon = true }.start()
        }
        return true
    }
}
