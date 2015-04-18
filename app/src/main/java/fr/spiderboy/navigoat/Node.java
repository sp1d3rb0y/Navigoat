package fr.spiderboy.navigoat;

import java.util.ArrayList;

/**
 * Created by pbni on 4/17/15.
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

    public Node(String n, Navigo.FieldType feType, byte[] add) {
        name = n;
        fieldType = feType;
        address = add;
        content = new ArrayList<Node>();
        value = new ArrayList<String>();
    }

    public Node(String n, Navigo.FieldType feType, int size) {
        name = n;
        fieldType = feType;
        this.size = size;
        content = new ArrayList<Node>();
        value = new ArrayList<String>();
    }

    public Node(String n, Navigo.FieldType feType, int size, Navigo.FinalType faType) {
        name = n;
        fieldType = feType;
        this.size = size;
        content = new ArrayList<Node>();
        finalType = faType;
        value = new ArrayList<String>();
    }

    public void setValue(String val, int file_number) {
        if (value.size() < file_number) {
            value.add(val);
            this.number_of_files++;
        } else {
            value.set(file_number - 1, val);
        }
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
}