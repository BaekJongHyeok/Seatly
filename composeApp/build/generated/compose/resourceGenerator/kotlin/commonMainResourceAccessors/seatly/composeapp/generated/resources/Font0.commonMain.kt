@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package seatly.composeapp.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.InternalResourceApi

private object CommonMainFont0 {
  public val material_symbols_outlined: FontResource by 
      lazy { init_material_symbols_outlined() }
}

@InternalResourceApi
internal fun _collectCommonMainFont0Resources(map: MutableMap<String, FontResource>) {
  map.put("material_symbols_outlined", CommonMainFont0.material_symbols_outlined)
}

internal val Res.font.material_symbols_outlined: FontResource
  get() = CommonMainFont0.material_symbols_outlined

private fun init_material_symbols_outlined(): FontResource =
    org.jetbrains.compose.resources.FontResource(
  "font:material_symbols_outlined",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/seatly.composeapp.generated.resources/font/material_symbols_outlined.ttf", -1, -1),
    )
)
