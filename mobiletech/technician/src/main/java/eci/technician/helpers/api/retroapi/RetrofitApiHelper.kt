package eci.technician.helpers.api.retroapi

import android.util.Base64
import android.util.Log
import com.google.gson.GsonBuilder
import eci.technician.BuildConfig
import eci.technician.helpers.AppAuth
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitApiHelper {
    private const val TAG = "RetrofitApiHelper"
    private const val EXCEPTION = "Exception"
    var temporalToken = ""
    private var instanceMain: MobileTechApi? = null
    private var instanceGPS: MobileTechApi? = null
    private var instanceLicenseLogin: MobileTechLoginApi? = null


    fun getApi(): MobileTechApi? {
        if (instanceMain != null) return instanceMain
        instanceMain = getApiWithURL(AppAuth.getInstance().serverAddress)
        return instanceMain
    }

    fun getGPSApi(): MobileTechApi? {
        if (instanceGPS != null) return instanceGPS
        instanceGPS = getApiWithURL(AppAuth.getInstance().gpsServer)
        return instanceGPS

    }

    fun getLoginApi(baseUrl: String, userName: String, password: String): MobileTechLoginApi? {
        if (instanceLicenseLogin != null) return instanceLicenseLogin
        instanceLicenseLogin = getLoginApiWithURL(baseUrl, userName, password)
        return instanceLicenseLogin
    }

    private fun getApiWithURL(baseUrl: String): MobileTechApi? {
        var instance: MobileTechApi? = null
        val interceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
            this.level = HttpLoggingInterceptor.Level.HEADERS
        }

        val okHttpClientBuilder = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request =
                    chain.request()
                        .newBuilder()
                        .addHeader("Authorization", "Bearer ${AppAuth.getInstance().token}")
                        .build()
                chain.proceed(request)
            }
        if (BuildConfig.DEBUG) {
            okHttpClientBuilder.addInterceptor(interceptor)
        }

        val okHttpClient = okHttpClientBuilder
            .addInterceptor(GzipDecodingInterceptor())
            .addInterceptor(Base64DecodingInterceptor())
            .build()
        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS Z").create()
        try {
            instance = Retrofit
                .Builder()
                .baseUrl("$baseUrl/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(MobileTechApi::class.java)
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
        return if (instance != null) {
            instance as MobileTechApi
        } else null

    }

    fun setApiToNull() {
        instanceMain = null
        instanceGPS = null
    }

    private fun getLoginApiWithURL(
        baseUrl: String, userName: String, password: String
    ): MobileTechLoginApi? {
        var instance: MobileTechLoginApi? = null
        val interceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
            this.level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClientBuilder = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request =
                    chain.request()
                        .newBuilder()
                        .addHeader("Authorization", getAuthorization(userName, password))
                        .build()
                chain.proceed(request)
            }
        if (BuildConfig.DEBUG) {
            okHttpClientBuilder.addInterceptor(interceptor)
        }

        val okHttpClient = okHttpClientBuilder
            .addInterceptor(GzipDecodingInterceptor())
            .addInterceptor(Base64DecodingInterceptor())
            .build()
        val gson = GsonBuilder().create()
        try {
            instance = Retrofit
                .Builder()
                .baseUrl("$baseUrl/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(MobileTechLoginApi::class.java)
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        }
        return instance
    }

    private fun getAuthorization(userName: String, password: String): String {
        val credentialsString = "$userName:$password"
        return "Basic " + Base64.encodeToString(credentialsString.toByteArray(), Base64.NO_WRAP)
    }

}