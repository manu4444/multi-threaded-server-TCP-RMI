package com.bank.rmi;

/**
 * Created by maste on 3/26/2017.
 */
class ServerDetail{
    String hostname;
    int rmiPort;
    int id;

    ServerDetail(int id, String hostname, int rmiPort){
        this.hostname = hostname;
        this.rmiPort = rmiPort;
        this.id = id;
    }
}
