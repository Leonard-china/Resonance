package com.resonance.player.platform

import androidx.compose.ui.graphics.ImageBitmap

expect fun decodeArtwork(path: String): ImageBitmap?
expect fun loadSponsorQr(isWeChat: Boolean): ImageBitmap?
