package fr.spiderboy.navigoat;

import android.nfc.tech.IsoDep;
import android.util.Log;

import java.io.IOException;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by pbni on 4/15/15.
 */
public class Navigo {

    public static enum FieldType {
        DF,
        RECORD_EF,
        BITMAP,
        FINAL,
        COUNTER,
        DF_NAME,
        DF_LIST,
        TRANSPARENT_EF,
        FINAL_REPEATED,
        STRUCT_REPEATED,
        REVERSED_STRUCT_REPEATED,
        FINAL_WITH_HEADER,
    };

    public static enum FinalType {
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
        HOLDER_DATA_CARD_STATUS,
    };

    private int id = 0;
    private IsoDep iso;

    private Node card_struct;

    public Navigo(byte[] nid) {
        id = new BigInteger(nid).intValue();
        card_struct = new Node("Calypso DF", FieldType.DF, new byte[]{0x20, 0x00});
        ///////////////
        /// Environment
        ///////////////
        Node env = new Node("Environment", FieldType.RECORD_EF, new byte[]{0x20, 0x01});
        Node envAppVersNumber = new Node("EnvApplicationVersionNumber", FieldType.FINAL, 6, FinalType.APPLICATION_VERSION_NUMBER);
        Node envBitMapGnrl = new Node("Bitmap generale", FieldType.BITMAP, 7);
        Node envNetworkId = new Node("EnvNetworkId", FieldType.FINAL, 24, FinalType.INTEGER);
        Node envAppIssuerId = new Node("EnvApplicationIssuerId", FieldType.FINAL, 8, FinalType.INTEGER);
        Node envAppValidEndDate = new Node("EnvApplicationValidityEndDate", FieldType.FINAL, 14, FinalType.DATE);
        Node envPayMethod = new Node("EnvPayMethod", FieldType.FINAL, 11, FinalType.PAY_METHOD);
        Node envAuthenticator = new Node("EnvAuthenticator", FieldType.FINAL, 16, FinalType.INTEGER);
        Node envSelectList = new Node("EnvSelectList", FieldType.FINAL, 32, FinalType.UNKNOWN);
        Node envData = new Node("EnvData", FieldType.BITMAP, 2);
        Node envDataCardStatus = new Node("EnvDataCardStatus", FieldType.FINAL, 1, FinalType.UNKNOWN);
        Node envData2 = new Node("EnvData2", FieldType.FINAL, 0, FinalType.UNKNOWN);
        envData.addSon(envDataCardStatus);
        envData.addSon(envData2);
        envBitMapGnrl.addSon(envNetworkId);
        envBitMapGnrl.addSon(envAppIssuerId);
        envBitMapGnrl.addSon(envAppValidEndDate);
        envBitMapGnrl.addSon(envPayMethod);
        envBitMapGnrl.addSon(envAuthenticator);
        envBitMapGnrl.addSon(envSelectList);
        envBitMapGnrl.addSon(envData);
        env.addSon(envAppVersNumber);
        env.addSon(envBitMapGnrl);
        /// TODO : Add struct Holder
        card_struct.addSon(env);
        ////////////
        /// EventLog
        ////////////
        Node events = new Node("EventLog", FieldType.RECORD_EF, new byte[]{0x20, 0x10});
        Node evDateStamp = new Node("EventDateStamp", FieldType.FINAL, 14, FinalType.DATE);
        Node evTimeStamp = new Node("EventTimeStamp", FieldType.FINAL, 11, FinalType.TIME);
        Node evBitmap = new Node("EventBitmap", FieldType.BITMAP, 28);
        Node evDisplayData = new Node("EventDisplayData",FieldType.FINAL, 8, FinalType.UNKNOWN);
        Node evNetworkId = new Node("EventNetworkId", FieldType.FINAL, 24, FinalType.INTEGER);
        Node evCode = new Node("EventCode", FieldType.FINAL, 8, FinalType.EVENT_CODE);
        Node evResult = new Node("EventResult", FieldType.FINAL, 8, FinalType.EVENT_RESULT);
        Node evServiceProvider = new Node("EventServiceProvider", FieldType.FINAL, 8, FinalType.EVENT_SERVICE_PROVIDER);
        Node evNotOkCounter = new Node("EventNotOkCounter", FieldType.FINAL, 8, FinalType.UNKNOWN);
        Node evSerialNumber = new Node("EventSerialNumber", FieldType.FINAL, 24, FinalType.UNKNOWN);
        Node evDestination = new Node("EventDestination", FieldType.FINAL, 16, FinalType.UNKNOWN);
        Node evLocationId = new Node("EventLocationId", FieldType.FINAL, 16, FinalType.LOCATION_ID);
        Node evLocationGate = new Node("EventLocationGate", FieldType.FINAL, 8, FinalType.UNKNOWN);
        Node evDevice = new Node("EventDevice", FieldType.FINAL, 16, FinalType.EVENT_DEVICE);
        Node evRouteNumber = new Node("EventRouteNumber", FieldType.FINAL, 16, FinalType.ROUTE_NUMBER);
        Node evRouteVariant = new Node("EventRouteVariant", FieldType.FINAL, 8, FinalType.UNKNOWN);
        Node evJourneyRun = new Node("EventJourneyRun", FieldType.FINAL, 16, FinalType.INTEGER);
        Node evVehicleId = new Node("EventVehicleId", FieldType.FINAL, 16, FinalType.INTEGER);
        Node evVehicleClass = new Node("EventVehicleClass", FieldType.FINAL, 8, FinalType.UNKNOWN);
        Node evLocationType = new Node("EventLocationType", FieldType.FINAL, 5, FinalType.UNKNOWN);
        Node evEmployee = new Node("EventEmployee", FieldType.FINAL, 240, FinalType.UNKNOWN);
        Node evLocationReference = new Node("EventLocationReference", FieldType.FINAL, 16, FinalType.UNKNOWN);
        Node evJourneyInterchanges = new Node("EventJourneyInterchanges", FieldType.FINAL, 8, FinalType.UNKNOWN);
        Node evPeriodJourneys = new Node("EventPeriodJourneys", FieldType.FINAL, 16, FinalType.UNKNOWN);
        Node evTotalJourneys = new Node("EventTotalJourneys", FieldType.FINAL, 16, FinalType.UNKNOWN);
        Node evJourneyDistance = new Node("EventJourneyDistance", FieldType.FINAL, 16, FinalType.UNKNOWN);
        Node evPriceAmount = new Node("EventPriceAmount", FieldType.FINAL, 16, FinalType.UNKNOWN);
        Node evPriceUnit = new Node("EventPriceUnit", FieldType.FINAL, 16, FinalType.UNKNOWN);
        Node evContractPointer = new Node("EventContractPointer", FieldType.FINAL, 5, FinalType.INTEGER);
        Node evAuthenticator = new Node("EventAuthenticator", FieldType.FINAL, 16, FinalType.UNKNOWN);
        Node evData = new Node("EventData", FieldType.BITMAP, 5);
        Node evDataDateFirstStamp = new Node("EventDataDateFirstStamp", FieldType.FINAL, 14, FinalType.DATE);
        Node evDataTimeFirstStamp = new Node("EventDataTimeFirstStamp", FieldType.FINAL, 11, FinalType.TIME);
        Node evDataSimulation = new Node("EventDataSimulation", FieldType.FINAL, 1, FinalType.UNKNOWN);
        Node evDataTrip = new Node("EventDataTrip", FieldType.FINAL, 2, FinalType.UNKNOWN);
        Node evDataRouteDirection = new Node("EventDataRouteDirection", FieldType.FINAL, 2, FinalType.UNKNOWN);
        evData.addSon(evDataDateFirstStamp);
        evData.addSon(evDataTimeFirstStamp);
        evData.addSon(evDataSimulation);
        evData.addSon(evDataTrip);
        evData.addSon(evDataRouteDirection);
        evBitmap.addSon(evDisplayData);
        evBitmap.addSon(evNetworkId);
        evBitmap.addSon(evCode);
        evBitmap.addSon(evResult);
        evBitmap.addSon(evServiceProvider);
        evBitmap.addSon(evNotOkCounter);
        evBitmap.addSon(evSerialNumber);
        evBitmap.addSon(evDestination);
        evBitmap.addSon(evLocationId);
        evBitmap.addSon(evLocationGate);
        evBitmap.addSon(evDevice);
        evBitmap.addSon(evRouteNumber);
        evBitmap.addSon(evRouteVariant);
        evBitmap.addSon(evJourneyRun);
        evBitmap.addSon(evVehicleId);
        evBitmap.addSon(evVehicleClass);
        evBitmap.addSon(evLocationType);
        evBitmap.addSon(evEmployee);
        evBitmap.addSon(evLocationReference);
        evBitmap.addSon(evJourneyInterchanges);
        evBitmap.addSon(evPeriodJourneys);
        evBitmap.addSon(evTotalJourneys);
        evBitmap.addSon(evJourneyDistance);
        evBitmap.addSon(evPriceAmount);
        evBitmap.addSon(evPriceUnit);
        evBitmap.addSon(evContractPointer);
        evBitmap.addSon(evAuthenticator);
        evBitmap.addSon(evData);
        events.addSon(evDateStamp);
        events.addSon(evTimeStamp);
        events.addSon(evBitmap);
        card_struct.addSon(events);
        /////////////
        /// Contracts
        /////////////
        Node contracts = new Node("Contracts", FieldType.RECORD_EF, new byte[]{0x20, 0x20});
        card_struct.addSon(contracts);
    }

