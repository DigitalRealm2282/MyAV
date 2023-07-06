package com.digitalrealm.shellsec.urlscanner

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ScanURLAPI {
    @GET("_result/{id}")
    fun getScanResults(@Path("id") id: String?): Call<Any?>?

    @POST("virustotal_report")
    fun getScanResultID(@Body body: ScanRequestBody?): Call<String?>?

    companion object {
        const val BASE_URL = "https://www.circl.lu/urlabuse/"
    }
}