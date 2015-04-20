package fr.spiderboy.navigoat;

import java.util.ArrayList;

/**
 * Created by spiderboy on 4/17/15.
 */
public class Node {
    private String name;
    private Navigo.FieldType fieldType;
    private Navigo.FinalType finalType;
    private int size = 0;
    private int number_of_files = 0;
    private byte[] address;
    private ArrayList<Node> content;
    private ArrayList<String> value;
    private ArrayList<String> interpretedValue;
    private String description;

    public Node(String n, String feType, int size) {
        name = n;
        this.size = size;
        parseFeType(feType);
        content = new ArrayList<Node>();
        value = new ArrayList<String>();
        interpretedValue = new ArrayList<String>();
    }

    public Node(String n, String feType, int size, String faType) {
        name = n;
        this.size = size;
        parseFeType(feType);
        parseFaType(faType);
        content = new ArrayList<Node>();
        value = new ArrayList<String>();
        interpretedValue = new ArrayList<String>();
    }

    public Node(String n, String feType, String addr) {
        name = n;
        parseFeType(feType);
        content = new ArrayList<Node>();
        value = new ArrayList<String>();
        interpretedValue = new ArrayList<String>();
        /// Parse hex string
        int len = addr.length();
        this.address = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            address[i / 2] = (byte) ((Character.digit(addr.charAt(i), 16) << 4)
                    + Character.digit(addr.charAt(i+1), 16));
        }
    }

    public void setValue(String val, int file_number) {
        if (value.size() < file_number) {
            value.add(val);
            this.number_of_files++;
        } else {
            value.set(file_number - 1, val);
        }
    }

    public void setInterpretedValue(String val, int file_number) {
        if (interpretedValue.size() < this.number_of_files)
            interpretedValue.addAll(value);
        interpretedValue.set(file_number - 1, val);
    }

    private void parseFeType(String feType) {
        if (feType.equals("DF"))
            fieldType = Navigo.FieldType.DF;
        else if (feType.equals("RecordEF"))
            fieldType = Navigo.FieldType.RECORD_EF;
        else if (feType.equals("Bitmap"))
            fieldType = Navigo.FieldType.BITMAP;
        else if (feType.equals("Final"))
            fieldType = Navigo.FieldType.FINAL;
        else if (feType.equals("Counter"))
            fieldType = Navigo.FieldType.COUNTER;
        else if (feType.equals("DFName"))
            fieldType = Navigo.FieldType.DF_NAME;
        else if (feType.equals("DFList"))
            fieldType = Navigo.FieldType.DF_LIST;
        else if (feType.equals("TransparentEF"))
            fieldType = Navigo.FieldType.TRANSPARENT_EF;
        else if (feType.equals("FinalRepeated"))
            fieldType = Navigo.FieldType.FINAL_REPEATED;
        else if (feType.equals("StructRepeated"))
            fieldType = Navigo.FieldType.STRUCT_REPEATED;
        else if (feType.equals("ReversedStructRepeated"))
            fieldType = Navigo.FieldType.REVERSED_STRUCT_REPEATED;
        else if (feType.equals("FinalWithHeader"))
            fieldType = Navigo.FieldType.FINAL_WITH_HEADER;
    }

    private void parseFaType(String faType) {
        if (faType.equals("Unknown"))
            finalType = Navigo.FinalType.UNKNOWN;
        else if (faType.equals("Date"))
            finalType = Navigo.FinalType.DATE;
        else if (faType.equals("Time"))
            finalType = Navigo.FinalType.TIME;
        else if (faType.equals("Zones"))
            finalType = Navigo.FinalType.ZONES;
        else if (faType.equals("ApplicationVersionNumber"))
            finalType = Navigo.FinalType.APPLICATION_VERSION_NUMBER;
        else if (faType.equals("Amount"))
            finalType = Navigo.FinalType.AMOUNT;
        else if (faType.equals("PayMethod"))
            finalType = Navigo.FinalType.PAY_METHOD;
        else if (faType.equals("BestContractTariff"))
            finalType = Navigo.FinalType.BEST_CONTRACT_TARIFF;
        else if (faType.equals("SpecialEventSeriousness"))
            finalType = Navigo.FinalType.SPECIAL_EVENT_SERIOUSNESS;
        else if (faType.equals("EventCode"))
            finalType = Navigo.FinalType.EVENT_CODE;
        else if (faType.equals("EventServiceProvider"))
            finalType = Navigo.FinalType.EVENT_SERVICE_PROVIDER;
        else if (faType.equals("Integer"))
            finalType = Navigo.FinalType.INTEGER;
        else if (faType.equals("EventResult"))
            finalType = Navigo.FinalType.EVENT_RESULT;
        else if (faType.equals("RouteNumber"))
            finalType = Navigo.FinalType.ROUTE_NUMBER;
        else if (faType.equals("LocationId"))
            finalType = Navigo.FinalType.LOCATION_ID;
        else if (faType.equals("TrainStationId"))
            finalType = Navigo.FinalType.TRAIN_STATION_ID;
        else if (faType.equals("EventDevice"))
            finalType = Navigo.FinalType.EVENT_DEVICE;
        else if (faType.equals("HolderDataCardStatus"))
            finalType = Navigo.FinalType.HOLDER_DATA_CARD_STATUS;
    }

    public void addSon(Node n) {
        content.add(n);
    }

    public String getName() {
        return name;
    }

    public ArrayList<Node> getSons() {
        return content;
    }

    /// file_number starts at 1
    public String getValue(int file_number) {
        if (value.size() < file_number) {
            return "";
        }
        return value.get(file_number - 1);
    }

    public String getInterpretedValue(int file_number) {
        if (value.size() < file_number)
            return "";
        return interpretedValue.get(file_number - 1);
    }

    public Navigo.FieldType getFieldType() {
        return fieldType;
    }

    public byte[] getAddress() {
        return address;
    }

    public int getSize() {
        return size;
    }

    public void setNumber_of_files(int num) {
        number_of_files = num;
    }

    public int getNumber_of_files() {
        return number_of_files;
    }

    public Navigo.FinalType getFinalType() {
        return finalType;
    }

    public void setDescription(String desc) {
        description = desc;
    }

    public String getDescription() {
        return description;
    }
}