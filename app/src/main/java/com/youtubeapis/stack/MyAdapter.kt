package com.youtubeapis.stack

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.youtubeapis.stack.StackFragment.Companion.newInstance


class MyAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
    override fun getCount(): Int {
        return NUM_ITEMS
    }

    override fun getItem(position: Int): Fragment {
        return newInstance()
    }

    companion object {
        const val NUM_ITEMS: Int = 1
    }
}