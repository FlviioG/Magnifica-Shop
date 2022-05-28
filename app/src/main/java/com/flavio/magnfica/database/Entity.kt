package com.flavio.magnfica.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

@Parcelize
@Entity(tableName = "favorites")
data class Favorites(
    @ColumnInfo(name = "key") @PrimaryKey var key: String = "",
    @ColumnInfo(name = "title") var title: String = "",
    @ColumnInfo(name = "description") var desc: String = "",
    @ColumnInfo(name = "estoque")  var estoque: @RawValue MutableList<Estoque> = mutableListOf(),
    @ColumnInfo(name = "reference") var ref: String = "",
    @ColumnInfo(name = "novidade") var new: Boolean = false
//    @ColumnInfo(name = "id") @PrimaryKey(autoGenerate = true) var id: Long = 0
) : Parcelable

@Entity(tableName = "cart")
data class Cart(
    @ColumnInfo(name = "key") @PrimaryKey var key: String = "",
    @ColumnInfo(name = "title") var title: String = "",
    @ColumnInfo(name = "description") var desc: String = "",
    @ColumnInfo(name = "estoque") var estoque: @RawValue MutableList<Estoque> = mutableListOf(),
    @ColumnInfo(name = "reference") var ref: String = "",
    @ColumnInfo(name = "selecao") var sel: @RawValue Estoque = Estoque(),
    @ColumnInfo(name = "novidade") var new: Boolean = false
)

data class Address(
    var nome: String = "",
    var tel: String = "",
    var end: String = "",
    var num: Int = 0,
    var bairro: String = "",
    var comp: String = ""
)

@Parcelize
data class Estoque(
    var tam: String = "",
    var qt: Int = 0
): Parcelable

data class Pedido(
    var items: List<Cart> = listOf(),
    var endereco: Address = Address(),
    var total: Double = 0.0,
    var pagamento: String = "",
    var entrega: String = "",
    var entregue: Boolean = false,
    var id: Int = 0,
    var userId: String = ""
)

