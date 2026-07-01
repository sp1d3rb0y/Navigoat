package fr.spiderboy.navigoat.logic

import android.content.res.XmlResourceParser
import android.util.Log
import fr.spiderboy.navigoat.models.*
import org.xmlpull.v1.XmlPullParser
import java.text.SimpleDateFormat
import java.util.*

class NavigoParser(
    private val cardXml: XmlResourceParser,
    private val stationsXml: XmlResourceParser
) {
    private var cardStruct: CardNode? = null
    private val metroStations = mutableMapOf<String, String>()
    private val trainStations = mutableMapOf<String, String>()
    private var currentEventCode = ""

    init {
        parseStations()
        parseCardStruct()
    }

    private fun parseStations() {
        try {
            var eventType = stationsXml.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && stationsXml.name == "station") {
                    val type = stationsXml.getAttributeValue(null, "type")
                    val name = stationsXml.getAttributeValue(null, "name")
                    val code = stationsXml.getAttributeValue(null, "code")
                    if (type == "metro") {
                        metroStations[code] = name
                    } else {
                        trainStations[code] = name
                    }
                }
                eventType = stationsXml.next()
            }
        } catch (e: Exception) {
            Log.e("NavigoParser", "Error parsing stations: ${e.message}")
        }
    }

    private fun parseCardStruct() {
        val stack = Stack<CardNode>()
        try {
            var eventType = cardXml.eventType
            var currentNode: CardNode? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (cardXml.name == "Node") {
                            val name = cardXml.getAttributeValue(null, "name")
                            val typeStr = cardXml.getAttributeValue(null, "type")
                            val addressStr = cardXml.getAttributeValue(null, "address")
                            val sizeStr = cardXml.getAttributeValue(null, "size")
                            val finalTypeStr = cardXml.getAttributeValue(null, "final")

                            val fieldType = when (typeStr?.lowercase()) {
                                "df" -> FieldType.DF
                                "recordef" -> FieldType.RECORD_EF
                                "bitmap" -> FieldType.BITMAP
                                "final" -> FieldType.FINAL
                                "counter" -> FieldType.COUNTER
                                else -> FieldType.UNKNOWN
                            }

                            val finalType = try {
                                if (finalTypeStr != null) {
                                    FinalType.values().firstOrNull { it.name.replace("_", "").equals(finalTypeStr.replace("_", ""), ignoreCase = true) }
                                        ?: FinalType.UNKNOWN
                                } else FinalType.UNKNOWN
                            } catch (e: Exception) {
                                FinalType.UNKNOWN
                            }

                            val address = addressStr?.let {
                                val intAddr = it.toInt(16)
                                byteArrayOf((intAddr shr 8).toByte(), (intAddr and 0xFF).toByte())
                            }

                            val size = sizeStr?.toIntOrNull() ?: 0

                            currentNode = CardNode(name, fieldType, address, size, finalType)
                            stack.push(currentNode)
                        }
                    }
                    XmlPullParser.TEXT -> {
                        currentNode?.description = cardXml.text.trim()
                    }
                    XmlPullParser.END_TAG -> {
                        if (cardXml.name == "Node") {
                            val finishedNode = stack.pop()
                            if (stack.isNotEmpty()) {
                                stack.peek().addSon(finishedNode)
                                currentNode = null
                            } else {
                                cardStruct = finishedNode
                            }
                        }
                    }
                }
                eventType = cardXml.next()
            }
        } catch (e: Exception) {
            Log.e("NavigoParser", "Error parsing card structure: ${e.message}")
        }
    }

    fun parseFileData(root: CardNode, binaryData: String, startPos: Int, fileNumber: Int): Int {
        var pos = startPos
        when (root.fieldType) {
            FieldType.RECORD_EF -> {
                root.sons.forEach { son ->
                    pos += parseFileData(son, binaryData, pos, fileNumber)
                }
                return pos - startPos
            }
            FieldType.BITMAP -> {
                if (pos + root.size > binaryData.length) return 0
                val bitmap = binaryData.substring(pos, pos + root.size)
                pos += root.size
                var sonIndex = 0
                for (i in bitmap.length - 1 downTo 0) {
                    if (bitmap[i] == '1' && sonIndex < root.sons.size) {
                        pos += parseFileData(root.sons[sonIndex], binaryData, pos, fileNumber)
                    }
                    sonIndex++
                }
                return pos - startPos
            }
            FieldType.FINAL -> {
                if (pos + root.size > binaryData.length) return 0
                val value = binaryData.substring(pos, pos + root.size)
                root.setValue(value, fileNumber)
                return root.size
            }
            else -> return 0
        }
    }

    fun generateDump(uid: String): String {
        val sb = StringBuilder()
        sb.append("===============================\n")
        sb.append("UID: $uid\n")
        cardStruct?.let { sb.append(dumpNode(it, 0, 1)) }
        sb.append("===============================\n")
        return sb.toString()
    }

    private fun dumpNode(node: CardNode, level: Int, fileNumber: Int): String {
        val sb = StringBuilder()
        when (node.fieldType) {
            FieldType.DF -> {
                sb.append("${node.name}\n")
                node.sons.forEach { sb.append(dumpNode(it, level + 1, fileNumber)) }
            }
            FieldType.RECORD_EF -> {
                sb.append("${node.name}\n")
                for (i in 1..node.numberOfFiles) {
                    sb.append("=== $i ===\n")
                    node.sons.forEach { sb.append(dumpNode(it, level + 1, i)) }
                }
            }
            FieldType.BITMAP -> {
                node.sons.forEach { sb.append(dumpNode(it, level, fileNumber)) }
            }
            FieldType.FINAL -> {
                val value = node.getValue(fileNumber)
                if (value.isNotEmpty()) {
                    repeat(level) { sb.append(" ") }
                    sb.append(interpretFinal(node, fileNumber))
                }
            }
            else -> {}
        }
        return sb.toString()
    }

    private fun interpretFinal(node: CardNode, fileNumber: Int): String {
        val value = node.getValue(fileNumber)
        val res = StringBuilder(" > ${node.name}: ")
        var interpreted = ""

        try {
            when (node.finalType) {
                FinalType.DATE -> {
                    if (value.isEmpty()) {
                        interpreted = "Empty date"
                    } else {
                        val dateInt = value.toInt(2)
                        val cal = GregorianCalendar(1997, Calendar.JANUARY, 1)
                        cal.add(Calendar.DATE, dateInt)
                        interpreted = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
                    }
                }
                FinalType.TIME -> {
                    if (value.isEmpty()) {
                        interpreted = "Empty time"
                    } else {
                        val timeInt = value.toInt(2)
                        val hours = timeInt / 60
                        val mins = timeInt % 60
                        interpreted = "%02dH%02d".format(hours, mins)
                    }
                }
                FinalType.INTEGER -> {
                    interpreted = if (value.isEmpty()) "Empty integer" else value.toInt(2).toString()
                }
                FinalType.EVENT_SERVICE_PROVIDER -> {
                    val sp = value.toInt(2)
                    interpreted = when (sp) {
                        2 -> "SNCF"
                        3 -> "RATP"
                        115 -> "CSO (VEOLIA)"
                        116 -> "R'Bus (VEOLIA)"
                        156 -> "Phebus"
                        else -> "UNKNOWN"
                    }
                }
                FinalType.ROUTE_NUMBER -> {
                    val transport = currentEventCode.split(" ").firstOrNull() ?: ""
                    if (transport == "Train" && value.toInt(2) > 16) {
                        val rerChar = ('A'.toInt() + value.toInt(2) - 17).toChar()
                        interpreted = "RER $rerChar"
                    } else {
                        val line = value.toInt(2)
                        interpreted = if (line == 103) "Ligne 3 bis" else "Ligne $line"
                    }
                }
                FinalType.AMOUNT -> {
                    val amount = value.toInt(2)
                    interpreted = "%.2f euros".format(amount / 100.0)
                }
                FinalType.EVENT_DEVICE -> {
                    val transport = currentEventCode.split(" ").firstOrNull() ?: ""
                    if (transport != "Bus") {
                        interpreted = value.toInt(2).toString()
                    } else {
                        val device = value.substring(8).toInt(2)
                        val door = (device / 2) + 1
                        interpreted = if (device % 2 == 0) "Porte $door, validateur droite" else "Porte $door, validateur gauche"
                    }
                }
                FinalType.EVENT_RESULT -> {
                    interpreted = when (value.toInt(2)) {
                        48 -> "Double validation en entrée"
                        49 -> "Zone invalide"
                        53 -> "Abonnement périmé"
                        69 -> "Double validation en sortie"
                        else -> "Unknown"
                    }
                }
                FinalType.EVENT_CODE -> {
                    val mode = value.substring(0, 4).toInt(2)
                    val modeStr = when (mode) {
                        1 -> "Bus urbain : "
                        2 -> "Bus interurbain : "
                        3 -> "Métro : "
                        4 -> "Tram : "
                        5 -> "Train : "
                        8 -> "Parking : "
                        else -> "Unknown : "
                    }
                    val transaction = value.substring(4, 8).toInt(2)
                    val transStr = when (transaction) {
                        0x1 -> "Validation en entrée"
                        0x2 -> "Validation en sortie"
                        0x4 -> "Contrôle volant (à bord)"
                        0x5 -> "Validation de test"
                        0x6 -> "Validation en correspondance (entrée)"
                        0x7 -> "Validation en correspondance (sortie)"
                        0x9 -> "Annulation de validation"
                        0xD -> "Distribution"
                        0xF -> "Invalidation"
                        else -> "Unknown"
                    }
                    interpreted = modeStr + transStr
                    currentEventCode = interpreted
                }
                FinalType.LOCATION_ID -> {
                    val transport = currentEventCode.split(" ").firstOrNull() ?: ""
                    val zone = value.substring(0, 7).toInt(2)
                    val location = value.substring(7, 12).toInt(2)
                    val code = "%02d-%02d".format(zone, location)
                    interpreted = when (transport) {
                        "Train" -> trainStations[code] ?: "Unknown Train Station ($code)"
                        "Bus" -> "Arrêt bus ${value.toInt(2)}"
                        else -> metroStations[code] ?: "Unknown Metro Station ($code)"
                    }
                }
                else -> interpreted = value
            }
        } catch (e: Exception) {
            interpreted = "Error: ${value}"
        }

        node.setInterpretedValue(interpreted, fileNumber)
        res.append(interpreted).append("\n")
        return res.toString()
    }

    fun getContracts(): List<NavigoContract> {
        val root = findNode(cardStruct, "Contracts") ?: run {
            Log.e("NavigoParser", "Contracts node not found")
            return emptyList()
        }
        val res = mutableListOf<NavigoContract>()
        val contractSn = findNode(root, "ContractSerialNumber")
        val contractPrice = findNode(root, "ContractPriceAmount")
        val contractStart = findNode(root, "ContractValidityStartDate")
        val contractEnd = findNode(root, "ContractValidityEndDate")

        Log.d("NavigoParser", "Processing contracts, root records: ${root.numberOfFiles}")
        if (contractStart != null && contractEnd != null) {
            for (i in 1..root.numberOfFiles) {
                var title = contractSn?.getInterpretedValue(i) ?: "No UID"
                if (title.isEmpty()) {
                    continue
                }
                var content = "Du ${contractStart.getInterpretedValue(i)} au ${contractEnd.getInterpretedValue(i)}"
                contractPrice?.getInterpretedValue(i)?.takeIf { it.isNotEmpty() }?.let {
                    content += " ($it)"
                }
                res.add(NavigoContract(title, content))
            }
        }
        return res
    }

    fun getEvents(): List<NavigoEvent> {
        val root = findNode(cardStruct, "EventLog") ?: run {
            Log.e("NavigoParser", "EventLog node not found")
            return emptyList()
        }
        val res = mutableListOf<NavigoEvent>()
        val lines = findNode(root, "EventRouteNumber")
        val stations = findNode(root, "EventLocationId")
        val dates = findNode(root, "EventDateStamp")
        val times = findNode(root, "EventTimeStamp")

        Log.d("NavigoParser", "Processing events, root records: ${root.numberOfFiles}")
        if (lines != null && stations != null && dates != null && times != null) {
            for (i in 1..root.numberOfFiles) {
                val lineText = lines.getInterpretedValue(i)
                res.add(NavigoEvent(
                    stations.getInterpretedValue(i),
                    "Le ${dates.getInterpretedValue(i)} à ${times.getInterpretedValue(i)}",
                    lineText,
                    if (lineText == "Ligne 3 bis") "Ligne 15" else null
                ))
            }
        }
        return res
    }

    fun getSpecialEvents(): List<NavigoEvent> {
        val root = findNode(cardStruct, "Special Events") ?: return emptyList()
        val res = mutableListOf<NavigoEvent>()
        val result = findNode(root, "EventResult")
        val lines = findNode(root, "EventRouteNumber")
        val dates = findNode(root, "EventDateStamp")
        val times = findNode(root, "EventTimeStamp")

        if (lines != null && result != null && dates != null && times != null) {
            for (i in 1..root.numberOfFiles) {
                val lineText = lines.getInterpretedValue(i)
                res.add(NavigoEvent(
                    result.getInterpretedValue(i),
                    "Le ${dates.getInterpretedValue(i)} à ${times.getInterpretedValue(i)}",
                    lineText,
                    if (lineText == "Ligne 3 bis") "Ligne 15" else null
                ))
            }
        }
        return res
    }

    fun getCardStruct() = cardStruct

    private fun findNode(n: CardNode?, name: String): CardNode? {
        if (n == null) return null
        if (n.name == name) return n
        n.sons.forEach { son ->
            findNode(son, name)?.let { return it }
        }
        return null
    }
}
