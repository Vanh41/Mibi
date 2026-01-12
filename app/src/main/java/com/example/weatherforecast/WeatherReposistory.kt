package com.example.weatherforecast

import retrofit2.Call

class WeatherRepository(
    private val api: apiInterface,
    private val database: WeatherDatabase
) {
    // Gọi API lấy thời tiết
    fun getWeatherData(city: String): Call<weatherforecast> {
        // Bạn có thể lưu API Key vào file constants hoặc buildConfig để bảo mật hơn
        return api.getWeatherData(city, "67a27574e9ff6d5c4b49ae1ce2b02989", "metric")
    }

    // Lưu lịch sử vào Database
    suspend fun saveWeatherToDb(weather: SavedWeather) {
        database.weatherDao().insertWeather(weather)
    }

    // Lấy danh sách lịch sử
    suspend fun getAllHistory(): List<SavedWeather> {
        return database.weatherDao().getAllWeather()
    }
}