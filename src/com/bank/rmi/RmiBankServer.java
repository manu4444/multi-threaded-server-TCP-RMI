package com.bank.rmi;

import com.bank.request.Request;
import com.bank.response.Response;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RmiBankServer extends Remote {

	void init()
			throws IOException, AlreadyBoundException;

	Response sendRequest(Request request) throws RemoteException, InterruptedException;

}
