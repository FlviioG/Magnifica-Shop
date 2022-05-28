package com.flavio.magnfica.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import com.flavio.magnfica.BuildConfig
import com.flavio.magnfica.R
import com.flavio.magnfica.database.AppDatabase
import com.flavio.magnfica.infra.Constants.LOGIN
import com.flavio.magnfica.infra.SecurityPreferences
import com.flavio.magnfica.ui.activities.AddressActivity
import com.flavio.magnfica.ui.activities.LoginActivity
import com.flavio.magnfica.ui.activities.PedidosActivity
import com.flavio.magnfica.ui.activities.SplashActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.tasks.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.net.URLEncoder

class AccountFragment : Fragment() {

    private lateinit var mDatabase: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mDatabase = AppDatabase.getDatabase(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_account, container, false)

        val gName = GoogleSignIn.getLastSignedInAccount(requireContext())?.displayName
        val cardAccount = view.findViewById<LinearLayoutCompat>(R.id.card_account)
        val pedidos = view.findViewById<TextView>(R.id.account_compras)
        val dados = view.findViewById<TextView>(R.id.account_dados)
        val esvaziar = view.findViewById<TextView>(R.id.account_limpar)
        val contato = view.findViewById<TextView>(R.id.account_contato)
        val comentarios = view.findViewById<TextView>(R.id.account_comentarios)
        val excluirConta = view.findViewById<TextView>(R.id.account_exclusao)
        val sair = view.findViewById<TextView>(R.id.account_logout)
        val buttonLogin = view.findViewById<ExtendedFloatingActionButton>(R.id.buttonLogin_account)
        val retirarP = view.findViewById<TextView>(R.id.retirar_pedido_pedidos)
        val txtVersion = view.findViewById<TextView>(R.id.txt_version)

//        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_bar).visibility =
//            View.GONE

        if (gName == null) {
            cardAccount.visibility = View.GONE
            buttonLogin.visibility = View.VISIBLE
        }
        txtVersion.text = "Versão: ${BuildConfig.VERSION_NAME}"

        pedidos.setOnClickListener {
            startActivity(Intent(requireContext(), PedidosActivity::class.java))
        }
        dados.setOnClickListener {
            startActivity(Intent(requireContext(), AddressActivity::class.java))
        }
        esvaziar.setOnClickListener {
            mDatabase.cartDao().deleteAll()
            mDatabase.favoritesDao().deleteAll()
            notify("Tudo limpo")
            val bottomBar = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_bar)

            bottomBar.getOrCreateBadge(R.id.page_2).number = 0
            bottomBar.getOrCreateBadge(R.id.page_3).number = 0
        }
        contato.setOnClickListener {
            contatoIntent("Olá, preciso de ajuda.")
        }
        comentarios.setOnClickListener {
            val reviewManager = ReviewManagerFactory.create(requireContext())

            val requestReviewTask = reviewManager.requestReviewFlow()
            requestReviewTask.addOnCompleteListener { request ->
                if (request.isSuccessful) {
                    // Request succeeded and a ReviewInfo instance was received
                    val reviewInfo: ReviewInfo = request.result
                    val launchReviewTask: Task<*> =
                        reviewManager.launchReviewFlow(requireActivity(), reviewInfo)
                    launchReviewTask.addOnCompleteListener {
                        // The review has finished, continue your app flow.
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Tente novamente mais tarde",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Tente novamente mais tarde", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        excluirConta.setOnClickListener {
            MaterialAlertDialogBuilder(requireActivity())
                .setTitle("Você tem certeza disso?")
                .setMessage("Todos os seus dados serão exluídos. As compras já realizadas não serão afetadas.")
                .setPositiveButton("Sim") { _, _ ->
                    Firebase.database.reference.child("usuarios")
                        .child(GoogleSignIn.getLastSignedInAccount(requireContext())?.id.toString())
                        .removeValue()
                    logout()
                }
                .setNegativeButton("Não") { dialog, _ -> dialog.cancel() }
                .show()
        }
        sair.setOnClickListener {
            MaterialAlertDialogBuilder(requireActivity())
                .setTitle("Você tem certeza disso?")
                .setMessage("Precisará entrar novamente para ver suas compras e dados.")
                .setPositiveButton("Sim") { _, _ -> logout() }
                .setNegativeButton("Não") { dialog, _ -> dialog.cancel() }
                .show()
        }

        buttonLogin.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            SecurityPreferences(requireContext()).storeInt(LOGIN, 1)
        }

        retirarP.setOnClickListener {
            contatoIntent("Olá, gostaria de saber como retirar os pedidos.")
        }

        return view
    }

    override fun onPause() {
        super.onPause()
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_bar).visibility =
            View.VISIBLE
        requireActivity().findViewById<FloatingActionButton>(R.id.icon_back).visibility =
            View.GONE
    }


    private fun contatoIntent(msg: String) {
        val url =
            "https://api.whatsapp.com/send?phone=+5577999539603&text=" + URLEncoder.encode(
                msg,
                "UTF-8"
            )
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(url)
        startActivity(i)
    }

    override fun onResume() {
        super.onResume()
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_bar)?.visibility =
            View.GONE
        requireActivity().findViewById<FloatingActionButton>(R.id.icon_back)?.visibility =
            View.VISIBLE
    }


    private fun logout() {
        Firebase.auth.signOut()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("782307490556-t7dl824uqm3dto9ptlbmu4rae8auvhgm.apps.googleusercontent.com")
            .requestEmail()
            .build()

        GoogleSignIn.getClient(requireActivity(), gso).signOut().addOnSuccessListener {
            this.requireActivity().cacheDir.deleteRecursively()
            this.requireActivity().dataDir.deleteRecursively()
            mDatabase.favoritesDao().deleteAll()
            mDatabase.cartDao().deleteAll()
            this.requireActivity().finishAffinity()
            startActivity(Intent(requireContext(), SplashActivity::class.java))
        }
    }

    private fun notify(text: String) {
        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
    }
}