    public String getId() {
        return "0" + id;
    }

    public String dump() {
        String res = "===============================\n";
        res += "UID: " + getId() + "\n";
        res += dumpNode(card_struct, 0, 1);
        res += "===============================\n";
        return res;
    }

    private String dumpNode(Node n, int level, int file_number) {
        String res = "";

        switch (n.getFieldType()) {
            case DF:
                res += n.getName() + "\n";
                for (Node son : n.getSons()) {
                    res += dumpNode(son, level + 1, file_number);
                }
                break;
            case RECORD_EF:
                res += n.getName() + "\n";
                for (int i = 1; i < n.getNumber_of_files(); i++) {
                    res += "=== " + i + " ===\n";
                    for (Node son : n.getSons()) {
                        res += dumpNode(son, level + 1, i);
                    }
                }
                break;
            case BITMAP:
                for (Node son : n.getSons()) {
                    res += dumpNode(son, level, file_number);
                }
                break;
            case FINAL:
                for (int i = 0; i < level; i++) {
                    res += " ";
                }
                if (n.getValue(file_number) != "") {
                    res += dumpFinal(n, file_number);
                }
                break;
            default:
                break;
        }
        return res;
    }

    private String dumpFinal(Node n, int file_number) {
        String value = n.getValue(file_number);
        String res = " > " + n.getName() + ": ";

        switch (n.getFinalType()) {
            case DATE:
                if (value.length() == 0) {
                    res += "Empty date";
                    break;
                }
                int date_int = Integer.parseInt(value, 2);
                Calendar cal = GregorianCalendar.getInstance();
                cal.set(1997, Calendar.JANUARY, 1);
                cal.add(Calendar.DATE, date_int);
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                res += sdf.format(cal.getTime());
                break;
            case TIME:
                if (value.length() == 0) {
                    res += "Empty time";
                    break;
                }
                int time_int = Integer.parseInt(value, 2);
                if (time_int / 60 < 10)
                    res += "0";
                res += time_int / 60;
                res += "H";
                if (time_int % 60 < 10)
                    res += "0";
                res += time_int % 60;
                break;
            case INTEGER:
                if (value.length() == 0) {
                    res += "Empty integer";
                    break;
                }
                res += Integer.parseInt(value, 2);
                break;
            case EVENT_SERVICE_PROVIDER:
                int sp = Integer.parseInt(value, 2);
                switch (sp) {
                    case 2:
                        res += "SNCF";
                        break;
                    case 3:
                        res += "RATP";
                        break;
                    case 115:
                        res += "CSO (VEOLIA)";
                        break;
                    case 116:
                        res += "R'Bus (VEOLIA)";
                        break;
                    case 156:
                        res += "Phebus";
                        break;
                    default:
                        res += "UNKOWN";
                        break;
                }
                break;
            case ROUTE_NUMBER:
                /// TODO : Parse RER value
                Log.i(MainActivity.dTag, value);
                int ligne = Integer.parseInt(value, 2);
                Log.i(MainActivity.dTag, Integer.toString(ligne));
                if (ligne == 103)
                    res += "Ligne 3 bis";
                else
                    res += "Ligne " + ligne;
                break;
            default:
                res += value;
                break;
        }
        res += "\n";
        return res;
    }

