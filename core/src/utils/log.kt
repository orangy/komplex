package komplex.utils

import org.slf4j.Logger

import java.io.IOException

// taken from http://stackoverflow.com/questions/11187461/redirect-system-out-and-system-err-to-slf4j

public enum class LogLevel {
    ERROR,
    WARNING,
    INFO,
    DEBUG,
    TRACE
}

public class LogOutputStream(val log: Logger, val level: LogLevel) : java.io.OutputStream() {

    protected var hasBeenClosed: Boolean = false

    protected var buf: ByteArray = ByteArray(DEFAULT_BUFFER_LENGTH)

    protected var count: Int = 0

    private var bufLength: Int = DEFAULT_BUFFER_LENGTH

    override fun close() {
        flush()
        hasBeenClosed = true
    }

    /**
     * Writes the specified byte to this output stream. The general contract
     * for `write` is that one byte is written to the output
     * stream. The byte to be written is the eight low-order bits of the
     * argument `b`. The 24 high-order bits of `b` are
     * ignored.

     * @param b
     * *            the `byte` to write
     */
    throws(IOException::class)
    override fun write(b: Int) {
        if (hasBeenClosed) throw IOException("The stream has been closed.")

        // don't log nulls
        if (b == 0) return

        // would this be writing past the buffer?
        if (count == bufLength) {
            // grow the buffer
            val newBufLength = bufLength + DEFAULT_BUFFER_LENGTH
            val newBuf = ByteArray(newBufLength)

            System.arraycopy(buf, 0, newBuf, 0, bufLength)

            buf = newBuf
            bufLength = newBufLength
        }

        buf[count] = b.toByte()
        count++
    }

    /**
     * Flushes this output stream and forces any buffered output bytes to be
     * written out. The general contract of `flush` is that
     * calling it is an indication that, if any bytes previously written
     * have been buffered by the implementation of the output stream, such
     * bytes should immediately be written to their intended destination.
     */
    override fun flush() {

        if (count == 0) return

        // don't print out blank lines; flushing from PrintStream puts out these
        if (count == LINE_SEPERATOR.length() &&
            ((buf[0].toChar()) == LINE_SEPERATOR.charAt(0) && ((count == 1) || // <- Unix & Mac,
             ((count == 2) && (buf[1].toChar()) == LINE_SEPERATOR.charAt(1))))) // <- Windows
        {
            reset()
            return
        }

        val theBytes = ByteArray(count)

        System.arraycopy(buf, 0, theBytes, 0, count)

        when (level) {
            LogLevel.ERROR -> log.error(String(theBytes))
            LogLevel.WARNING -> log.warn(String(theBytes))
            LogLevel.INFO -> log.info(String(theBytes))
            LogLevel.DEBUG -> log.debug(String(theBytes))
            LogLevel.TRACE -> log.trace(String(theBytes))
        }

        reset()
    }

    private fun reset() {
        // not resetting the buffer -- assuming that if it grew that it
        // will likely grow similarly again
        count = 0
    }

    companion object {
        protected val LINE_SEPERATOR: String = System.getProperty("line.separator")
        public val DEFAULT_BUFFER_LENGTH: Int = 2048
    }
}

