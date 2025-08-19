package eci.technician.helpers.api.retroapi
import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.buffer
import okio.source
import java.io.IOException
import java.io.InputStream
internal abstract class AbstractTransformingDecodingInterceptor : Interceptor {
    @Throws(IOException::class)
    protected abstract fun transformInputStream(inputStream: InputStream?): InputStream?
    companion object{
        const val TAG ="AbstractTransformingDecodingInterceptor"
        const val EXCEPTION ="EXCEPTION"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val body = response.body
        return if (body != null) {
            try {
                if (response.headers["Content-Type"] == "application/octet-stream") {
                    response.newBuilder()
                        .body(
                            transformInputStream(body.byteStream())?.let { it.source().buffer() }
                                ?.let {
                                    it
                                        .asResponseBody(
                                            body?.contentType(),
                                            body?.contentLength()
                                        )
                                }
                        )
                        .build()
                } else {
                    response
                }
            } catch (e: Exception) {
                Log.e(TAG, EXCEPTION, e)
                response
            }
        } else {
            response
        }
    }
}