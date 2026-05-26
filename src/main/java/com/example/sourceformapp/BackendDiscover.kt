package com.example.sourceformapp

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log

import org.json.JSONObject

import java.net.DatagramPacket
import java.net.DatagramSocket

object BackendDiscovery {

    fun discoverBackend(

        context: Context,

        onFound: (String) -> Unit

    ) {

        Thread {

            try {

                val wifi =

                    context.applicationContext
                        .getSystemService(
                            Context.WIFI_SERVICE
                        ) as WifiManager

                val lock =

                    wifi.createMulticastLock(
                        "sourceformLock"
                    )

                lock.acquire()

                val socket =
                    DatagramSocket(41234)

                socket.broadcast = true

                val buffer =
                    ByteArray(1024)

                val packet =
                    DatagramPacket(
                        buffer,
                        buffer.size
                    )

                Log.d(
                    "DISCOVERY",
                    "Listening..."
                )

                socket.receive(packet)

                Log.d(
                    "DISCOVERY",
                    "Packet Received"
                )

                val received =

                    String(
                        packet.data,
                        0,
                        packet.length
                    )

                Log.d(
                    "DISCOVERY",
                    received
                )

                val json =
                    JSONObject(received)

                val ip =
                    json.getString("ip")

                val port =
                    json.getInt("port")

                val backendUrl =

                    "http://$ip:$port/"

                onFound(backendUrl)

                socket.close()

                lock.release()

            } catch (e: Exception) {

                Log.e(
                    "DISCOVERY",
                    e.toString()
                )
            }

        }.start()
    }
}



