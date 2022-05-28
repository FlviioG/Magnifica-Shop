package com.flavio.magnfica.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.flavio.magnfica.R
import com.flavio.magnfica.adapters.CartAdapter
import com.flavio.magnfica.database.AppDatabase
import com.flavio.magnfica.database.Cart
import com.flavio.magnfica.database.Estoque
import com.flavio.magnfica.database.Favorites
import com.flavio.magnfica.infra.Constants
import com.flavio.magnfica.infra.FirebaseDatabase.Companion.getList
import com.flavio.magnfica.infra.SecurityPreferences
import com.flavio.magnfica.ui.activities.CheckoutActivity
import com.flavio.magnfica.ui.activities.LoginActivity
import com.flavio.magnfica.ui.activities.ViewActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.fragment_cart.*
import java.util.*

class CartFragment : Fragment() {

    private lateinit var itemAdapter: CartAdapter
    private lateinit var mDatabase: AppDatabase
    private lateinit var mStorageRef: StorageReference
    private lateinit var mSharedPreferences: SecurityPreferences
    private lateinit var list: MutableList<Cart>

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
        val view = inflater.inflate(R.layout.fragment_cart, container, false)
        val bottomBar = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_bar)

        view.findViewById<RecyclerView>(R.id.recycler_cart)
            .setOnScrollChangeListener { rView, _, _, _, _ ->
                if (!rView.canScrollVertically(1) && rView.canScrollVertically(-1)) {
                    bottomBar.findViewById<BottomNavigationView>(R.id.bottom_bar).visibility =
                        View.GONE
                } else {
                    bottomBar.findViewById<BottomNavigationView>(R.id.bottom_bar).visibility =
                        View.VISIBLE
                }
            }

        view.findViewById<Button>(R.id.button_excluir_tudo).setOnClickListener {
            mDatabase.cartDao().deleteAll()
            list = mDatabase.cartDao().getAll()
            readData()
            showBadges()
        }

        view.findViewById<Button>(R.id.button_finalizar_compra).setOnClickListener {
            if (Firebase.auth.currentUser == null) {
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                Toast.makeText(requireContext(), "Faça login primeiro", Toast.LENGTH_SHORT).show()
                mSharedPreferences.storeInt(Constants.LOGIN, 1)
                mSharedPreferences.storeInt(Constants.SHOPPING, 2)
            } else {
                mSharedPreferences.saveCartList(Constants.ACTUAL_LIST, list)
                startActivity(Intent(requireContext(), CheckoutActivity::class.java))
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
        helper.attachToRecyclerView(recycler_cart)
    }

    override fun onResume() {
        super.onResume()
        readData()
    }

    private fun readData() {
        list = mDatabase.cartDao().getAll()
        val dataList = getList

        itemAdapter = CartAdapter { adapter, imageView -> onClick(adapter, imageView) }
        recycler_cart.adapter = itemAdapter

        if (list.isEmpty()) {
            text_empty_cart.visibility = View.VISIBLE
            buttons_cart.visibility = View.GONE
            showBadges()
        } else {
            text_empty_cart.visibility = View.GONE
            buttons_cart.visibility = View.VISIBLE

            list.onEach {
                val fav = Favorites(
                    it.key,
                    it.title,
                    it.desc,
                    it.estoque as MutableList<Estoque>,
                    it.ref,
                    it.new
                )

                val f = dataList.find {
                    it.title == fav.title
                }

                if (f == null) {
                    list.remove(it)
                    mDatabase.cartDao().delete(it)
                    Toast.makeText(
                        requireContext(),
                        "Um ou mais itens foram removidos por falta de estoque.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            itemAdapter.setDataSet(list)
            showBadges()
        }
    }

    private fun onClick(adapter: Cart, imageView: ImageView) {
        val a = Favorites(
            adapter.key,
            adapter.title,
            adapter.desc,
            adapter.estoque as MutableList<Estoque>,
            adapter.ref
        )

        val intent = Intent(requireContext(), ViewActivity::class.java)
        intent.putExtra("extra", a)
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
            mDatabase.cartDao().deleteAll()
            list.forEach {
                mDatabase.cartDao().insertAll(it)
            }
            itemAdapter.notifyItemMoved(from, to)
            return true
        }


        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            if (direction == androidx.recyclerview.widget.ItemTouchHelper.LEFT) {
                mDatabase.cartDao().delete(list[viewHolder.bindingAdapterPosition])
                list = mDatabase.cartDao().getAll()
                itemAdapter.setDataSet(list)

                showBadges()
                if (list.size == 0) {
                    text_empty_cart.visibility = View.VISIBLE
                    buttons_cart.visibility = View.GONE
                }
            }
        }
    }

    private fun showBadges() {
        val bottomBar = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_bar)
        bottomBar.getOrCreateBadge(R.id.page_3).number = list.size
    }
}