package com.example.weatherforecast

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherViewModel(private val repository: WeatherRepository) : ViewModel() {

    // LiveData chứa dữ liệu thời tiết thành công
    private val _weatherResult = MutableLiveData<weatherforecast>()
    val weatherResult: LiveData<weatherforecast> = _weatherResult

    // LiveData báo lỗi (nếu có)
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // Hàm gọi API
    fun fetchWeather(cityName: String) {
        val response = repository.getWeatherData(cityName)
        response.enqueue(object : Callback<weatherforecast> {
            override fun onResponse(
                call: Call<weatherforecast>,
                response: Response<weatherforecast>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    _weatherResult.postValue(data)

                    // Logic lưu vào DB khi lấy thành công
                    saveToHistory(cityName, data)
                } else {
                    _errorMessage.postValue("Lỗi lấy dữ liệu: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<weatherforecast>, t: Throwable) {
                _errorMessage.postValue(t.message ?: "Lỗi kết nối")
            }
        })
    }

    // Hàm lưu lịch sử (chạy ngầm với Coroutines)
    private fun saveToHistory(cityName: String, data: weatherforecast) {
        viewModelScope.launch(Dispatchers.IO) {
            val conditionValue = data.weather.firstOrNull()?.main ?: "unknown"
            val historyItem = SavedWeather(
                cityName = cityName,
                temperature = data.main.temp.toString(),
                date = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date()),
                condition = conditionValue
            )
            repository.saveWeatherToDb(historyItem)
        }
    }
}