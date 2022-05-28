package com.flavio.magnfica.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.flavio.magnfica.R
import com.flavio.magnfica.adapters.FavAdapter
import com.flavio.magnfica.database.AppDatabase
import com.flavio.magnfica.database.Favorites
import com.flavio.magnfica.infra.FirebaseDatabase.Companion.getList
import com.flavio.magnfica.infra.SecurityPreferences
import com.flavio.magnfica.ui.activities.ViewActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.fragment_favorites.*
import java.util.*

class FavoritesFragment : Fragment() {

    private lateinit var itemAdapter: FavAdapter
    private lateinit var mDatabase: AppDatabase
    private lateinit var mStorageRef: StorageReference
    private lateinit var mSharedPreferences: SecurityPreferences
    private lateinit var list: MutableList<Favorites>

    override fun onCreate(savedInstanceState: Bundle?) {
        parentFragment?.also { parentFragment ->
            parentFragment.postponeEnterTransition()
        }
        super.onCreate(savedInstanceState)
        mStorageRef = FirebaseStorage.getInstance().reference
        mDatabase = AppDatabase.getDatabase(requireContext())
        mSharedPreferences = SecurityPreferences(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_favorites, container, false)

        view.findViewById<RecyclerView>(R.id.recycler_fav)
            .setOnScrollChangeListener { viewv, _, _, _, _ ->
                if (!viewv.canScrollVertically(1) && viewv.canScrollVertically(-1)) {
                    requireActivity().findViewById<BottomNavigationView>(R.id.bottom_bar).visibility =
                        View.GONE
                } else {
                    requireActivity().findViewById<BottomNavigationView>(R.id.bottom_bar).visibility =
                        View.VISIBLE
                }
            }

        return view
    }


    override fun onStart() {
        super.onStart()

        readData()

        val helper = androidx.recyclerview.widget.ItemTouchHelper(
            ItemTouchHelper(
                androidx.recyclerview.widget.ItemTouchHelper.UP or androidx.recyclerview.widget.ItemTouchHelper.DOWN,
                androidx.recyclerview.widget.ItemTouchHelper.LEFT
            )
        )
        helper.attachToRecyclerView(recycler_fav)
    }

    override fun onResume() {
        super.onResume()
        readData()
    }

    private fun readData() {
        itemAdapter = FavAdapter { adapter, imageView -> onClick(adapter, imageView) }
        recycler_fav.adapter = itemAdapter

        list = mDatabase.favoritesDao().getAll()
        if (list.isEmpty()) {
            text_empty_fav.visibility = View.VISIBLE
        } else {
            text_empty_fav.visibility = View.GONE
            itemAdapter.setDataSet(list)
        }
        showBadges()
    }

    private fun onClick(adapter: Favorites, imageView: ImageView) {
        val intent = Intent(requireContext(), ViewActivity::class.java)
        intent.putExtra("extra", adapter)
        intent.putExtra("list", getList)
        startActivity(intent)
    }

    inner class ItemTouchHelper(dragDirs: Int, swipDirs: Int) :
        androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(dragDirs, swipDirs) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val from = viewHolder.bindingAdapterPosition
            val to = target.bindingAdapterPosition

            Collections.swap(list, from, to)
            mDatabase.favoritesDao().deleteAll()
            list.forEach {
                mDatabase.favoritesDao().insertAll(it)
            }
            itemAdapter.notifyItemMoved(from, to)
            return true
        }


        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            if (direction == androidx.recyclerview.widget.ItemTouchHelper.LEFT) {
                mDatabase.favoritesDao().delete(list[viewHolder.bindingAdapterPosition])
                list = mDatabase.favoritesDao().getAll()
                itemAdapter.setDataSet(list)
                if (list.size == 0) {
                    text_empty_fav.visibility = View.VISIBLE
                }
                showBadges()
            }
        }
    }

    private fun showBadges() {
        val bottomBar = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_bar)
        bottomBar.getOrCreateBadge(R.id.page_2).number = list.size
    }
}