package com.devreis.businesscard.setcolor

import android.text.TextUtils
import android.util.Log
import android.util.SparseArray
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask

object Logger {
    private const val DEFAULT_TAG = "TopDefaultsLogger"
    private var tagPrefix = DEFAULT_TAG
    /**
     * Get the log level, which reuse the definition of [Log], [Log.VERBOSE],
     * [Log.DEBUG], etc.
     *
     * @return The log level
     */
    /**
     * Set the log level, which reuse the definition of [Log], [Log.VERBOSE],
     * [Log.DEBUG], etc.
     *
     * @param level The log level
     */
    private var level = Log.VERBOSE
    private lateinit var logFilePath: String
    private var logFileSizeInMegabytes = 2
    private const val prevLogFileSuffix = "-prev"
    private var logWriter: BufferedWriter? = null
    private var executorService: ExecutorService? = null
    private var timer: Timer? = null
    private var scheduledFlushTask: TimerTask? = null
    private var scheduledCloseTask: TimerTask? = null

    /**
     * Set the prefix of log tag, the real log tag will be determined at runtime, which
     * will contain the file and line info.
     *
     * @param tagPrefix The prefix of log tag
     */
    fun setTagPrefix(tagPrefix: String) {
        Logger.tagPrefix = tagPrefix
    }

