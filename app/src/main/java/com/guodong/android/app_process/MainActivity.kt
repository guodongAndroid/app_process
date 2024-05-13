package com.guodong.android.app_process

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.guodong.android.app_process.databinding.ActivityMainBinding
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Created by john.wick on 2024/5/13 10:15.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        binding.initView()
    }

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
                    runOnUiThread { Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }

}