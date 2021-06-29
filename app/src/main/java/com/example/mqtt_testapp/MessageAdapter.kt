package com.example.mqtt_testapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.mqtt_testapp.MainActivity.Companion.MESSAGE_CLIENT
import com.example.mqtt_testapp.MainActivity.Companion.MESSAGE_SERVER

class MessageAdapter(context: Context, textViewResourceId: Int) :
    ArrayAdapter<String>(context, textViewResourceId) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater =  context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val root = convertView?:inflater.inflate(R.layout.item_message, parent, false)

        root.findViewById<TextView>(R.id.message).apply {
            val str = getItem(position)?:""
            text = str

            when{
                str.contains(MESSAGE_SERVER) -> setBackgroundResource(R.color.placeHolderColor0)
                str.contains(MESSAGE_CLIENT) -> setBackgroundResource(R.color.white)
                else -> setBackgroundResource(R.color.placeHolderColor1)
            }
        }

        return root
    }
}