    /**
     * Set the path for log file，we will keep at most two log files (one with the suffix "-prev")
     * and the file size will be limited at [.setLogFileMaxSizeInMegabytes], if one
     * file exceed the limit, the following logs will be written to the other one.
     *
     * Default value is null, which means the logs will not be saved to file.
     *
     * Caveat: Please make sure you have the WRITE_EXTERNAL_STORAGE permission when needed.
     *
     * @param filePath Path for log file
     */
    fun setLogFile(filePath: String?) {
        if (filePath != null) {
            logFilePath = filePath
        }
        if (filePath == null) {
            executorService!!.shutdown()
            timer!!.cancel()
            return
        }
        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor()
        }
        if (timer == null) {
            timer = Timer()
        }
    }

    /**
     * The maximum size in megabytes a log file can be.
     *
     * @param sizeInMegabytes The size of log file in megabytes
     */
    fun setLogFileMaxSizeInMegabytes(sizeInMegabytes: Int) {
        logFileSizeInMegabytes = sizeInMegabytes
    }

    fun v(message: String, vararg args: Any?) {
        vWithTag(realTag(), message, *args)
    }

    fun d(message: String, vararg args: Any?) {
        dWithTag(realTag(), message, *args)
    }

    fun i(message: String, vararg args: Any?) {
        iWithTag(realTag(), message, *args)
    }

    fun w(message: String, vararg args: Any?) {
        wWithTag(realTag(), message, *args)
    }

    fun e(message: String, vararg args: Any?) {
        eWithTag(realTag(), message, *args)
    }

    fun wtf(message: String, vararg args: Any?) {
        wtfWithTag(realTag(), message, *args)
    }

    private fun vWithTag(tag: String, message: String, vararg args: Any?) {
        log(Log.VERBOSE, tag, message, args)
    }

    private fun dWithTag(tag: String, message: String, vararg args: Any?) {
        log(Log.DEBUG, tag, message, args)
    }

    private fun iWithTag(tag: String, message: String, vararg args: Any?) {
        log(Log.INFO, tag, message, args)
    }

    private fun wWithTag(tag: String, message: String, vararg args: Any?) {
        log(Log.WARN, tag, message, args)
    }

    private fun eWithTag(tag: String, message: String, vararg args: Any?) {
        log(Log.ERROR, tag, message, args)
    }

    private fun wtfWithTag(tag: String, message: String, vararg args: Any?) {
        log(Log.ASSERT, tag, message, args)
    }

    fun logThreadStart() {
        dWithTag(realTag(), ">>>>>>>> " + Thread.currentThread().javaClass + " start running >>>>>>>>")
    }

    fun logThreadFinish() {
        dWithTag(realTag(), "<<<<<<<< " + Thread.currentThread().javaClass + " finished running <<<<<<<<")
    }

    private fun log(priority: Int, tag: String, message: String, vararg args: Array<out Any?>) {
        var message = message
        if (level > priority && !Log.isLoggable(tagPrefix, Log.DEBUG)) return
        message = formatMessage(message, arrayOf(args))
        if (priority == Log.ASSERT) {
            Log.wtf(tag, message)
        } else {
            Log.println(priority, tag, message)
        }
        try {
            writeLogFile(priorityAbbr(priority) + "/" + tag + "\t" + message)
        } catch (ignored: Exception) {
        } // fail silent
    }

    private val PRIORITY_MAP = SparseArray<String>()
    private fun priorityAbbr(priority: Int): String {
        return PRIORITY_MAP[priority]
    }

    /**
     * A convenient method which can be used to adapt to [Timber](https://github.com/JakeWharton/timber).
     *
     * @param priority The priority of the log
     * @param customTag The custom tag of the log, the real log tag will be determined at runtime, which
     * will contain the file and line info
     * @param message The log message
     */
    fun logWithTimber(priority: Int, customTag: String, message: String) {
        val tag = realTimberTag(customTag)
        when (priority) {
            Log.VERBOSE -> vWithTag(tag, message)
            Log.DEBUG -> dWithTag(tag, message)
            Log.INFO -> iWithTag(tag, message)
            Log.WARN -> wWithTag(tag, message)
            Log.ERROR -> eWithTag(tag, message)
            Log.ASSERT -> wtfWithTag(tag, message)
            else -> {
            }
        }
    }

    private fun formatMessage(message: String, args: Array<Any>?): String {
        var message = message
        if (args != null && args.isNotEmpty()) {
            message = String.format(message, *args)
        }
        return message
    }

    private fun writeLogFile(message: String) {
        val sdfDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        val strDate = sdfDate.format(Date())
        val line = "$strDate $message"
        executorService!!.submit(LogWriterRunnable(line))
        if (scheduledFlushTask != null) {
            scheduledFlushTask!!.cancel()
        }
        scheduledFlushTask = object : TimerTask() {
            override fun run() {
                val flushTask = FutureTask<Void> {
                    logWriter!!.flush()
                    null
                }
                executorService!!.submit(flushTask)
            }
        }
        timer!!.schedule(scheduledFlushTask, 1000)
        if (scheduledCloseTask != null) {
            scheduledCloseTask!!.cancel()
        }
        scheduledCloseTask = object : TimerTask() {
            override fun run() {
                val closeTask = FutureTask<Void> {
                    logWriter!!.close()
                    logWriter = null
                    null
                }
                executorService!!.submit(closeTask)
            }
        }
        timer!!.schedule(scheduledCloseTask, (1000 * 60).toLong())
    }

    private fun realTag(): String {
        return "$tagPrefix|$lineInfo"
    }

    private fun realTimberTag(timberTag: String): String {
        return (if (TextUtils.isEmpty(timberTag)) tagPrefix else timberTag) + "|" + lineInfoBypassTimber
    }

    /**
     * The magic number 5 is determined because of the stack trace:
     *
     * <pre> StackTraceElement[]:
     * 0 = {java.lang.StackTraceElement@5260} "dalvik.system.VMStack.getThreadStackTrace(Native Method)"
     * 1 = {java.lang.StackTraceElement@5261} "java.lang.Thread.getStackTrace(Thread.java:1556)"
     * 2 = {java.lang.StackTraceElement@5262} "top.defaults.logger.Logger.getLineInfo(Logger.java:164)"
     * 3 = {java.lang.StackTraceElement@5262} "top.defaults.logger.Logger.realTag(Logger.java:166)"
     * 4 = {java.lang.StackTraceElement@5263} "top.defaults.logger.Logger.d(Logger.java:60)"
     * 5 = {java.lang.StackTraceElement@5264} ... // the caller
    </pre> *
     *
     * So the real call site's StackTraceElement is stackTraceElement[4], then we use this stackTraceElement
     * to retrieve the line info.
     *
     * @return The line info which will be used as log tag, for example:
     */
    private val lineInfo: String
        get() {
            val stackTraceElement = Thread.currentThread().stackTrace
            val fileName = stackTraceElement[5].fileName
            val lineNumber = stackTraceElement[5].lineNumber
            return ".($fileName:$lineNumber)"
        }
    private val lineInfoBypassTimber: String
        get() {
            val stackTraceElement = Thread.currentThread().stackTrace
            val offset = getStackOffsetBypassTimber(stackTraceElement)
            if (offset < 0) return ""
            val fileName = stackTraceElement[offset].fileName
            val lineNumber = stackTraceElement[offset].lineNumber
            return ".($fileName:$lineNumber)"
        }

    private fun getStackOffsetBypassTimber(stackTraceElements: Array<StackTraceElement>): Int {
        for (i in 6 until stackTraceElements.size) {
            val e = stackTraceElements[i]
            val name = e.className
            if (!name.startsWith("timber.log.Timber")) {
                return i
            }
        }
        return -1
    }

    private class LogWriterRunnable(private val line: String) : Runnable {
        override fun run() {
            try {
                if (logWriter == null) {
                    logWriter = BufferedWriter(FileWriter(logFilePath, true))
                }
                logWriter!!.append(line)
                logWriter!!.newLine()
            } catch (ignored: IOException) {
            }
            val currentFile = File(logFilePath)
            if (currentFile.length() >= logFileSizeInMegabytes * 1024 * 1024) {
                // delete previous log file and change current log file to prev
                val prevFile = File(logFilePath + prevLogFileSuffix)
                currentFile.renameTo(prevFile)
                try {
                    logWriter!!.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                logWriter = null
            }
        }
    }

    init {
        PRIORITY_MAP.append(Log.VERBOSE, "V")
        PRIORITY_MAP.append(Log.DEBUG, "D")
        PRIORITY_MAP.append(Log.INFO, "I")
        PRIORITY_MAP.append(Log.WARN, "W")
        PRIORITY_MAP.append(Log.ERROR, "E")
        PRIORITY_MAP.append(Log.ASSERT, "X")
    }
}