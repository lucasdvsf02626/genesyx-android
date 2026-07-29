package com.genesyx.app.data.remote.dto

import com.genesyx.app.domain.model.GenesyxProduct
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire model for a Supabase `genesyx_products` row (read-only catalogue). */
@Serializable
data class GenesyxProductDto(
    val id: String,
    val name: String,
    val dose: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    val active: Boolean = true,
)

fun GenesyxProductDto.toDomain(): GenesyxProduct = GenesyxProduct(id = id, name = name, dose = dose)
