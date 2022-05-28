package com.flavio.magnfica.ui.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.flavio.magnfica.R
import com.flavio.magnfica.adapters.AdminAdapter
import com.flavio.magnfica.database.Estoque
import com.flavio.magnfica.database.Favorites
import com.flavio.magnfica.database.Pedido
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_admin.*

class AdminActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        readData()

        expand_admin1.setOnClickListener {
            if(expand_admin1.rotation == 90f) {
                expand_admin1.rotation = -90f
                l1.visibility = View.VISIBLE
                l2.visibility = View.GONE
                expand_admin2.rotation = 90f
            } else {
                expand_admin1.rotation = 90f
                l1.visibility = View.GONE
            }
        }

        expand_admin2.setOnClickListener {
            if(expand_admin2.rotation == 90f) {
                expand_admin2.rotation = -90f
                l2.visibility = View.VISIBLE
                l1.visibility = View.GONE
                expand_admin1.rotation = 90f
            } else {
                expand_admin2.rotation = 90f
                l2.visibility = View.GONE
            }
        }

        button_add.setOnClickListener {
            val tP = button1.isChecked
            val tM = button2.isChecked
            val tG = button3.isChecked
            val tGG = button4.isChecked

            val e = mutableListOf<Estoque>()

            if (tP) {
                e.add(Estoque("P", Integer.parseInt(editTextP.text.toString())))
            }
            if (tM) {
                e.add(Estoque("M", Integer.parseInt(editTextM.text.toString())))
            }
            if (tG) {
                e.add(Estoque("G", Integer.parseInt(editTextG.text.toString())))
            }
            if (tGG) {
                e.add(Estoque("GG", Integer.parseInt(editTextGG.text.toString())))
            }

            val child = Firebase.database.reference.child("blusas")

            child.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val a = snapshot.children.last().key
                    if (a != null) {
                        var b = Integer.parseInt(a)
                        b++
                        val fav = Favorites(
                            "$b",
                            textTitle.text.toString(),
                            textDesc.text.toString(),
                            e,
                            "blusas/${textRef.text}.webp"
                        )

                        Firebase.database.reference.child("blusas").child(b.toString())
                            .setValue(fav).addOnSuccessListener {
                            Toast.makeText(this@AdminActivity, "OK", Toast.LENGTH_SHORT).show()
                            textDesc.text.clear()
                            textTitle.text.clear()
                            button1.isChecked = false
                            button2.isChecked = false
                            button3.isChecked = false
                            button4.isChecked = false
                            editTextP.text.clear()
                            editTextM.text.clear()
                            editTextG.text.clear()
                            editTextGG.text.clear()
                            textRef.text.clear()
                        }

                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })

        }
    }

    private fun readData() {
        val list = mutableListOf<Pedido>()
        Firebase.database.reference.child("pedidos").get().addOnSuccessListener { snapshot ->
            for (snap in snapshot.children) {
                snap.children.forEach {
                    it.getValue(Pedido::class.java)?.let { t1 ->
                        if (!t1.entregue) {
                            list.add(t1)
                        }
                    }
                    val pAdapte = AdminAdapter { adapter -> onClick(adapter) }
                    recycler_admin.adapter = pAdapte
                    pAdapte.setDataSet(list)
                }
            }
        }
    }

    private fun onClick(adapter: Pedido) {
        TODO("Not yet implemented")
    }
}