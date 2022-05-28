package com.flavio.magnfica.ui.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.flavio.magnfica.R
import com.flavio.magnfica.adapters.PedidoAdapter
import com.flavio.magnfica.database.Pedido
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_pedidos.*

class PedidosActivity : AppCompatActivity() {

    private lateinit var mDatabase: DatabaseReference
    private var list = mutableListOf<Pedido>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pedidos)

        mDatabase = Firebase.database.reference
        val id = GoogleSignIn.getLastSignedInAccount(this)?.id

        mDatabase.child("pedidos").child(id.toString()).addValueEventListener(object :ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (snap in snapshot.children) {
                    val a = snap.getValue(Pedido::class.java)
                    if (a != null) {
                        list.add(a)
                    }
                }

                val pAdapter = PedidoAdapter { adapter -> onClick(adapter) }

                if (list.isEmpty()) {
                    text_empty_pedidos.visibility = View.VISIBLE
                } else {
                    text_empty_pedidos.visibility = View.GONE
                }

                recycler_pedidos.adapter = pAdapter
                pAdapter.setDataSet(list)
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })


    }

    private fun onClick(adapter: Pedido) {

    }
}