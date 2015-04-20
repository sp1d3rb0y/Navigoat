package fr.spiderboy.navigoat;

import android.content.res.XmlResourceParser;
import android.nfc.tech.IsoDep;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Created by spiderboy on 4/15/15.
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
    }

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
    }

    private int id = 0;
    private IsoDep iso;
    private Map<String, String> stations;
    private Node card_struct = null;
    private XmlResourceParser xmlparser_card;
    private XmlResourceParser xmlparser_stations;
    private String dump = "";

    public Navigo(byte[] nid, XmlResourceParser parser_card, XmlResourceParser parser_stations) {
        id = new BigInteger(nid).intValue();
        xmlparser_card = parser_card;
        xmlparser_stations = parser_stations;
        fillCardStruct();
        stations = new HashMap<String, String> ();
        fillStations();
    }

    private void fillStations() {
        try {
            String node = "";
            int event = xmlparser_stations.getEventType();

            while (event != XmlPullParser.END_DOCUMENT) {
                switch (event) {
                    case XmlPullParser.START_TAG:
                        node = xmlparser_stations.getName();
                        if (node.equals("station")) {
                            /// TODO : Handle train type
                            String name = xmlparser_stations.getAttributeValue(null, "name");
                            String code = xmlparser_stations.getAttributeValue(null, "code");
                            stations.put(code, name);
                        }
                        break;
                }
                event = xmlparser_stations.next();
            }
        } catch (Exception e) {
            Log.e(MainActivity.dTag, "Error parsing stations XML file: " + e.getMessage());
        }
    }

    private void fillCardStruct() {
        Stack<Node> stack = new Stack<Node>();
        String node = "";
        Node current = null;

        try {
            int event = xmlparser_card.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                switch (event) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.TEXT:
                        current.setDescription(xmlparser_card.getText());
                        break;
                    case XmlPullParser.START_TAG:
                        node = xmlparser_card.getName();
                        if (node.equals("Node")) {
                            String name = xmlparser_card.getAttributeValue(null, "name");
                            String type = xmlparser_card.getAttributeValue(null, "type");
                            String address = xmlparser_card.getAttributeValue(null, "address");
                            String size = xmlparser_card.getAttributeValue(null, "size");
                            String finalType = xmlparser_card.getAttributeValue(null, "final");
                            if (address == null) {
                                if (finalType == null) {
                                    current = new Node(name, type, Integer.parseInt(size));
                                } else {
                                    current = new Node(name, type, Integer.parseInt(size), finalType);
                                }
                            } else {
                                current = new Node(name, type, address);
                            }
                            stack.push(current);
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        node = xmlparser_card.getName();
                        if (node.equals("Node")) {
                            Node n = stack.pop();
                            if (stack.size() > 0) {
                                stack.peek().addSon(n);
                                current = null;
                            } else {
                                card_struct = n;
                            }
                        }
                        break;
                }
                event = xmlparser_card.next();
            }
        } catch (Exception e) {
            Log.e(MainActivity.dTag, "Error parsing card structure XML file: " + e.getMessage());
        }
    }

    public String getId() {
        return "0" + id;
    }

    public void dump() {
        String res = "===============================\n";
        res += "UID: " + getId() + "\n";
        res += dumpNode(card_struct, 0, 1);
        res += "===============================\n";
        dump = res;
    }

    public String getDump() {
        return this.dump;
    }

    public ArrayList<String> getElements() {
        ArrayList<String> res = new ArrayList<String> ();
        if (card_struct != null) {
            res.add("UID: " + getId());
            Node lignes = findNode(card_struct, "EventRouteNumber");
            if (lignes != null) {
                for (int i = 1; i <= lignes.getNumber_of_files(); ++i)
                    res.add(lignes.getInterpretedValue(i));
            }
        }
        return res;
    }

    private Node findNode(Node n, String name) {
        if (n != null) {
            if (n.getName().equals(name)) {
                return n;
            }
            for (Node son : n.getSons()) {
                Node res = findNode(son, name);
                if (res != null) {
                    return res;
                }
            }
        }
        return null;
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
                if (!n.getValue(file_number).equals("")) {
                    for (int i = 0; i < level; i++)
                        res += " ";
                    res += dumpFinal(n, file_number);
                }
                break;
            default:
                break;
        }
        return res;
    }

    /// TODO : Interpreter class with static functions
    /// TODO : SetInterpreted value for each node and final type
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
                n.setInterpretedValue(sdf.format(cal.getTime()), file_number);
                res += n.getInterpretedValue(file_number);
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
                int ligne = Integer.parseInt(value, 2);
                if (ligne == 103)
                    n.setInterpretedValue("Ligne 3 bis", file_number);
                else
                    n.setInterpretedValue("Ligne " + ligne, file_number);
                res += n.getInterpretedValue(file_number);
                break;
            case AMOUNT:
                float amount = Integer.parseInt(value, 2);
                res += amount / 100.0;
                res += " euros";
                break;
            case EVENT_RESULT:
                int result = Integer.parseInt(value, 2);
                switch (result) {
                    case 48:
                        res += "Double validation en entrée";
                        break;
                    case 49:
                        res += "Zone invalide";
                        break;
                    case 53:
                        res += "Abonnement périmé";
                        break;
                    case 69:
                        res += "Double validation en sortie";
                        break;
                    default:
                        res += "Unkown";
                        break;
                }
                break;
            case LOCATION_ID:
                int zone = Integer.parseInt(value.substring(0,7), 2);
                int location = Integer.parseInt(value.substring(7,12), 2);
                String code = (zone < 10) ? "0" : "";
                code += zone + "-";
                code += (location < 10) ? "0" : "";
                code += location;
                res += stations.get(code);
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
            Log.e(MainActivity.dTag, "Exception during parse node: " + e.toString());
        }
    }

    private void parseNode(Node n, byte[] addr) throws IOException {
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
                if (pos + n.getSize() >= res.length())
                    return 0;
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
                if (pos + n.getSize() >= res.length())
                    return 0;
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
