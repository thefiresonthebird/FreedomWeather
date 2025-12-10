package com.thefiresonthebird.freedomweather.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.thefiresonthebird.freedomweather.data.WeatherUpdateHelper

class WeatherWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val TAG = "WeatherWorker"
    private val weatherUpdateHelper = WeatherUpdateHelper(context)

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting background weather update")

        return if (weatherUpdateHelper.updateWeather()) {
            Log.i(TAG, "Weather update successful")
            Result.success()
        } else {
            Log.e(TAG, "Weather update failed")
            Result.retry()
        }
    }
}
