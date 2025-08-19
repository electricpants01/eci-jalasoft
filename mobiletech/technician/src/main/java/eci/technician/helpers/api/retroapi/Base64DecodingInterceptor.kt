package eci.technician.helpers.api.retroapi


import android.os.Build
import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.InputStream


internal class Base64DecodingInterceptor : AbstractTransformingDecodingInterceptor() {

    override fun transformInputStream(inputStream: InputStream?): InputStream {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            java.util.Base64.getDecoder().wrap(inputStream)
        } else {
            val byteArray = inputStream?.readBytes()
            val decoded = Base64.decode(byteArray, Base64.DEFAULT)
            ByteArrayInputStream(decoded)
        }
    }
}
