package com.example.conversordemoedasapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.conversordemoedasapp.model.Wallet

class MainViewModel : ViewModel() {

    private val _wallet = MutableLiveData<Wallet>().apply {
        value = Wallet() // Carteira inicial
    }
    val wallet: LiveData<Wallet> = _wallet

    fun updateWallet(newWallet: Wallet) {
        _wallet.value = newWallet
    }
}