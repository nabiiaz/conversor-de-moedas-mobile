package com.example.conversordemoedasapp.network

import com.example.conversordemoedasapp.model.Currency
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @GET("json/last/{pair}")
    suspend fun getCurrency(
        @Path("pair") pair: String
    ): Response<Map<String, Currency>>
}

