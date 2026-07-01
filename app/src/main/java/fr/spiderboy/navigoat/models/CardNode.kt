package fr.spiderboy.navigoat.models

enum class FieldType {
    DF,
    RECORD_EF,
    BITMAP,
    FINAL,
    COUNTER,
    UNKNOWN
}

enum class FinalType {
    UNKNOWN,
    DATE,
    TIME,
    ZONES,
    APPLICATION_VERSION_NUMBER,
    AMOUNT,
    PAY_METHOD,
    BEST_CONTRACT_TARIFF,
    SPECIAL_EVENT_SERIOUSNESS,
    EVENT_CODE,
    EVENT_SERVICE_PROVIDER,
    INTEGER,
    EVENT_RESULT,
    ROUTE_NUMBER,
    LOCATION_ID,
    TRAIN_STATION_ID,
    EVENT_DEVICE,
    HOLDER_DATA_CARD_STATUS
}

data class CardNode(
    val name: String,
    val fieldType: FieldType,
    val address: ByteArray? = null,
    val size: Int = 0,
    val finalType: FinalType = FinalType.UNKNOWN,
    var description: String = ""
) {
    val sons = mutableListOf<CardNode>()
    private val values = mutableMapOf<Int, String>()
    private val interpretedValues = mutableMapOf<Int, String>()
    var numberOfFiles: Int = 0

    fun addSon(son: CardNode) {
        sons.add(son)
    }

    fun setValue(value: String, fileNumber: Int) {
        values[fileNumber] = value
    }

    fun getValue(fileNumber: Int): String {
        return values[fileNumber] ?: ""
    }

    fun setInterpretedValue(value: String, fileNumber: Int) {
        interpretedValues[fileNumber] = value
    }

    fun getInterpretedValue(fileNumber: Int): String {
        return interpretedValues[fileNumber] ?: ""
    }

    override fun toString(): String {
        return "CardNode(name='$name', fieldType=$fieldType, size=$size, finalType=$finalType)"
    }
}
