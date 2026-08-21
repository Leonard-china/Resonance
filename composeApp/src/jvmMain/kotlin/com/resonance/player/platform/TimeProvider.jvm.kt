package com.resonance.player.platform

import java.util.Calendar

actual fun currentHourOfDay(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
