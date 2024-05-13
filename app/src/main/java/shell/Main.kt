package shell

import android.os.IBinder
import android.os.Looper
import android.view.SurfaceControl
import java.net.ServerSocket
import kotlin.concurrent.thread

/**
 * Created by john.wick on 2024/5/11 16:14.
 */
fun main() {

    thread {
        println("Server stated")

        val serverSocket = ServerSocket(7890)

        while (true) {
            val socket = serverSocket.accept()
            println("Receive client: ${socket.inetAddress.hostAddress}:${socket.port}")

            socket.getInputStream().bufferedReader().use { br ->
                val line = br.readLine()
                println("Main: $line")

                displayPowerMode()
            }
        }
    }

    Looper.prepare()
    Looper.loop()
}

private fun displayPowerMode() {
    val surfaceControlClass = SurfaceControl::class.java

    val getInternalDisplayTokenMethod =
        surfaceControlClass.getDeclaredMethod("getInternalDisplayToken")
    getInternalDisplayTokenMethod.isAccessible = true
    val token = getInternalDisplayTokenMethod.invoke(null)

    val method = surfaceControlClass.getDeclaredMethod(
        "setDisplayPowerMode",
        IBinder::class.java,
        Int::class.java
    )
    method.isAccessible = true

    method.invoke(null, token, 0)
}