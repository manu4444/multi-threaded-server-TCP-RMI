package com.bank.socket;

import com.bank.request.*;
import com.bank.response.*;
import com.bank.socket.Account;

import java.net.*;
import java.util.Hashtable;


import java.io.*;

public class BankServer extends Thread {
	private Socket clientSocket;
	private static Hashtable<Integer, Account> accounts;
	private String hostName, portNumber;
	private ServerSocket server;
	private static PrintWriter writer;
	private boolean stop = false;

	BankServer(Socket socket) {
		this.clientSocket = socket;
	}

	public void stopServer() {
		stop = true;
	}

	public BankServer(String hostName, String portName) throws FileNotFoundException {
		this.hostName = hostName;
		this.portNumber = portName;
		accounts = new Hashtable<Integer, Account>();
		writer = new PrintWriter("severLogfile");
	}

	public void run() {
		try {
			ObjectInputStream inputReader = new ObjectInputStream(clientSocket.getInputStream());
			ObjectOutputStream ostream = new ObjectOutputStream(clientSocket.getOutputStream());
			Request clientRequest = (Request) inputReader.readObject();
			if (clientRequest instanceof CreateAccountRequest) {
				Response response = createAccount((CreateAccountRequest) clientRequest);
				ostream.writeObject(response);
			} else if (clientRequest instanceof DepositRequest) {
				Response response = depositAmount((DepositRequest) clientRequest);
				ostream.writeObject(response);
			} else if (clientRequest instanceof TransferRequest) {
				Response response = transferAmount((TransferRequest) clientRequest);
				ostream.writeObject(response);
			} else if (clientRequest instanceof GetBalanceRequest) {
				Response response = checkBalance((GetBalanceRequest) clientRequest);
				ostream.writeObject(response);
			} else
				writer.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				clientSocket.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

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
		DepositResponse response = null;
		if (accounts.containsKey(clientRequest.getUid())) {
			accounts.get(clientRequest.getUid()).balance += clientRequest.getAmount();
			response = new DepositResponse("OK");
		} else
			response = new DepositResponse("FAILED");
		log(clientRequest, response);
		return response;
	}

	private synchronized Response createAccount(CreateAccountRequest clientRequest) {
		int accountId = generateAccountId();
		accounts.put(accountId, new Account(accountId));
		CreateAccountResponse response = new CreateAccountResponse(accountId);
		log(clientRequest, response);
		return response;
	}

	private int generateAccountId() {
		while (accounts.containsKey((int) System.currentTimeMillis())) {
		}
		return (int) System.currentTimeMillis();
	}

	private synchronized Response checkBalance(GetBalanceRequest clientRequest) {
		BalanceResponse response = null;
		if (accounts.containsKey(clientRequest.getUid())) {
			response = new BalanceResponse(accounts.get(clientRequest.getUid()).balance);
		}
		log(clientRequest, response);
		return response;
	}

	protected void startServer() throws NumberFormatException, IOException {
		server = new ServerSocket(Integer.parseInt(portNumber));
		while (true && stop == false) {
			Socket client = server.accept();
			BankServer bankServer = new BankServer(client);
			bankServer.start();
		}
	}

	private synchronized void log(Request clientRequest, Response serverResponse) {
		writer.println("Request:{" + clientRequest + "}" + "  Response:{" + serverResponse + "}");
		writer.flush();
	}

	public static void main(String args[]) throws NumberFormatException, IOException {
		BankServer server = new BankServer("localhost", args[0]);
		server.startServer();
	}
}
