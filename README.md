探索 ARDC 「灭屏投屏」功能时，学习下 `app_process` 的提权操作。

`app_process` 是 Android 上的一个原生程序，是 APP 进程的主入口点。它可以让虚拟机从 `main()` 方法开始执行一个 Java 程序，**那么这个 Java 程序就会拥有一个较高的权限，从而可以执行一些特殊的操作**。

## 用法和参数

对于 `app_process` 的使用没有官方文档，但是 [程序源码](https://android.googlesource.com/platform/frameworks/base/+/android-10.0.0_r47/cmds/app_process/app_main.cpp#31) 里说得很清楚：

```
Usage: app_process [java-options] cmd-dir start-class-name [options]
```

[源码](https://android.googlesource.com/platform/frameworks/base/+/android-10.0.0_r47/cmds/app_process/app_main.cpp#191) 里也对参数做了说明：

```
java-options     - 传递给 JVM 的参数
cmd-dir          - 暂时没有用，可以传 /system/bin 或 / 都行
start-class-name - 程序入口， main() 方法所在类的全限定名
options          - 可以是下面这些
                    --zygote 启动 zygote 进程用的
                    --start-system-server 启动系统服务(也是启动 zygote 进程的时候用的)
                    --application 启动应用程序
                    --nice-name=启动之后的进程名称，方便查找
```

## CLASSPATH

与 Java 相似， Android 支持在环境变量 `CLASSPATH` 中指定类搜索路径，此外还可以在虚拟机参数中指定 `-Djava.class.path=xxx` 。但是， Android 使用 ART 环境运行 Java ，传统的 Java 字节码文件(.class) 是不能直接运行的，`app_process` 支持在 `CLASSPATH` 中指定 `dex` 或 `apk` 文件：

```
# 使用CLASSPATH & dex
export CLASSPATH=/data/local/tmp/test.dex
app_process /system/bin com.guodong.android.power.shell.MainKt

# 使用 -Djava.class.path 和 apk
app_process -Djava.class.path=/data/app/com.guodong.android.power-mWiGBpbL6vEE0QKJqfMgyw==/base.apk / com.guodong.android.power.shell.MainKt
```

## 服务端

```kotlin
//  Main.kt

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
```

## 启动服务端

编译并安装 APP 至手机。

```shell
adb shell

$ pm path com.guodong.android.power
package:/data/app/com.guodong.android.power-mWiGBpbL6vEE0QKJqfMgyw==/base.apk

$ app_process -Djava.class.path=/data/app/com.guodong.android.power-mWiGBpbL6vEE0QKJqfMgyw==/base.apk / com.guodong.android.power.shell.MainKt
```

`Main.kt` 被 `app_process` 执行启动后作为服务端接收客户端的连接，客户端发送任意消息被服务器接收到后就会执行 `displayPowerMode` 函数，函数内部就会反射调用 `SurfaceControl#setDisplayPowerMode` 方法，由于服务端由  `app_process` 启动，那么它就有很高的权限，所以反射可以执行成功。

## 客户端

```kotlin
private fun ActivityMainBinding.initView() {
    powerOff.setOnClickListener {
        thread {
            try {
                Socket("127.0.0.1", 7890).use {
                    it.getOutputStream().bufferedWriter().use { bw ->
                       bw.write("power off")
                    }
                }
            } catch (e: Exception) {
            }
        }
    }
}
```

客户端连接服务端后发送任意消息即可。