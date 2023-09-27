package com.enrih.folkbluetoothcompanion.constants;

public enum Statuses {
    CONNECTING_STATUS(1),
    MESSAGE_READ(2),
    PERMISSIONS_REQUEST_CODE(100);




    public final int code;
    Statuses(final int newValue) {
        code = newValue;
    }

    public int getCode(){
        return this.code;
    }
}
