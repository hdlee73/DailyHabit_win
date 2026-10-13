package com.hdlee73.dailyhabit.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hdlee73.dailyhabit.data.Photos

/** 저장된 방문 사진 (정사각형으로 잘라 보여준다). 파일이 없으면 아무것도 그리지 않는다 */
@Composable
fun PhotoThumb(name: String, size: Dp, modifier: Modifier = Modifier) {
    if (name.isBlank()) return
    val bitmap by produceState<ImageBitmap?>(null, name) {
        value = Photos.load(name, 640)
    }
    val bmp = bitmap ?: return
    Box(modifier.size(size).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainer)) {
        Image(bmp, contentDescription = "방문 사진", contentScale = ContentScale.Crop, modifier = Modifier.size(size))
    }
}
