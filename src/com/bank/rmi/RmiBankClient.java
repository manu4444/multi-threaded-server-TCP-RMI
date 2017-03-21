package com.bank.rmi;

import com.bank.request.CreateAccountRequest;
import com.bank.request.DepositRequest;
import com.bank.request.GetBalanceRequest;
import com.bank.request.TransferRequest;
import com.bank.response.BalanceResponse;
import com.bank.response.CreateAccountResponse;
import com.bank.response.DepositResponse;
import com.bank.response.TransferResponse;

import java.io.PrintWriter;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import com.bank.*;

public class RmiBankClient extends Thread {
	private static String host;
	private static int portNumber;
	private static Hashtable<Integer, Integer> accountWithBalance = new Hashtable<Integer, Integer>();
	private static List<Integer> accountIds = new Vector<Integer>();
	private static int threadCount, iterationCount;
	private static RmiBankServer dateServer;
	private static PrintWriter writer;

	public static void setThreadCount(int threadCount) {
		RmiBankClient.threadCount = threadCount;
	}

	public static void setIterationCount(int iterationCount) {
		RmiBankClient.iterationCount = iterationCount;
	}

	public static void main(String args[]) throws Exception {
		host = args[0];
		writer = new PrintWriter("clientLogfile");
		portNumber = Integer.parseInt(args[1]);
		RmiBankClient.setThreadCount(Integer.parseInt(args[2]));
		RmiBankClient.setIterationCount(Integer.parseInt(args[3]));
		System.setSecurityManager(new RMISecurityManager());
		dateServer = new RmiBankServerImpl();
		dateServer = (RmiBankServer) Naming.lookup("//" + host + ":" + portNumber + "/RmiBankServer");
		RmiBankClient client = new RmiBankClient();
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

	public void run() {
		for (int i = 0; i < iterationCount; i++) {
			Random random = new Random();
			int sourceAccount = random.nextInt(accountIds.size());
			int destinationAccount;
			while ((destinationAccount = random.nextInt(accountIds.size())) == sourceAccount) {
			}
			TransferResponse response = null;
			try {
				response = (TransferResponse) transferMoney(accountIds.get(sourceAccount),
						accountIds.get(destinationAccount), 10);
				if (response.status.equals("FAILED"))
					log("Response:{" + response.status + "}" + "  {Source Account:" + accountIds.get(sourceAccount)
							+ " Destination Account:" + accountIds.get(destinationAccount) + "}");
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}

	private TransferResponse transferMoney(int sourceAccount, int destinationAccount, int amount)
			throws RemoteException, InterruptedException {
		TransferRequest request = new TransferRequest("Transfer", sourceAccount, destinationAccount, amount);
		TransferResponse response = (TransferResponse) dateServer.sendRequest(request);
		return response;
	}

	private DepositResponse depositAmount(Integer accountId, int amount) throws RemoteException, InterruptedException {
		DepositRequest request = new DepositRequest("Deposit", accountId, amount);
		DepositResponse response = (DepositResponse) dateServer.sendRequest(request);
		return response;
	}

	private void runMultipleClients() throws InterruptedException {
		for (int i = 0; i < threadCount; i++) {
			RmiBankClient client = new RmiBankClient();
			client.start();
			client.join();
		}

	}

	private void createAcoountIdList() {
		for (Integer accountId : accountWithBalance.keySet()) {
			accountIds.add(accountId);
		}
	}

	private int checkTotalBalanceInAllAccounts(RmiBankClient client, boolean printAllRecords)
			throws RemoteException, InterruptedException {
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

	private BalanceResponse checkBalance(Integer accountId) throws RemoteException, InterruptedException {
		GetBalanceRequest request = new GetBalanceRequest("Balance", accountId);
		BalanceResponse response = (BalanceResponse) dateServer.sendRequest(request);
		return response;
	}

	private CreateAccountResponse createAccount() throws RemoteException, InterruptedException {
		CreateAccountRequest request = new CreateAccountRequest("CreateAcccount");
		CreateAccountResponse response = (CreateAccountResponse) dateServer.sendRequest(request);
		return response;
	}

	private synchronized void log(String message) {
		writer.println(message);
	}
}
