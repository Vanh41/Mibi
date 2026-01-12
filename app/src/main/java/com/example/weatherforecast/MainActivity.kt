package com.example.weatherforecast

import android.os.Bundle
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherforecast.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    // Khai báo ViewModel và Database
    private lateinit var viewModel: WeatherViewModel
    private lateinit var database: WeatherDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // 1. Khởi tạo Database & API
        database = WeatherDatabase.getDatabase(this)

        val api = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .build()
            .create(apiInterface::class.java)

        // 2. Khởi tạo Repository và ViewModel
        val repository = WeatherRepository(api, database)
        val factory = WeatherViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[WeatherViewModel::class.java]

        // 3. Lắng nghe dữ liệu từ ViewModel (Observer)
        setupObservers()

        // 4. Gọi dữ liệu mặc định ban đầu
        viewModel.fetchWeather("Indore")

        // Setup sự kiện tìm kiếm và lịch sử
        searchCity()
        binding.btnHistory.setOnClickListener {
            showHistoryDialog()
        }
    }

    private fun setupObservers() {
        // Khi có dữ liệu thời tiết thành công
        viewModel.weatherResult.observe(this) { responseBody ->
            val temp = binding.temp
            val maxT = binding.maxTemp
            val minT = binding.minTemp
            val weather = binding.weather
            val day = binding.day
            val date = binding.date
            val city = binding.city
            val humidity = binding.humidityValue
            val sunrise = binding.sunriseValue
            val sunset = binding.sunsetValue
            val condition = binding.conditionValue
            val sea = binding.seaValue
            val wind = binding.windSpeedValue

            // Cập nhật UI
            val temperature = responseBody.main.temp.toString()
            val maxTemp = responseBody.main.temp_max.toString()
            val minTemp = responseBody.main.temp_min.toString()
            val sunriseValue = responseBody.sys.sunrise
            val sunsetValue = responseBody.sys.sunset
            val humidityValue = responseBody.main.humidity.toString()
            val seaLevel = responseBody.main.pressure.toString()
            val windSpeed = responseBody.wind.speed.toString()
            val conditionValue = responseBody.weather.firstOrNull()?.main ?: "unknown"

            temp.text = getString(R.string.tempValue, temperature)
            maxT.text = getString(R.string.max, maxTemp)
            minT.text = getString(R.string.min, minTemp)
            weather.text = getString(R.string.temp, conditionValue)
            sunrise.text = getString(R.string.title4value, time(sunriseValue.toLong()))
            sunset.text = getString(R.string.title5value, time(sunsetValue.toLong()))
            humidity.text = getString(R.string.title1value, "$humidityValue %")
            condition.text = getString(R.string.title3value, conditionValue)
            sea.text = getString(R.string.title6value, seaLevel)
            day.text = dayName()
            date.text = exactDate()
            wind.text = getString(R.string.title2value, windSpeed)
            city.text = responseBody.name

            // Đổi hình nền/animation
            changeBackgroundOnCondition(conditionValue)
        }

        // Khi có lỗi (Optional)
        viewModel.errorMessage.observe(this) { errorMsg ->
            // Bạn có thể Toast lỗi lên nếu muốn
            // Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun searchCity() {
        val searchView = binding.searchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    // GỌI QUA VIEWMODEL
                    viewModel.fetchWeather(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })
    }

    // --- Các hàm UI Helper giữ nguyên ---

    private fun changeBackgroundOnCondition(conditions: String) {
        when (conditions) {
            "Clear Sky", "Sunny", "Clear" -> {
                binding.lottieAnimationView.setAnimation(R.raw.sun)
                binding.linearLayout.setBackgroundResource(R.drawable.backgroundshape2)
                binding.linearLayout2.setBackgroundResource(R.drawable.backgroundshape2)
                binding.linearLayout3.setBackgroundResource(R.drawable.backgroundshape2)
                binding.linearLayout4.setBackgroundResource(R.drawable.backgroundshape2)
                binding.linearLayout5.setBackgroundResource(R.drawable.backgroundshape2)
                binding.linearLayout6.setBackgroundResource(R.drawable.backgroundshape2)
            }
            "Partly Clouds", "Clouds", "Overcast", "Mist", "Foggy", "Haze" -> {
                binding.lottieAnimationView.setAnimation(R.raw.cloud)
                binding.linearLayout.setBackgroundResource(R.drawable.backgroundshape3)
                binding.linearLayout2.setBackgroundResource(R.drawable.backgroundshape3)
                binding.linearLayout3.setBackgroundResource(R.drawable.backgroundshape3)
                binding.linearLayout4.setBackgroundResource(R.drawable.backgroundshape3)
                binding.linearLayout5.setBackgroundResource(R.drawable.backgroundshape3)
                binding.linearLayout6.setBackgroundResource(R.drawable.backgroundshape3)
            }
            "Light Rain", "Drizzle", "Moderate Rain", "Showers", "Heavy Rain" -> {
                binding.lottieAnimationView.setAnimation(R.raw.rain)
                binding.linearLayout.setBackgroundResource(R.drawable.backgroundshape4)
                binding.linearLayout2.setBackgroundResource(R.drawable.backgroundshape4)
                binding.linearLayout3.setBackgroundResource(R.drawable.backgroundshape4)
                binding.linearLayout4.setBackgroundResource(R.drawable.backgroundshape4)
                binding.linearLayout5.setBackgroundResource(R.drawable.backgroundshape4)
                binding.linearLayout6.setBackgroundResource(R.drawable.backgroundshape4)
            }
            "Light Snow", "Moderate Snow", "Blizzard", "Heavy Snow" -> {
                binding.lottieAnimationView.setAnimation(R.raw.snow)
                binding.linearLayout.setBackgroundResource(R.drawable.backgroundshape5)
                binding.linearLayout2.setBackgroundResource(R.drawable.backgroundshape5)
                binding.linearLayout3.setBackgroundResource(R.drawable.backgroundshape5)
                binding.linearLayout4.setBackgroundResource(R.drawable.backgroundshape5)
                binding.linearLayout5.setBackgroundResource(R.drawable.backgroundshape5)
                binding.linearLayout6.setBackgroundResource(R.drawable.backgroundshape5)
            }
        }
        binding.lottieAnimationView.playAnimation()
    }

    private fun exactDate(): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun dayName(): String {
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun time(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp * 1000))
    }

    private fun showHistoryDialog() {
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(R.layout.dialog_history)
        val rvHistory = dialog.findViewById<RecyclerView>(R.id.rvHistory)
        rvHistory?.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch(Dispatchers.IO) {
            // Lấy trực tiếp từ DB (hoặc có thể viết thêm hàm trong ViewModel để lấy list này)
            val historyList = database.weatherDao().getAllWeather()

            withContext(Dispatchers.Main) {
                if (historyList.isNotEmpty()) {
                    val adapter = WeatherHistoryAdapter(historyList)
                    rvHistory?.adapter = adapter
                }
                dialog.show()
            }
        }
    }
}