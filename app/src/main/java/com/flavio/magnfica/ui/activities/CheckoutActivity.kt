package com.flavio.magnfica.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.flavio.magnfica.adapters.CheckoutAdapter
import com.flavio.magnfica.database.*
import com.flavio.magnfica.databinding.ActivityCheckoutBinding
import com.flavio.magnfica.infra.Constants
import com.flavio.magnfica.infra.Constants.ENTREGA
import com.flavio.magnfica.infra.Constants.PEDIDO
import com.flavio.magnfica.infra.SecurityPreferences
import com.flavio.magnfica.infra.util.viewmodel.CheckoutViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_checkout.*
import java.text.NumberFormat
import java.util.*

class CheckoutActivity : AppCompatActivity() {

    private lateinit var layoutBinding: ActivityCheckoutBinding
    private var precoTotal: Double = 0.0
    private lateinit var address: Address
    private lateinit var userId: String
    private lateinit var pagamento: String
    private lateinit var mStorageRef: StorageReference
    private lateinit var mDatabase: DatabaseReference
    private lateinit var mSharedPreferences: SecurityPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mStorageRef = FirebaseStorage.getInstance().reference
        mSharedPreferences = SecurityPreferences(this)
        mDatabase = Firebase.database.reference
        userId = GoogleSignIn.getLastSignedInAccount(this)?.id.toString()

        // Use view binding to access the UI elements
        layoutBinding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(layoutBinding.root)

//        // Check whether Google Pay can be used to complete a payment
//        model.canUseGooglePay.observe(this, Observer(::setGooglePayAvailable))

        val item = mSharedPreferences.getCartItem(Constants.ACTUAL_LIST)
        precoTotal = (34.99 * item.size)
        total_preco.text = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(precoTotal)
        val itemAdapter = CheckoutAdapter()
        recycler_chkout.adapter = itemAdapter
        if (item.isNotEmpty()) {
            itemAdapter.setDataSet(item)
        }

        add_endereco_button.setOnClickListener {
            startActivity(Intent(this, AddressActivity::class.java))
        }

        entrega_radiogroup.setOnCheckedChangeListener { radioGroup, i ->
            if (retirada.isChecked) {
                end_l.visibility = View.GONE
                total_preco.text =
                    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(precoTotal)
            } else if (entrega.isChecked) {
                end_l.visibility = View.VISIBLE
                read()
                total_preco.text =
                    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(precoTotal + 2.50)
            }
        }