    public void parseIsoDep(IsoDep iso) {
        this.iso = iso;
        try {
            parseNode(card_struct, new byte[]{});
        } catch (Exception e) {
            Log.e(MainActivity.dTag, "Exception during parse node: " + e.getMessage());
        }
    }

    private void parseNode(Node n, byte[] addr) throws IOException {
        Log.i(MainActivity.dTag, "Parsing node " + n.getName());
        switch (n.getFieldType()) {
            case DF:
                for (Node son : n.getSons()) {
                    parseNode(son, n.getAddress());
                }
                break;
            case RECORD_EF:
                byte[] args = {APDU.ins.SELECT_FILE_PARAM.getValue(), /// PARAM1
                                0x00, /// PARAM2 is 0
                                (byte) (n.getAddress().length + addr.length), // ADDR LENGTH
                                addr[0], // DF ADDR
                                addr[1],
                                n.getAddress()[0], // RECORD ADDR
                                n.getAddress()[1]
                };
                // Send select EF
                byte[] result = sendAPDU(APDU.ins.SELECT_FILE.getValue(), args);
                if (APDU.getStatus(result) == APDU.status.OK) {
                    Log.i(MainActivity.dTag, "Select RECORD EF OK");
                    n.setValue(APDU.toString(result), 1);

                    int file_number = 1;
                    args = new byte[]{
                            (byte) file_number,
                            APDU.ins.READ_RECORD_MODE.getValue(),
                            0x00
                    };
                    // Read each file
                    while (APDU.getStatus(result) != APDU.status.RECORD_NOT_FOUND) {
                        args[0] = (byte) file_number;
                        result = sendAPDU(APDU.ins.READ_RECORD.getValue(), args);
                        if (APDU.getStatus(result) == APDU.status.BAD_LENGTH_WITH_CORRECTION) {
                            args[2] = result[1]; // size send by the badge
                            continue;
                        }
                        if (APDU.getStatus(result) != APDU.status.RECORD_NOT_FOUND) {
                            /// Set nodes value according to result
                            parseFileRecord(n, APDU.toBinaryString(result), 0, file_number);
                        }
                        file_number++;
                    }
                    n.setNumber_of_files(file_number - 1);
                } else {
                    Log.e(MainActivity.dTag, "Select RECORD EF KO");
                }
                break;
            default:
                break;
        }
    }

    private int parseFileRecord(Node n, String res, int pos, int file_number) {
        switch (n.getFieldType()) {
            case RECORD_EF:
                for (Node son : n.getSons()) {
                    pos += parseFileRecord(son, res, pos, file_number);
                }
                return 0;
            case BITMAP:
                String bitmap = res.substring(pos, pos + n.getSize());
                ArrayList<Node> sons = n.getSons();
                int j = 0;
                pos += n.getSize();
                for (int i = bitmap.length() - 1; i >= 0; i--) {
                    if (bitmap.charAt(i) == '1') {
                        pos += parseFileRecord(sons.get(j), res, pos, file_number);
                    } // else, record is empty
                    j++;
                }
                return pos;
            case FINAL:
                String val = res.substring(pos, pos + n.getSize());
                n.setValue(val, file_number);
                return n.getSize();
            default:
                return 0;
        }
    }

    private byte[] sendAPDU(byte ins, byte[] args) throws IOException {
        APDU ap = new APDU(ins, args);
        Log.i(MainActivity.dTag, "Sending APDU >>> " + ap.toString());
        byte[] result = iso.transceive(ap.getValue());
        Log.i(MainActivity.dTag, "Receive APDU <<< " + APDU.toString(result));
        return result;
    }
}
