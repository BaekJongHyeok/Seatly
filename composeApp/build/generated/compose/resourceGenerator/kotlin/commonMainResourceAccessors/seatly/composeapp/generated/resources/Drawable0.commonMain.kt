@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package seatly.composeapp.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.InternalResourceApi

private object CommonMainDrawable0 {
  public val icon_google: DrawableResource by 
      lazy { init_icon_google() }

  public val icon_kakao: DrawableResource by 
      lazy { init_icon_kakao() }

  public val icon_seatly: DrawableResource by 
      lazy { init_icon_seatly() }

  public val img_default_cafe: DrawableResource by 
      lazy { init_img_default_cafe() }
}

@InternalResourceApi
internal fun _collectCommonMainDrawable0Resources(map: MutableMap<String, DrawableResource>) {
  map.put("icon_google", CommonMainDrawable0.icon_google)
  map.put("icon_kakao", CommonMainDrawable0.icon_kakao)
  map.put("icon_seatly", CommonMainDrawable0.icon_seatly)
  map.put("img_default_cafe", CommonMainDrawable0.img_default_cafe)
}

internal val Res.drawable.icon_google: DrawableResource
  get() = CommonMainDrawable0.icon_google

private fun init_icon_google(): DrawableResource = org.jetbrains.compose.resources.DrawableResource(
  "drawable:icon_google",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/seatly.composeapp.generated.resources/drawable/icon_google.png", -1, -1),
    )
)

internal val Res.drawable.icon_kakao: DrawableResource
  get() = CommonMainDrawable0.icon_kakao

private fun init_icon_kakao(): DrawableResource = org.jetbrains.compose.resources.DrawableResource(
  "drawable:icon_kakao",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/seatly.composeapp.generated.resources/drawable/icon_kakao.png", -1, -1),
    )
)

internal val Res.drawable.icon_seatly: DrawableResource
  get() = CommonMainDrawable0.icon_seatly

private fun init_icon_seatly(): DrawableResource = org.jetbrains.compose.resources.DrawableResource(
  "drawable:icon_seatly",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/seatly.composeapp.generated.resources/drawable/icon_seatly.png", -1, -1),
    )
)

internal val Res.drawable.img_default_cafe: DrawableResource
  get() = CommonMainDrawable0.img_default_cafe

private fun init_img_default_cafe(): DrawableResource =
    org.jetbrains.compose.resources.DrawableResource(
  "drawable:img_default_cafe",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/seatly.composeapp.generated.resources/drawable/img_default_cafe.png", -1, -1),
    )
)
