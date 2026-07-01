package fr.spiderboy.navigoat.logic

object ApduUtils {
    const val CLA = 0x94.toByte()

    enum class Status {
        OK,
        SECURITY_NOT_OK,
        RECORD_NOT_FOUND,
        COMMAND_NOT_ALLOWED,
        FILE_NOT_FOUND,
        WRONG_PARAMETER,
        BAD_LENGTH,
        BAD_LENGTH_WITH_CORRECTION,
        HAS_RESPONSE,
        PIN_REQUIRED,
        UNKNOWN
    }

    enum class Ins(val value: Byte) {
        SELECT_FILE(0xa4.toByte()),
        SELECT_FILE_PARAM(0x08.toByte()),
        READ_RECORD(0xb2.toByte()),
        READ_RECORD_MODE(0x04.toByte())
    }

    fun createApdu(ins: Byte, args: ByteArray): ByteArray {
        val apdu = ByteArray(2 + args.size)
        apdu[0] = CLA
        apdu[1] = ins
        System.arraycopy(args, 0, apdu, 2, args.size)
        return apdu
    }

    fun toHexString(bytes: ByteArray?): String {
        return bytes?.joinToString("") { "%02x".format(it) } ?: ""
    }

    fun toBinaryString(bytes: ByteArray?): String {
        if (bytes == null || bytes.size < 2) return ""
        val resultLength = (bytes.size - 2) * 8
        val sb = StringBuilder(resultLength)
        for (i in 0 until resultLength) {
            val byteIndex = i / 8
            val bitIndex = i % 8
            val bit = ((bytes[byteIndex].toInt() and 0xFF) shr (7 - bitIndex)) and 1
            sb.append(bit)
        }
        return sb.toString()
    }

    fun getStatus(response: ByteArray?): Status {
        if (response == null || response.size < 2) return Status.UNKNOWN
        val sw1 = response[response.size - 2].toInt() and 0xFF
        val sw2 = response[response.size - 1].toInt() and 0xFF

        return when (sw1) {
            0x90 -> if (sw2 == 0x00) Status.OK else Status.UNKNOWN
            0x69 -> when (sw2) {
                0x82 -> Status.SECURITY_NOT_OK
                0x86 -> Status.COMMAND_NOT_ALLOWED
                else -> Status.UNKNOWN
            }
            0x6A -> when (sw2) {
                0x83 -> Status.RECORD_NOT_FOUND
                0x82 -> Status.FILE_NOT_FOUND
                0x86 -> Status.WRONG_PARAMETER
                else -> Status.UNKNOWN
            }
            0x67 -> Status.BAD_LENGTH
            0x6C -> Status.BAD_LENGTH_WITH_CORRECTION
            0x9F -> Status.HAS_RESPONSE
            0x98 -> if (sw2 == 0x04 || sw2 == 0x40) Status.PIN_REQUIRED else Status.UNKNOWN
            else -> Status.UNKNOWN
        }
    }
}
