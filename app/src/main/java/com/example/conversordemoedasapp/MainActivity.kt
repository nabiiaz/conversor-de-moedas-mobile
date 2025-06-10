package com.example.conversordemoedasapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.conversordemoedasapp.databinding.ActivityMainBinding
import com.example.conversordemoedasapp.model.Wallet
import com.example.conversordemoedasapp.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {

    private val converterActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            if (data != null) {
                val currentWallet = viewModel.wallet.value ?: Wallet()

                val real = data.getDoubleExtra("updated_real", currentWallet.real)
                val dolar = data.getDoubleExtra("updated_dolar", currentWallet.dolar)
                val bitcoin = data.getDoubleExtra("updated_bitcoin", currentWallet.bitcoin)

                viewModel.updateWallet(Wallet(real, dolar, bitcoin))
            }
        }
    }

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Observa as mudanças na carteira
        viewModel.wallet.observe(this, Observer { wallet ->
            updateWalletUI(wallet)
        })

        // Botão para ir para a tela de conversão
        binding.btnConverter.setOnClickListener {
            val intent = Intent(this, ConverterActivity::class.java)
            converterActivityLauncher.launch(intent)
        }
    }

    private fun updateWalletUI(wallet: Wallet) {
        binding.tvReal.text = "Saldo em Real: R$ %.2f".format(wallet.real)
        binding.tvDolar.text = "Saldo em Dólar: $ %.2f".format(wallet.dolar)
        binding.tvBitcoin.text = "Saldo em Bitcoin: BTC %.4f".format(wallet.bitcoin)
    }

}