package com.flavio.magnfica.ui.activities

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.flavio.magnfica.R
import com.flavio.magnfica.database.Address
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_adress.*

class AddressActivity : AppCompatActivity() {

    private lateinit var mDatabase: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adress)

        mDatabase = Firebase.database.reference

        read()

        button_save_inf.setOnClickListener {
            var qt = 0
            val l = listOf<EditText>(
                nome_inf,
                telefone_inf,
                endereco_inf,
                complemento_inf,
                numero_inf,
                bairro_inf
            )

            l.forEach {
                if (it.text.isNullOrBlank()) {
                    qt++
                }
            }

            if (qt > 0) {
                Toast.makeText(this, "Preencha todos os campos corretamente.", Toast.LENGTH_LONG)
                    .show()
            } else {
                val i = Address(
                    nome_inf.text.toString(),
                    telefone_inf.text.toString(),
                    endereco_inf.text.toString(),
                    numero_inf.text.toString().toInt(),
                    bairro_inf.text.toString(),
                    complemento_inf.text.toString()
                )
                mDatabase.child("usuarios")
                    .child(GoogleSignIn.getLastSignedInAccount(this)?.id.toString())
                    .setValue(i)
                Toast.makeText(this, "Dados atualizados", Toast.LENGTH_LONG).show()
                onBackPressed()
            }
        }
    }

    private fun read() {
        mDatabase.child("usuarios").child(GoogleSignIn.getLastSignedInAccount(this)?.id.toString())
            .get().addOnSuccessListener {
            val address: Address? = it.getValue(Address::class.java)
            if (address != null) {
                nome_inf.setText(address.nome)
                endereco_inf.setText(address.end)
                telefone_inf.setText(address.tel)
                numero_inf.setText(address.num.toString())
                bairro_inf.setText(address.bairro)
                complemento_inf.setText(address.comp)
            }
        }
    }
}