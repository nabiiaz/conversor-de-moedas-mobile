package com.example.conversordemoedasapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.conversordemoedasapp.databinding.ActivityConverterBinding
import com.example.conversordemoedasapp.model.Wallet
import com.example.conversordemoedasapp.network.RetrofitClient
import com.example.conversordemoedasapp.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConverterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConverterBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var wallet: Wallet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConverterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        wallet = viewModel.wallet.value ?: Wallet()

        val moedas = listOf("BRL", "USD", "BTC")

        // Configurando os spinners
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, moedas)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerOrigem.adapter = adapter
        binding.spinnerDestino.adapter = adapter

        binding.btnConverter.setOnClickListener {
            val origem = binding.spinnerOrigem.selectedItem.toString()
            val destino = binding.spinnerDestino.selectedItem.toString()
            val valor = binding.etValor.text.toString().toDoubleOrNull()


            if (origem == destino) {
                Toast.makeText(this, "Selecione moedas diferentes.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (valor == null || valor <= 0) {
                Toast.makeText(this, "Digite um valor válido.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!temSaldo(origem, valor)) {
                Toast.makeText(this, "Saldo insuficiente.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            converterMoeda(origem, destino, valor)
        }


        binding.btnComprar.setOnClickListener {
            val currentWallet = viewModel.wallet.value ?: Wallet()

            viewModel.updateWallet(currentWallet)

            val returnIntent = Intent()
            returnIntent.putExtra("updated_real", currentWallet.real)
            returnIntent.putExtra("updated_dolar", currentWallet.dolar)
            returnIntent.putExtra("updated_bitcoin", currentWallet.bitcoin)
            setResult(RESULT_OK, returnIntent)
            finish()
        }
    }

    private fun temSaldo(origem: String, valor: Double): Boolean {
        val wallet = viewModel.wallet.value ?: return false

        return when (origem) {
            "BRL" -> wallet.real >= valor
            "USD" -> wallet.dolar >= valor
            "BTC" -> wallet.bitcoin >= valor
            else -> false
        }
    }

    private suspend fun getCotacao(origem: String, destino: String): Double? {
        val pair = "$origem-$destino"
        val response = RetrofitClient.apiService.getCurrency(pair)

        if (response.isSuccessful) {
            val currencyMap = response.body()
            val key = pair.replace("-", "")
            if (currencyMap != null && currencyMap.containsKey(key)) {
                return currencyMap[key]?.bid?.toDouble()
            }
        }

        // Tentar buscar a cotação inversa se não existir a direta
        val inversePair = "$destino-$origem"
        val inverseResponse = RetrofitClient.apiService.getCurrency(inversePair)

        if (inverseResponse.isSuccessful) {
            val inverseCurrencyMap = inverseResponse.body()
            val inverseKey = inversePair.replace("-", "")
            if (inverseCurrencyMap != null && inverseCurrencyMap.containsKey(inverseKey)) {
                val inverseBid = inverseCurrencyMap[inverseKey]?.bid?.toDouble()
                // Inverter a cotação
                if (inverseBid != null && inverseBid != 0.0) {
                    return 1 / inverseBid
                }
            }
        }

        return null // Cotação não encontrada
    }

    private fun converterMoeda(origem: String, destino: String, valor: Double) {
        binding.progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cotacao = getCotacao(origem, destino)

                if (cotacao != null) {
                    val wallet = viewModel.wallet.value ?: return@launch

                    val valorConvertido = valor * cotacao

                    // Atualiza a carteira
                    when (origem) {
                        "BRL" -> wallet.real -= valor
                        "USD" -> wallet.dolar -= valor
                        "BTC" -> wallet.bitcoin -= valor
                    }
                    when (destino) {
                        "BRL" -> wallet.real += valorConvertido
                        "USD" -> wallet.dolar += valorConvertido
                        "BTC" -> wallet.bitcoin += valorConvertido
                    }

                    withContext(Dispatchers.Main) {
                        viewModel.updateWallet(wallet)
                        binding.progressBar.visibility = View.GONE

                        binding.tvResultConversion.text =
                            "Valor convertido: %.6f $destino".format(valorConvertido)

                        Toast.makeText(
                            this@ConverterActivity,
                            "Conversão realizada com sucesso!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(
                            this@ConverterActivity,
                            "Cotação não encontrada para $origem-$destino",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@ConverterActivity,
                        "Erro: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}