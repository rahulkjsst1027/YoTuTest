package com.youtubeapis.stack

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.youtubeapis.R


class StackFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_stack, container, false)


        val stack = v.findViewById<StackView>(R.id.stackView)
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
        Log.d("TAG", "onCreateView: $screenHeight")
        stack.setCardHeight(screenHeight)
        stack.setAdapter(object : StackCallBack {
            override fun getTitle(position: Int): String {
                return "Item $position"
            }

            override fun getView(position: Int): View {
                val iv = ImageView(activity)
                iv.scaleType = ImageView.ScaleType.CENTER_CROP
                iv.setImageResource(R.drawable.marketing)
                iv.setBackgroundColor(-0x1)
                return iv
            }

            @SuppressLint("UseCompatLoadingForDrawables")
            override fun getIcon(position: Int): Drawable {
                return resources.getDrawable(R.mipmap.ic_launcher)
            }

            override fun getHeaderColor(position: Int): Int {
                return -0x1
            }

            override fun getCount(): Int {
                return 2
            }
        })

        stack.setOnItemClickListener { view, i ->
            Toast.makeText(
                view.context,
                "Card $i clicked",
                Toast.LENGTH_SHORT
            ).show()
        }

        return v
    }

    companion object {
        @JvmStatic
        fun newInstance(): StackFragment {
            return StackFragment()
        }
    }
}