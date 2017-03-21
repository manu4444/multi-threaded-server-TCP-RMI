package com.bank.rmi;

import com.bank.request.*;
import com.bank.response.*;
import com.bank.socket.*;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.rmi.AlreadyBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import com.bank.*;
import com.bank.*;
public class RmiBankServerImpl extends UnicastRemoteObject implements RmiBankServer, Runnable {
	private static Hashtable<Integer, Account> accounts = new Hashtable<Integer, Account>();
	private Request clientRequest;
	private static PrintWriter writer;
	private List<Response> responseList = null;;

	public RmiBankServerImpl(Request request, List<Response> responseList) throws RemoteException {
		this.clientRequest = request;
		this.responseList = responseList;
	}

	public RmiBankServerImpl() throws RemoteException, FileNotFoundException, UnsupportedEncodingException {
		writer = new PrintWriter("severLogfile");
	}

	public Response sendRequest(Request request) throws RemoteException, InterruptedException {
		RmiBankServerImpl server = new RmiBankServerImpl(request, new ArrayList<Response>());
		Thread thread = new Thread(server);
		thread.start();
		thread.join();
		return server.responseList.get(0);
	}

	public void init(int portNumber)
			throws RemoteException, AlreadyBoundException, FileNotFoundException, UnsupportedEncodingException {

		System.setSecurityManager(new RMISecurityManager());
		RmiBankServerImpl bankServer = new RmiBankServerImpl();
		LocateRegistry.createRegistry( Registry.REGISTRY_PORT );
		Registry localRegistry = LocateRegistry.getRegistry(portNumber);
		localRegistry.rebind("RmiBankServer", bankServer);
	}

	@Override
	public void run() {
		if (clientRequest instanceof CreateAccountRequest) {
			Response response = createAccount((CreateAccountRequest) clientRequest);
			responseList.add(response);
		}
		if (clientRequest instanceof DepositRequest) {
			Response response = depositAmount((DepositRequest) clientRequest);
			responseList.add(response);
		}
		if (clientRequest instanceof TransferRequest) {
			Response response = transferAmount((TransferRequest) clientRequest);
			responseList.add(response);
		}
		if (clientRequest instanceof GetBalanceRequest) {
			Response response = checkBalance((GetBalanceRequest) clientRequest);
			responseList.add(response);
		}
	}

	private synchronized Response checkBalance(GetBalanceRequest clientRequest) {
		BalanceResponse response = null;
		if (accounts.containsKey(clientRequest.getUid())) {
			response = new BalanceResponse(accounts.get(clientRequest.getUid()).balance);
		}
		log(clientRequest, response);
		return response;
	}

	private synchronized void log(Request clientRequest, Response serverResponse) {
		writer.println("Request:{" + clientRequest + "}" + "  Response:{" + serverResponse + "}");
		writer.flush();
	}

	private synchronized Response transferAmount(TransferRequest clientRequest) {
		TransferResponse response = null;
		if (accounts.containsKey(clientRequest.getSourceUid())
				&& accounts.containsKey(clientRequest.getDestinationUid())
				&& accounts.get(clientRequest.getSourceUid()).balance >= clientRequest.getAmount()) {
			{
				accounts.get(clientRequest.getSourceUid()).balance -= clientRequest.getAmount();
				accounts.get(clientRequest.getDestinationUid()).balance += clientRequest.getAmount();
				response = new TransferResponse("OK");
			}
		} else
			response = new TransferResponse("FAILED");
		log(clientRequest, response);
		return response;
	}

	private synchronized Response depositAmount(DepositRequest clientRequest) {
		DepositResponse response;
		if (accounts.containsKey(clientRequest.getUid())) {
			accounts.get(clientRequest.getUid()).balance += clientRequest.getAmount();
			response = new DepositResponse("OK");
		} else
			response = new DepositResponse("FAILED");
		log(clientRequest, response);
		return response;
	}

	private synchronized Response createAccount(CreateAccountRequest clientRequest) {
		int accountNumber = generateAccountId();
		accounts.put(accountNumber, new Account(accountNumber));
		CreateAccountResponse response = new CreateAccountResponse(accountNumber);
		log(clientRequest, response);
		return response;
	}

	public int generateAccountId() {
		while (accounts.containsKey((int) System.currentTimeMillis())) {
		}
		return (int) System.currentTimeMillis();
	}

	public static void main(String args[])
			throws RemoteException, AlreadyBoundException, FileNotFoundException, UnsupportedEncodingException {
		RmiBankServerImpl server = new RmiBankServerImpl();
		server.init(Integer.parseInt(args[0]));
	}
}
