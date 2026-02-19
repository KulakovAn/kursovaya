package com.example.kursovaya

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.kursovaya.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class MainFragment : Fragment(R.layout.fragment_main) {

    private val tagLog = "KURSOVAYA_NET"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val baseInput = view.findViewById<EditText>(R.id.currencyInput)
        val targetInput = view.findViewById<EditText>(R.id.targetInput)
        val btn = view.findViewById<Button>(R.id.searchButton)
        val name = view.findViewById<TextView>(R.id.currencyName)
        val price = view.findViewById<TextView>(R.id.currencyPrice)

        btn.setOnClickListener {
            val base = baseInput.text.toString().trim().uppercase()
            val target = targetInput.text.toString().trim().uppercase()

            if (!base.matches(Regex("^[A-Z]{3}$"))) {
                Toast.makeText(requireContext(), "Введите базовую валюту из 3 букв (например USD)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!target.matches(Regex("^[A-Z]{3}$"))) {
                Toast.makeText(requireContext(), "Введите валюту назначения из 3 букв (например RUB)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            name.text = "$base → $target"
            price.text = "Загрузка..."

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val rate = withContext(Dispatchers.IO) {
                        Log.d(tagLog, "Запрос: latest/$base, достаю $target")
                        val resp = ApiClient.api.latest(base)
                        if (resp.result != "success") {
                            throw IllegalStateException("API error: ${resp.errorType ?: "unknown"}")
                        }
                        resp.rates[target]
                    }

                    price.text = if (rate == null) {
                        "Нет данных по $target"
                    } else {
                        "1 $base = %.4f $target".format(rate)
                    }

                } catch (e: HttpException) {
                    val body = e.response()?.errorBody()?.string()
                    Log.e(tagLog, "HTTP ${e.code()} body=$body", e)
                    price.text = "Ошибка HTTP ${e.code()}"
                    Toast.makeText(requireContext(), "HTTP ${e.code()}", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Log.e(tagLog, "Ошибка: ${e.message}", e)
                    price.text = "Ошибка"
                    Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
