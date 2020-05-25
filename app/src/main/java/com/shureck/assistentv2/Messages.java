package com.shureck.assistentv2;

public class Messages implements RowType {

    private String name;
    private String company;
    private int image;
    private boolean ask;

    public Messages(String name, String company, int image, boolean ask){

        this.name=name;
        this.company = company;
        this.image = image;
        this.ask = ask;
    }

    public boolean isAsk() {
        return this.ask;
    }

    public void setState(boolean ask) {
        this.ask = ask;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompany() {
        return this.company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public int getImage() {
        return this.image;
    }

    public void setImage(int image) {
        this.image = image;
    }
}