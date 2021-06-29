package com.example.mqtt_testapp

import android.database.DataSetObserver
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import timber.log.Timber


class MainActivity : AppCompatActivity() {
    companion object {
        const val SERVER_URL = "tcp://test.mosquitto.org:1883"
        const val TOPIC_NAME = "DH_MQTT_Test"

        const val OPTION_INTERVAL_START = "[Op01] Interval Start"
        const val OPTION_INTERVAL_END = "[Op02] Interval End"

        const val MESSAGE_SERVER = "[Server]"
        const val MESSAGE_CLIENT = "[Client]"

        const val TAG_HEADER = "MQTT_TEST"
        const val TAG_SYSTEM = "[System]"
        const val TAG_MESSAGE = "[Message]"

        const val CONNECTION_SUCCESS = "Connection success"
        const val CONNECTION_FAIL = "Connection fail"
    }

    private lateinit var mqttAndroidClient: MqttAndroidClient
    private lateinit var messageAdapter: MessageAdapter

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
//                setWill(TOPIC_NAME, "$TOPIC_NAME Open".toByteArray(), 1, true)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(Timber.DebugTree())

        setContentView(R.layout.activity_main)
        messageAdapter = MessageAdapter(this, R.layout.item_message)
        mqttAndroidClient = MqttAndroidClient(this, SERVER_URL, MqttClient.generateClientId())

        try {
            val token = mqttAndroidClient.connect(mqttConnectionOption)
            token.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions)
                    printMessage(TAG_SYSTEM, CONNECTION_SUCCESS)
                    try {
                        mqttAndroidClient.subscribe(TOPIC_NAME, 0)
                    } catch (e: MqttException) {
                        e.printStackTrace()
                    }
                }

                override fun onFailure(
                    asyncActionToken: IMqttToken,
                    exception: Throwable
                ) {
                    printMessage(TAG_SYSTEM, "$CONNECTION_FAIL $exception")
                }
            }

            mqttAndroidClient.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable) {}

                override fun messageArrived(topic: String, message: MqttMessage) {
                    printMessage("$TAG_MESSAGE($topic)", String(message.payload))
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

        findViewById<Button>(R.id.button).setOnClickListener {
            try {
                val et = findViewById<EditText>(R.id.editText)
                val textStr = et.text.trim().toString()

                if (textStr.isNotEmpty()) {
                    mqttAndroidClient.publish(
                        TOPIC_NAME,
                        "$MESSAGE_CLIENT $textStr".toByteArray(),
                        0,
                        false
                    )
                    et.setText("")
                }
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_interval, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_start -> {
                mqttAndroidClient.publish(
                    TOPIC_NAME,
                    OPTION_INTERVAL_START.toByteArray(),
                    0,
                    false
                )
                true
            }
            R.id.menu_end -> {
                mqttAndroidClient.publish(
                    TOPIC_NAME,
                    OPTION_INTERVAL_END.toByteArray(),
                    0,
                    false
                )
                true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onDestroy() {
        mqttAndroidClient.close()
        super.onDestroy()
    }

    private fun printMessage(tag: String, message: String) {
        Timber.e("$TAG_HEADER $tag $message")
        messageAdapter.add(message)
    }
}