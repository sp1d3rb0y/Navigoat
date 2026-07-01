package fr.spiderboy.navigoat.logic

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcB
import android.os.Build
import android.util.Log
import fr.spiderboy.navigoat.models.CardNode
import fr.spiderboy.navigoat.models.FieldType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NfcManager(private val adapter: NfcAdapter?) {

    suspend fun readCard(tag: Tag, parser: NavigoParser): String = withContext(Dispatchers.IO) {
        val isoDep = IsoDep.get(tag) ?: return@withContext "Not an IsoDep tag"
        
        try {
            isoDep.connect()
            isoDep.timeout = 5000
            
            val cardStruct = parser.getCardStruct() ?: return@withContext "Card structure not initialized"
            
            parseNode(isoDep, cardStruct, byteArrayOf(), parser)
            
            val uid = ApduUtils.toHexString(tag.id)
            parser.generateDump(uid)
        } catch (e: Exception) {
            Log.e("NfcManager", "Error reading card: ${e.message}")
            "Error reading card: ${e.message}"
        } finally {
            try {
                isoDep.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private suspend fun parseNode(isoDep: IsoDep, node: CardNode, parentAddr: ByteArray, parser: NavigoParser) {
        when (node.fieldType) {
            FieldType.DF -> {
                node.sons.forEach { son ->
                    parseNode(isoDep, son, node.address ?: parentAddr, parser)
                }
            }
            FieldType.RECORD_EF -> {
                val nodeAddr = node.address ?: return
                val fullAddr = parentAddr + nodeAddr
                
                val selectArgs = byteArrayOf(
                    ApduUtils.Ins.SELECT_FILE_PARAM.value,
                    0x00.toByte(),
                    fullAddr.size.toByte()
                ) + fullAddr
                
                val selectResult = sendApdu(isoDep, ApduUtils.Ins.SELECT_FILE.value, selectArgs)
                val selectStatus = ApduUtils.getStatus(selectResult)
                Log.d("NfcManager", "Selecting EF ${node.name} at ${ApduUtils.toHexString(fullAddr)}: $selectStatus")
                if (selectStatus == ApduUtils.Status.OK || selectStatus == ApduUtils.Status.HAS_RESPONSE) {
                    var fileNumber = 1
                    var readResult: ByteArray?
                    
                    while (true) {
                        val readArgs = byteArrayOf(
                            fileNumber.toByte(),
                            ApduUtils.Ins.READ_RECORD_MODE.value,
                            0x00.toByte()
                        )
                        
                        readResult = sendApdu(isoDep, ApduUtils.Ins.READ_RECORD.value, readArgs)
                        var status = ApduUtils.getStatus(readResult)
                        
                        if (status == ApduUtils.Status.BAD_LENGTH_WITH_CORRECTION) {
                            val correctedArgs = byteArrayOf(
                                fileNumber.toByte(),
                                ApduUtils.Ins.READ_RECORD_MODE.value,
                                readResult!![1]
                            )
                            readResult = sendApdu(isoDep, ApduUtils.Ins.READ_RECORD.value, correctedArgs)
                            status = ApduUtils.getStatus(readResult)
                        }
                        
                        Log.d("NfcManager", "Read record $fileNumber of ${node.name}: $status")
                        if (status == ApduUtils.Status.RECORD_NOT_FOUND) {
                            break
                        }
                        
                        if (status == ApduUtils.Status.OK || status == ApduUtils.Status.HAS_RESPONSE) {
                            val binaryData = ApduUtils.toBinaryString(readResult)
                            parser.parseFileData(node, binaryData, 0, fileNumber)
                            fileNumber++
                        } else {
                            break
                        }
                    }
                    node.numberOfFiles = fileNumber - 1
                    Log.d("NfcManager", "Finished reading ${node.name}: ${node.numberOfFiles} records")
                }
            }
            else -> {}
        }
    }

    private suspend fun sendApdu(isoDep: IsoDep, ins: Byte, args: ByteArray): ByteArray {
        val apdu = ApduUtils.createApdu(ins, args)
        return isoDep.transceive(apdu)
    }

    companion object {
        fun setupForegroundDispatch(activity: Activity, adapter: NfcAdapter?) {
            if (adapter == null) return
            
            val intent = Intent(activity, activity.javaClass).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            
            val pendingIntent = PendingIntent.getActivity(activity, 0, intent, flags)
            
            val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED))
            val techList = arrayOf(arrayOf(IsoDep::class.java.name, NfcB::class.java.name))
            
            adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList)
        }

        fun stopForegroundDispatch(activity: Activity, adapter: NfcAdapter?) {
            adapter?.disableForegroundDispatch(activity)
        }
    }
}
