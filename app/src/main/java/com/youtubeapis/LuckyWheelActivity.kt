package com.youtubeapis


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.youtubeapis.wheelView.LuckyWheel
import com.youtubeapis.wheelView.OnLuckyWheelReachTheTarget
import com.youtubeapis.wheelView.WheelItem


class LuckyWheelActivity : AppCompatActivity() {


    private var lw: LuckyWheel? = null
    private var wheelItems: ArrayList<WheelItem>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lucky_wheel)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        generateWheelItems()

        lw = findViewById(R.id.lwv)
        lw?.addWheelItems(wheelItems)
        lw?.setTarget(0)


        lw?.setLuckyWheelReachTheTarget(object : OnLuckyWheelReachTheTarget{
            override fun onReachTarget(index : Int) {
                Log.i("TAG", "onReachTarget: $index")
                //val safeIndex = if (index <= 0) 0 else index - 1
                val label = if (wheelItems!![index].text == "10") {
                    wheelItems!![index].text
                } else {
                    wheelItems!![index].text
                }
                Toast.makeText(
                    this@LuckyWheelActivity,
                    "Target Reached ${label}",
                    Toast.LENGTH_LONG
                ).show()

            }
        })

        val oneIndexes = wheelItems?.mapIndexed { index, item ->
            /*if (item.text == "10") 0 else*/ index
        }


        val start = findViewById<Button>(R.id.start)
        start.setOnClickListener {

            val targetIndex = oneIndexes?.random() ?: 0
            Log.i("TAG", "onCreate: $targetIndex")
            lw?.rotateWheelTo(targetIndex)
        }

    }


    private fun getBitmap() : Bitmap{
       return BitmapFactory.decodeResource(resources, R.drawable.star)
    }
    private fun generateWheelItems() {
        wheelItems = ArrayList()
        wheelItems?.add(WheelItem(getColor(R.color.white), getBitmap(), "1", getColor(R.color.black)))
        wheelItems?.add(WheelItem(getColor(R.color.red), getBitmap(), "5"))
        wheelItems?.add(WheelItem(getColor(R.color.white), getBitmap(), "2", getColor(R.color.black)))
        wheelItems?.add(WheelItem(getColor(R.color.red), getBitmap(), "2"))
        wheelItems?.add(WheelItem(getColor(R.color.white), getBitmap(), "1", getColor(R.color.black)))
        wheelItems?.add(WheelItem(getColor(R.color.red), getBitmap(), "2"))
        wheelItems?.add(WheelItem(getColor(R.color.white), getBitmap(), "10", getColor(R.color.black)))
        wheelItems?.add(WheelItem(getColor(R.color.red), getBitmap(), "1"))
        wheelItems?.add(WheelItem(getColor(R.color.white), getBitmap(), "1", getColor(R.color.black)))
        wheelItems?.add(WheelItem(getColor(R.color.red), getBitmap(), "2"))
    }
}