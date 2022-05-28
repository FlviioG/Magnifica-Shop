package com.flavio.magnfica.ui.fragments

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import android.widget.*
import androidx.appcompat.widget.AppCompatImageButton
import androidx.fragment.app.Fragment
import com.flavio.magnfica.R
import com.flavio.magnfica.database.AppDatabase
import com.flavio.magnfica.database.Cart
import com.flavio.magnfica.database.Estoque
import com.flavio.magnfica.database.Favorites
import com.flavio.magnfica.infra.Constants
import com.flavio.magnfica.infra.Constants.ACTUAL_LIST
import com.flavio.magnfica.infra.Constants.IS_LOW_MEMORY_DEVICE
import com.flavio.magnfica.infra.FirebaseDatabase.Companion.getList
import com.flavio.magnfica.infra.SecurityPreferences
import com.flavio.magnfica.ui.activities.CheckoutActivity
import com.github.mmin18.widget.RealtimeBlurView
import com.google.android.gms.common.wrappers.InstantApps
import kotlinx.android.synthetic.main.menu_cart.*

class MenuFragment : Fragment() {

    private lateinit var actualView: Favorites
    private var tam: MutableList<String> = mutableListOf()
    private lateinit var mDatabase: AppDatabase
    private lateinit var mSharedPreferences: SecurityPreferences
    private var memory: Int = 0

    private var qtP: Int = 0
    private var qtM: Int = 0
    private var qtG: Int = 0
    private var qtGG: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.window?.addFlags(FLAG_LAYOUT_NO_LIMITS)

        mDatabase = AppDatabase.getDatabase(requireContext())
        mSharedPreferences = SecurityPreferences(requireContext())
        memory = mSharedPreferences.getInt(IS_LOW_MEMORY_DEVICE)
        actualView = mSharedPreferences.getFav(Constants.ACTUAL_VIEW)

        val a = getList
        a.find {
            it.title == actualView.title
        }.apply {
            if(this != null) actualView = this
        }

        actualView.estoque.forEach { estoque ->
            tam.add(estoque.tam)

            when (estoque.tam) {
                "P" -> {
                    qtP = estoque.qt
                }
                "M" -> {
                    qtM = estoque.qt
                }
                "G" -> {
                    qtG = estoque.qt
                }
                "GG" -> {
                    qtGG = estoque.qt
                }
            }

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.menu_cart, container, false)
        val spinnerTam = view.findViewById<Spinner>(R.id.list_tam_menu)
        val buttonAddMenu = view.findViewById<Button>(R.id.button_add_menu)
        val buttonAddToCart = requireActivity().findViewById<TextView>(R.id.buttonAddToCart)
        val buttonCancelMenu = view.findViewById<Button>(R.id.button_cancel_menu)
        val textEstoque = view.findViewById<TextView>(R.id.estoque_menu)
        val qt = view.findViewById<TextView>(R.id.qt_menu)
        val blurView = view.findViewById<RealtimeBlurView>(R.id.blurView_menu)

        if (memory == 1) {
            view.findViewById<RealtimeBlurView>(R.id.blurView_menu).setBlurRadius(0.1f)
        }

        if (actualView.estoque.isEmpty()) {
            tam.add("Fora de estoque")
            buttonAddMenu?.apply {
                isActivated = false
                isEnabled = false
                isClickable = false
            }
            spinnerTam?.isActivated = false
            textEstoque?.apply {
                text = "0"
                isEnabled = false
                isClickable = false
            }
        }
        if (mSharedPreferences.getInt("buy") == 1) {
            buttonAddMenu?.text = "Comprar"
        }

        buttonAddMenu?.setOnClickListener {
            val v = Cart(
                actualView.key,
                actualView.title,
                actualView.desc,
                actualView.estoque,
                actualView.ref,
                Estoque(spinnerTam?.selectedItem.toString(), qt.text.toString().toInt())
            )

            if (!InstantApps.isInstantApp(requireContext())) {
                buttonAddToCart.text = "Adicionado à sacola"
                mDatabase.cartDao().insertAll(v)
                Toast.makeText(requireContext(), "Adicionado à sacola", Toast.LENGTH_SHORT).show()
            }

            if (mSharedPreferences.getInt("buy") == 1) {
                mSharedPreferences.saveCartList(ACTUAL_LIST, listOf(v))
                requireActivity().startActivity(
                    Intent(
                        requireContext(),
                        CheckoutActivity::class.java
                    )
                )
            }

            parentFragmentManager.beginTransaction().remove(this).commit()

            requireActivity().window?.clearFlags(FLAG_LAYOUT_NO_LIMITS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity?.window?.setDecorFitsSystemWindows(true)
            }
        }
        buttonCancelMenu.setOnClickListener {
            activity?.window?.clearFlags(FLAG_LAYOUT_NO_LIMITS)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity?.window?.setDecorFitsSystemWindows(true)
            }
            parentFragmentManager.beginTransaction().remove(this).commit()
        }
        blurView.setOnClickListener {
            activity?.window?.clearFlags(FLAG_LAYOUT_NO_LIMITS)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity?.window?.setDecorFitsSystemWindows(true)
            }
            parentFragmentManager.beginTransaction().remove(this).commit()
        }

        spinnerTam?.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, tam)
        qt(view)
        return view
    }

    private fun qt(view: View) {
        val spin: Spinner = view.findViewById(R.id.list_tam_menu)
        val qt: TextView = view.findViewById(R.id.qt_menu)
        val btPlus: AppCompatImageButton = view.findViewById(R.id.bt_plus)
        val btMinus: AppCompatImageButton = view.findViewById(R.id.bt_minus)
        val est: TextView = view.findViewById(R.id.estoque_menu)

        when (spin.selectedItem) {
            "P" -> {
                est.text = qtP.toString()
            }
            "M" -> {
                est.text = qtM.toString()
            }
            "G" -> {
                est.text = qtG.toString()
            }
            "GG" -> {
                est.text = qtGG.toString()
            }
        }

        spin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                qt.text = "1"

                btMinus.isEnabled = false

                when (spin.selectedItem) {
                    "P" -> {
                        est.text = qtP.toString()
                    }
                    "M" -> {
                        est.text = qtM.toString()
                    }
                    "G" -> {
                        est.text = qtG.toString()
                    }
                    "GG" -> {
                        est.text = qtGG.toString()
                    }
                }
                if (qt.text == est.text) {
                    btPlus.isEnabled = false
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
        }

        btMinus.setOnClickListener {
            var qtText = qt_menu.text.toString().toIntOrNull()

            if (qtText != null) {
                if (qtText > 1) {
                    if (qtText == 2) {
                        qtText--
                        qt_menu.text = qtText.toString()
                        btMinus.isEnabled = false
                        btPlus.isEnabled = true
                    } else {
                        qtText--
                        qt_menu.text = qtText.toString()
                        btPlus.isEnabled = true
                    }
                }
            }
        }

        btPlus.setOnClickListener {
            var qtText = qt_menu.text.toString().toIntOrNull()
            val estText = est.text.toString().toIntOrNull()

            if (estText != null && qtText != null) {
                if (estText > qtText) {
                    if (qtText == estText - 1) {
                        qtText++
                        qt_menu.text = qtText.toString()
                        btPlus.isEnabled = false
                        btMinus.isEnabled = true
                    } else {
                        qtText++
                        qt_menu.text = qtText.toString()
                        btMinus.isEnabled = true
                    }
                }
            }
        }


    }
}