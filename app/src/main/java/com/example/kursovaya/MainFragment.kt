package com.example.kursovaya

import android.os.Bundle
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

class MainFragment : Fragment(R.layout.fragment_main) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val input = view.findViewById<EditText>(R.id.currencyInput)
        val btn = view.findViewById<Button>(R.id.searchButton)
        val name = view.findViewById<TextView>(R.id.currencyName)
        val price = view.findViewById<TextView>(R.id.currencyPrice)

        btn.setOnClickListener {
            val code = input.text.toString().trim().uppercase()

            if (code.length < 3) {
                Toast.makeText(requireContext(), "Введите код валюты, например USD", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            name.text = code
            price.text = "Загрузка..."

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val rate = withContext(Dispatchers.IO) {
                        ApiClient.api.convert(from = code).info?.rate
                    }

                    if (rate == null) {
                        price.text = "Нет данных"
                    } else {
                        price.text = "Курс: %.2f".format(rate)
                    }
                } catch (e: Exception) {
                    price.text = "Ошибка"
                    Toast.makeText(requireContext(), "Ошибка запроса: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
