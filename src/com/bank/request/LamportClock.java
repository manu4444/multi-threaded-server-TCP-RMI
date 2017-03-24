package com.bank.request;

import java.io.Serializable;

/**
 * Created by maste on 3/22/2017.
 */
public class LamportClock implements Serializable{
    public int timestamp;
    public int serverId;


    public LamportClock(int timestamp, int serverId){
        this.timestamp = timestamp;
        this.serverId = serverId;
    }

    public int getServerId() {
        return serverId;
    }

    public  String toString() {
        return "["+Integer.toString(timestamp)+","+Integer.toString(serverId)+"]";
    }


}