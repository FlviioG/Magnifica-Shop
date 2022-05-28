package com.flavio.magnfica.adapters

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fa: AppCompatActivity, private val contents: List<Fragment>): FragmentStateAdapter(fa) {
    override fun getItemCount(): Int {
        return contents.size
    }

    override fun createFragment(position: Int): Fragment {
       return contents[position]
    }
}