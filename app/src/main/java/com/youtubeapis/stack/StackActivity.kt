package com.youtubeapis.stack


import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.youtubeapis.databinding.ActivityStackBinding


class StackActivity : AppCompatActivity() {

    private var mAdapter: MyAdapter? = null


    private lateinit var binding: ActivityStackBinding
    private val tabList = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityStackBinding.inflate(layoutInflater)
        setContentView(binding.root)


        //  mAdapter = MyAdapter(supportFragmentManager)
        // binding.pager.adapter = mAdapter

        /* var button = findViewById<View>(R.id.goto_first) as Button
        button.setOnClickListener { binding.pager.currentItem = 0 }
        button = findViewById<View>(R.id.goto_last) as Button
        button.setOnClickListener { binding.pager.currentItem = mAdapter!!.count - 1 }*/


        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
        Log.d("TAG", "onCreateView: $screenHeight")
        /* binding.stackView.setCardHeight(screenHeight)
        binding.stackView.setAdapter(object : StackCallBack {
            override fun getTitle(position: Int): String {
                return "Item $position"
            }

            override fun getView(position: Int): View {
                val iv = ImageView(this@StackActivity)
                iv.scaleType = ImageView.ScaleType.CENTER_CROP
                iv.setImageResource(R.drawable.marketing)
                iv.setBackgroundColor(Color.BLACK)
                return iv
            }

            @SuppressLint("UseCompatLoadingForDrawables")
            override fun getIcon(position: Int): Drawable {
                return resources.getDrawable(R.mipmap.ic_launcher)
            }

            override fun getHeaderColor(position: Int): Int {
                return Color.BLACK
            }

            override fun getCount(): Int {
                return 2
            }
        })

        binding.stackView.setOnItemClickListener { view, i ->
            Toast.makeText(
                view.context,
                "Card $i clicked",
                Toast.LENGTH_SHORT
            ).show()
        }*/

        setupRecyclerView()


    }

    private fun setupRecyclerView() {
        val tabList = mutableListOf<String>()
        for (i in 0 until 5) tabList.add("Tab $i")

        val adapter = TabAdapter(tabList)

        val layoutManager = StackLayoutManager(this)
        binding.rev.layoutManager = layoutManager
        binding.rev.adapter = adapter

        // Setup swipe gestures
        setupSwipeGestures(layoutManager)

    }

    private fun setupSwipeGestures(layoutManager: StackLayoutManager) {
        var initialY = 0f
        var currentPosition = -1
        var isDragging = false

        binding.rev.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialY = e.rawY
                        val child = rv.findChildViewUnder(e.x, e.y)
                        currentPosition = child?.let { rv.getChildAdapterPosition(it) } ?: -1
                        isDragging = currentPosition != -1
                        return isDragging
                    }
                }
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                if (!isDragging) return

                when (e.action) {
                    MotionEvent.ACTION_MOVE -> {
                        val deltaY = e.rawY - initialY
                        if (deltaY > 0) { // Only allow downward swipe
                            layoutManager.setTranslation(currentPosition, deltaY)
                        }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        val deltaY = e.rawY - initialY

                        /*if (deltaY > 300f) { // Swipe threshold reached
                            closeTabWithAnimation(currentPosition, deltaY, layoutManager)
                        } else {
                            resetPositionWithAnimation(currentPosition, deltaY, layoutManager)
                        }*/

                        isDragging = false
                        currentPosition = -1
                    }
                }
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
    }



}