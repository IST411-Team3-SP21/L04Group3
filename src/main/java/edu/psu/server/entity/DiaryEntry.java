package edu.psu.server.entity;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class DiaryEntry implements Serializable
{

    @SerializedName("body")
    @Expose
    private String body;
    @SerializedName("date")
    @Expose
    private String date;
    @SerializedName("ipAddress")
    @Expose
    private String ipAddress;

    private final static long serialVersionUID = -571960809632268581L;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public DiaryEntry withBody(String body) {
        this.body = body;
        return this;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public DiaryEntry withDate(String date) {
        this.date = date;
        return this;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public DiaryEntry withIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

}
