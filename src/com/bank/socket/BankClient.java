package com.bank.socket;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import com.bank.request.CreateAccountRequest;
import com.bank.request.DepositRequest;
import com.bank.request.GetBalanceRequest;
import com.bank.request.TransferRequest;
import com.bank.response.*;
import com.bank.rmi.*;

public class BankClient extends Thread {
	protected static String host;
	protected static int port;
	protected DataInputStream in;
	protected DataOutputStream out;
	protected String operationName;
	protected static Hashtable<Integer, Integer> accountWithBalance = new Hashtable<Integer, Integer>();
	protected static int threadCount, iterationCount;
	protected static List<Integer> accountIds = new Vector<Integer>();
	protected static BankServer server = null;
	private static PrintWriter writer;

	public static void setThreadCount(int threadCount) {
		BankClient.threadCount = threadCount;
	}

	public static void setIterationCount(int iterationCount) {
		BankClient.iterationCount = iterationCount;
	}

	public BankClient(String operationName) {
		this.operationName = operationName;
	}

	public BankClient() {
	}

	public static void setHost(String host) {
		BankClient.host = host;
	}

	public static void setPort(int port) {
		BankClient.port = port;
	}

	public static void main(String args[]) throws NumberFormatException, IOException, InterruptedException {
		BankClient.setHost(args[0]);
		BankClient.setPort(Integer.parseInt(args[1]));
		BankClient.setThreadCount(Integer.parseInt(args[2]));
		BankClient.setIterationCount(Integer.parseInt(args[3]));
		BankClient client = new BankClient();
		writer = new PrintWriter("clientLogfile");
		// client.startServer();
		for (int i = 0; i < 100; i++) {
			CreateAccountResponse response = (CreateAccountResponse) client.createAccount();
			accountWithBalance.put(response.getUid(), 0);
		}
		for (Integer accountId : accountWithBalance.keySet()) {
			DepositResponse response = (DepositResponse) client.depositAmount(accountId, 100);
		}
		client.checkTotalBalanceInAllAccounts(client, false);
		client.createAcoountIdList();
		client.runMultipleClients();
		System.out.println("After multiple transfer of $10");
		client.checkTotalBalanceInAllAccounts(client, false);
		writer.close();
		System.exit(0);
	}

	private int checkTotalBalanceInAllAccounts(BankClient client, boolean printAllRecords)
			throws InterruptedException, UnknownHostException, IOException {
		int total = 0;
		for (Integer accountId : accountWithBalance.keySet()) {
			BalanceResponse response = (BalanceResponse) client.checkBalance(accountId);
			if (printAllRecords)
				System.out.println(response.getBalance());
			total += response.getBalance();
		}
		System.out.println("Total Amount deposited in all account:" + total);
		return total;
	}

	private void createAcoountIdList() {
		for (Integer accountId : accountWithBalance.keySet()) {
			accountIds.add(accountId);
		}
	}

	public void run() {
		for (int i = 0; i < iterationCount; i++) {
			final Socket socket;
			Random random = new Random();
			int sourceAccount = random.nextInt(accountIds.size());
			int destinationAccount;
			while ((destinationAccount = random.nextInt(accountIds.size())) == sourceAccount) {
			}
			try {
				TransferResponse response = (TransferResponse) transferMoney(accountIds.get(sourceAccount),
						accountIds.get(destinationAccount), 10);
				if (response.status.equals("FAILED"))
					log("Response:{" + response.status + "}" + "  {Source Account:" + accountIds.get(sourceAccount)
							+ " Destination Account:" + accountIds.get(destinationAccount) + "}");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void runMultipleClients() throws InterruptedException {
		for (int i = 0; i < threadCount; i++) {
			BankClient client = new BankClient();
			client.start();
			client.join();
		}
	}

	public Response transferMoney(int sourceAccount, int destinationAccount, int amount) throws IOException {
		Socket socket = new Socket(host, port);
		TransferResponse response = null;
		OutputStream rawOut = socket.getOutputStream();
		ObjectOutputStream sender = new ObjectOutputStream(rawOut);
		TransferRequest request = new TransferRequest("Transfer", sourceAccount, destinationAccount, amount, "Client");
		sender.writeObject(request);
		sender.flush();
		socket.shutdownOutput();
		try {
			ObjectInputStream rawIn = new ObjectInputStream(socket.getInputStream());
			response = (TransferResponse) rawIn.readObject();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		socket.close();
		return response;
	}

	public Response createAccount() throws IOException {
		Socket socket = new Socket(host, port);
		CreateAccountResponse response = null;
		OutputStream rawOut = socket.getOutputStream();
		ObjectOutputStream sender = new ObjectOutputStream(rawOut);
		CreateAccountRequest request = new CreateAccountRequest("CreateAcccount", "Client");
		sender.writeObject(request);
		sender.flush();
		socket.shutdownOutput();
		try {
			ObjectInputStream rawIn = new ObjectInputStream(socket.getInputStream());
			response = (CreateAccountResponse) rawIn.readObject();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		socket.close();
		return response;
	}

	private Response depositAmount(Integer accountId, int amount) throws IOException {
		Socket socket = new Socket(host, port);
		DepositResponse response = null;
		OutputStream rawOut = socket.getOutputStream();
		ObjectOutputStream sender = new ObjectOutputStream(rawOut);
		DepositRequest request = new DepositRequest("Deposit", accountId, amount, "Client");
		sender.writeObject(request);
		sender.flush();
		socket.shutdownOutput();
		try {
			ObjectInputStream rawIn = new ObjectInputStream(socket.getInputStream());
			response = (DepositResponse) rawIn.readObject();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		socket.close();
		return response;
	}

	private Response checkBalance(Integer accountId) throws IOException {
		Socket socket = new Socket(host, port);
		BalanceResponse response = null;
		OutputStream rawOut = socket.getOutputStream();
		ObjectOutputStream sender = new ObjectOutputStream(rawOut);
		GetBalanceRequest request = new GetBalanceRequest("Balance", accountId, "Client");
		sender.writeObject(request);
		sender.flush();
		socket.shutdownOutput();
		try {
			ObjectInputStream rawIn = new ObjectInputStream(socket.getInputStream());
			response = (BalanceResponse) rawIn.readObject();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		socket.close();
		return response;
	}

	private void startServer() {
		new Thread() {
			public void run() {
				try {
					server = new BankServer(host, String.valueOf(port));
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}
				try {
					server.startServer();
				} catch (NumberFormatException | IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	private void sendCloseRequest() throws UnknownHostException, IOException {

		Socket socket = new Socket(host, port);
		BalanceResponse response = null;
		OutputStream rawOut = socket.getOutputStream();
		ObjectOutputStream sender = new ObjectOutputStream(rawOut);
		sender.writeObject(null);
		sender.flush();
		socket.shutdownOutput();
		socket.close();

	}

	private synchronized void log(String message) {
		writer.println(message);
	}
}