        button_finalizar_pedido.setOnClickListener {
            val id = Random().nextInt(1000)
            if (cartao_radio.isChecked || dinheiro_radio.isChecked) {

                if (cartao_radio.isChecked) {
                    pagamento = "cartão"
                } else if (dinheiro_radio.isChecked) {
                    pagamento = "dinheiro"
                }

                if (entrega.isChecked) {
                    if (::address.isInitialized) {

                        val pedido =
                            Pedido(
                                item,
                                address,
                                precoTotal + 2.50,
                                pagamento,
                                "Receber em casa",
                                false,
                                id = id,
                                userId = userId
                            )
                        mDatabase.child("pedidos").child(userId)
                            .child("pedido $id")
                            .setValue(pedido)
                            .addOnSuccessListener {
                                mSharedPreferences.storeInt(ENTREGA, 1)
                                mSharedPreferences.savePedido(PEDIDO, pedido)
                                process(pedido.items as MutableList<Cart>)
                            }.addOnFailureListener {
                                Toast.makeText(
                                    this,
                                    "Desculpe, algo deu errado. Tente novamente mais tarde. Erro:${it}",
                                    Toast.LENGTH_LONG
                                ).show()
                                onBackPressed()
                            }

                    } else {
                        Toast.makeText(this, "Adicione um endereço primeiro", Toast.LENGTH_LONG)
                            .show()
                    }
                } else {
                    val pedido = Pedido(
                        item,
                        total = precoTotal,
                        pagamento = pagamento,
                        entrega = "Retirar na loja",
                        id = id,
                        userId = userId
                    )
                    mDatabase.child("pedidos").child(userId)
                        .child("pedido $id")
                        .setValue(pedido)
                        .addOnSuccessListener {
                            mSharedPreferences.storeInt(ENTREGA, 0)
                            mSharedPreferences.savePedido(PEDIDO, pedido)
                            process(pedido.items as MutableList<Cart>)
                        }.addOnFailureListener {
                            Toast.makeText(
                                this,
                                "Desculpe, algo deu errado. Tente novamente mais tarde. Erro:${it}",
                                Toast.LENGTH_LONG
                            ).show()
                            onBackPressed()
                        }
                }
            } else {
                Toast.makeText(this, "Selecione a forma de pagamento primeiro.", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun read() {
        mDatabase.child("usuarios").child(userId).get().addOnSuccessListener {
            if (it.exists()) {
                address = it.getValue(Address::class.java)!!
                if (address != null) {
                    end_actual_l.visibility = View.VISIBLE
                    end_nome.text = address.nome
                    end_tel.text = address.tel
                    end_end.text = address.end + ", " + address.num + ", " + address.bairro
                    end_comp.text = address.comp
                    add_endereco_button.text = "Alterar endereço"
                } else {
                    end_actual_l.visibility = View.GONE
                }

            }
        }
    }

    override fun onResume() {
        super.onResume()
        read()
    }

    private fun process(items: MutableList<Cart>) {
        val database = Firebase.database.reference
        val l = mutableListOf<Favorites>()
        database.child("blusas").get().addOnSuccessListener { it ->
            for (snap in it.children) {
                val i = snap.getValue(Favorites::class.java)
                if (i != null) {
                    l.add(i)
                }
            }
            items.onEach {
                val fav = l.find { l ->
                    l.title == it.title
                }
                val estoque: MutableList<Estoque>? = fav?.estoque

                val estOp = estoque?.find { e ->
                    e.tam == it.sel.tam
                }

                estoque?.remove(estOp)
                estOp?.qt = estOp!!.qt.minus(it.sel.qt)
                estoque.add(estOp)

                fav.estoque = estoque
                l.remove(fav)
                l.add(fav)
            }
            database.child("blusas").setValue(l).addOnSuccessListener {
                AppDatabase.getDatabase(this).cartDao().deleteAll()
                startActivity(Intent(this, CompleteActivity::class.java))
            }
        }
    }
//    /**
//     * If isReadyToPay returned `true`, show the button and hide the "checking" text. Otherwise,
//     * notify the user that Google Pay is not available. Please adjust to fit in with your current
//     * user flow. You are not required to explicitly let the user know if isReadyToPay returns `false`.
//     *
//     * @param available isReadyToPay API response.
//     */
//    private fun setGooglePayAvailable(available: Boolean) {
//        if (available) {
//            google_pay_check.visibility = View.VISIBLE
//        } else {
//            google_pay_check.visibility = View.GONE
//        }
//    }
//
//    private fun requestPayment() {
//
//        // Disables the button to prevent multiple clicks.
//        google_pay_check.isClickable = false
//
//        // The price provided to the API should include taxes and shipping.
//        // This price is not displayed to the user.
//        val dummyPriceCents = 100L
//        val shippingCostCents = 900L
//        val task = model.getLoadPaymentDataTask(dummyPriceCents + shippingCostCents)
//
//        // Shows the payment sheet and forwards the result to the onActivityResult method.
//        AutoResolveHelper.resolveTask(task, this, loadPaymentDataRequestCode)
//    }
//
//    /**
//     * Handle a resolved activity from the Google Pay payment sheet.
//     *
//     * @param requestCode Request code originally supplied to AutoResolveHelper in requestPayment().
//     * @param resultCode Result code returned by the Google Pay API.
//     * @param data Intent from the Google Pay API containing payment or error data.
//     * @see [Getting a result
//     * from an Activity](https://developer.android.com/training/basics/intents/result)
//     */
//    @Suppress("Deprecation")
//    // Suppressing deprecation until `registerForActivityResult` is available on the Google Pay API.
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        when (requestCode) {
//            // Value passed in AutoResolveHelper
//            loadPaymentDataRequestCode -> {
//                when (resultCode) {
//                    RESULT_OK ->
//                        data?.let { intent ->
//                            PaymentData.getFromIntent(intent)?.let(::handlePaymentSuccess)
//                        }
//
//                    RESULT_CANCELED -> {
//                        // The user cancelled the payment attempt
//                    }
//
//                    AutoResolveHelper.RESULT_ERROR -> {
//                        AutoResolveHelper.getStatusFromIntent(data)?.let {
//                            handleError(it.statusCode)
//                        }
//                    }
//                }
//
//                // Re-enables the Google Pay payment button.
//                google_pay_check.isClickable = true
//            }
//        }
//    }
//
//    /**
//     * PaymentData response object contains the payment information, as well as any additional
//     * requested information, such as billing and shipping address.
//     *
//     * @param paymentData A response object returned by Google after a payer approves payment.
//     * @see [Payment
//     * Data](https://developers.google.com/pay/api/android/reference/object.PaymentData)
//     */
//    private fun handlePaymentSuccess(paymentData: PaymentData) {
//        val paymentInformation = paymentData.toJson()
//
//        try {
//            // Token will be null if PaymentDataRequest was not constructed using fromJson(String).
//            val paymentMethodData =
//                JSONObject(paymentInformation).getJSONObject("paymentMethodData")
//            val billingName = paymentMethodData.getJSONObject("info")
//                .getJSONObject("billingAddress").getString("name")
//            Log.d("BillingName", billingName)
//
//            Toast.makeText(
//                this,
//                getString(R.string.payments_show_name, billingName),
//                Toast.LENGTH_LONG
//            ).show()
//
//            // Logging token string.
//            Log.d(
//                "GooglePaymentToken", paymentMethodData
//                    .getJSONObject("tokenizationData")
//                    .getString("token")
//            )
//
//        } catch (error: JSONException) {
//            Log.e("handlePaymentSuccess", "Error: $error")
//        }
//    }
//
//    /**
//     * At this stage, the user has already seen a popup informing them an error occurred. Normally,
//     * only logging is required.
//     *
//     * @param statusCode will hold the value of any constant from CommonStatusCode or one of the
//     * WalletConstants.ERROR_CODE_* constants.
//     * @see [
//     * Wallet Constants Library](https://developers.google.com/android/reference/com/google/android/gms/wallet/WalletConstants.constant-summary)
//     */
//    private fun handleError(statusCode: Int) {
//        Log.w("loadPaymentData failed", "Error code: $statusCode")
//    }
}