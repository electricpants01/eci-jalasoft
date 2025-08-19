package eci.technician.helpers.api.retroapi

import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream

internal class GzipDecodingInterceptor :
    AbstractTransformingDecodingInterceptor() {
    @Throws(IOException::class)
    override fun transformInputStream(inputStream: InputStream?): InputStream {
        return GZIPInputStream(inputStream)
    }


}