package com.example.rest;

import java.util.List;

public class RestDto {
    List<RestRecord> records;
    String name;

    public List<RestRecord> getRecords() {
        return records;
    }

    public void setRecords(List<RestRecord> records) {
        this.records = records;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
