package kr.jiyeok.seatly.ui.component.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import android.location.Geocoder
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Composable
actual fun MapView(
    modifier: Modifier,
    address: String,
    cafeName: String
) {
    val context = LocalContext.current
    var location by remember { mutableStateOf<LatLng?>(null) }

    LaunchedEffect(address) {
        if (address.isBlank()) {
            location = LatLng(37.5665, 126.9780)
            return@LaunchedEffect
        }

        try {
            val geocoder = Geocoder(context, Locale.KOREA)
            val addresses = geocoder.getFromLocationName(address, 1)
            if (!addresses.isNullOrEmpty()) {
                location = LatLng(addresses[0].latitude, addresses[0].longitude)
            } else {
                location = LatLng(37.5665, 126.9780)
            }
        } catch (e: Exception) {
            location = LatLng(37.5665, 126.9780)
        }
    }

    if (location != null) {
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(location!!, 15f)
        }

        GoogleMap(
            modifier = modifier,
            cameraPositionState = cameraPositionState
        ) {
            Marker(
                state = MarkerState(position = location!!),
                title = cafeName,
                snippet = address
            )
        }
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("지도 로딩 중...")
        }
    }
}
