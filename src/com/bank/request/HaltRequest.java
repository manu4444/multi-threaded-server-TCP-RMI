package com.bank.request;

import java.io.Serializable;
import java.util.Comparator;

public class HaltRequest extends Request implements Serializable{

    public int clientId,serverId;

    @Override
    protected void setRequestName(String requestName) {
        this.requestName = requestName;
    }
    @Override
    public void setRequestOrigin(String requestOrigin) {
        this.requestOrigin = requestOrigin;
    }

    public HaltRequest(String requestName, String requestOrigin, int clientId)
    {
        this.setRequestName(requestName);
        this.setRequestOrigin(requestOrigin);
        this.clientId = clientId;
    }
    @Override
    public String toString() {
        return this.requestName;

    }
}