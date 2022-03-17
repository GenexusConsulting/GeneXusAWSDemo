package com.genexusconsulting.iaas.helpers;

public class Contexto {
    private String DB_USER_ID;
    public String getDB_USER_ID() {
        return DB_USER_ID;
    }
    public void setDB_USER_ID(String dB_USER_ID) {
        DB_USER_ID = dB_USER_ID;
    }
   
    private String DB_URL;
    public String getDB_URL() {
        return DB_URL;
    }
    public void setDB_URL(String dB_URL) {
        DB_URL = dB_URL;
    }

    private String DATABASE_NAME;
    public String getDB_DATABASE_NAME() {
        return DATABASE_NAME;
    }
    public void setDATABASE_NAME(String dB_DATABASE_NAME) {
        DATABASE_NAME = dB_DATABASE_NAME;
    }

    private String FQDN;
    public String getFQDN() {
        return FQDN ;
    }
    public void setFQDN(String fqdn) {
        FQDN = fqdn;
    }
}
