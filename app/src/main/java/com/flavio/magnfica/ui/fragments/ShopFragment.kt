package com.flavio.magnfica.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.flavio.magnfica.R
import com.flavio.magnfica.adapters.ItemAdapter
import com.flavio.magnfica.database.AppDatabase
import com.flavio.magnfica.database.Favorites
import com.flavio.magnfica.infra.Constants.IS_LOW_MEMORY_DEVICE
import com.flavio.magnfica.infra.FirebaseDatabase.Companion.getList
import com.flavio.magnfica.infra.FirebaseDatabase.Companion.isReady
import com.flavio.magnfica.infra.SecurityPreferences
import com.flavio.magnfica.ui.activities.ViewActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.fragment_shop.*


class ShopFragment : Fragment() {

    private var memory: Int = 0
    private lateinit var mStorageRef: StorageReference
    private lateinit var mSharedPreferences: SecurityPreferences
    private lateinit var mDatabase: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        mSharedPreferences = SecurityPreferences(requireContext())
        memory = mSharedPreferences.getInt(IS_LOW_MEMORY_DEVICE)
        if (memory == 0) {
            parentFragment?.postponeEnterTransition()
            view?.doOnPreDraw { startPostponedEnterTransition() }
        }
        super.onCreate(savedInstanceState)
        mStorageRef = FirebaseStorage.getInstance().reference

        FirebaseDatabase.getInstance().apply {
            mDatabase = this.reference
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_shop, container, false)

        view.findViewById<RecyclerView>(R.id.recyclerView)
            .setOnScrollChangeListener { v, _, _, _, _ ->
                if (!v.canScrollVertically(1)) {
                    requireActivity().findViewById<BottomNavigationView>(R.id.bottom_bar)?.visibility =
                        View.GONE
                } else {
                    requireActivity().findViewById<BottomNavigationView>(R.id.bottom_bar)?.visibility =
                        View.VISIBLE
                }
            }

        return view
    }

    override fun onStart() {
        super.onStart()
        readData()
    }


    private fun readData() {
        if(!isReady) {
           Handler(Looper.myLooper()!!).postDelayed({readData()}, 250)
            return
        }
        val itemAdapter = ItemAdapter { adapter, imageView -> onClick(adapter, imageView) }
        recyclerView.adapter = itemAdapter
        itemAdapter.setDataSet(getList)
        showBadges()
    }


    private fun onClick(adapter: Favorites, imageView: ImageView) {
        val intent = Intent(requireContext(), ViewActivity::class.java)
        intent.putExtra("extra", adapter)
        intent.putExtra("list", getList)

        val opt: ActivityOptionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
            requireActivity(),
            imageView,
            "photo"
        )

        if (memory == 0) {
            startActivity(intent, opt.toBundle())
        } else {
            startActivity(intent)
        }
    }

    private fun showBadges() {
        val database = AppDatabase.getDatabase(requireContext())
        val cart = database.cartDao().getAll()
        val fav = database.favoritesDao().getAll()

        val bottomBar = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_bar)

        bottomBar.getOrCreateBadge(R.id.page_2).number = fav.size
        bottomBar.getOrCreateBadge(R.id.page_3).number = cart.size
    }
}
