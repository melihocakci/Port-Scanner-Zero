package com.yildiz.scanner;

public class Port {
    public int number;
    public String state;
    public String service;

    public Port(int number, String state, String service) {
        this.number = number;
        this.state = state;
        this.service = service;
    }
}
