package com.example.mqtt_testapp

import android.database.DataSetObserver
import android.os.Bundle
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import timber.log.Timber


class MainActivity : AppCompatActivity() {
    companion object {
        const val TOPIC_NAME = "DH_Topic"
    }

    lateinit var messageAdapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(Timber.DebugTree())

        setContentView(R.layout.activity_main)
        messageAdapter = MessageAdapter(this, R.layout.item_message)

        val mqttAndroidClient = MqttAndroidClient(
            this,
            "tcp://test.mosquitto.org:1883",
            MqttClient.generateClientId()
        )

        try {
            val token = mqttAndroidClient.connect(mqttConnectionOption)
            token.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions)
                    printMessage("MQTT_Test Connect : Success")
                    try {
                        mqttAndroidClient.subscribe(TOPIC_NAME, 0)
//                        mqttAndroidClient.subscribe(TOPIC_NAME, 0) { topic, message ->
//                            val msg = String(message.payload)
//                            printMessage("MQTT Message callback ($topic) : $msg")
//                        }
                    } catch (e: MqttException) {
                        e.printStackTrace()
                    }
                }

                override fun onFailure(
                    asyncActionToken: IMqttToken,
                    exception: Throwable
                ) {
                    printMessage("MQTT_Test Connect : Fail\n$exception")
                }
            }

            mqttAndroidClient.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable) {}

                override fun messageArrived(topic: String, message: MqttMessage) {
                    val msg = String(message.payload)
                    printMessage("MQTT_Test Message ($topic) : $msg")
                }

                override fun deliveryComplete(token: IMqttDeliveryToken) {}
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }

        findViewById<ListView>(R.id.listView).apply {
            adapter = messageAdapter
            transcriptMode = ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL

            val listView = this
            messageAdapter.registerDataSetObserver(object : DataSetObserver() {
                override fun onChanged() {
                    super.onChanged()
                    listView.setSelection(messageAdapter.count - 1)
                }
            })
        }
    }

    private fun printMessage(message: String) {
        Timber.e(message)
        messageAdapter.add(message)
    }

    private val disconnectedBufferOptions: DisconnectedBufferOptions
        get() {
            return DisconnectedBufferOptions().apply {
                isBufferEnabled = true
                bufferSize = 100
                isPersistBuffer = true
                isDeleteOldestMessages = false
            }
        }
    private val mqttConnectionOption: MqttConnectOptions
        get() {
            return MqttConnectOptions().apply {
                isCleanSession = false
                isAutomaticReconnect = true
                setWill(TOPIC_NAME, "I am going offline".toByteArray(), 1, true)
            }
        }
}