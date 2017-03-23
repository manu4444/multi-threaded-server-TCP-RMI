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
import java.rmi.registry.Registry;
import java.util.*;

import com.bank.*;

public class RmiBankClient extends Thread {
	private static String host;
	private static int portNumber;
	private static Hashtable<Integer, Integer> accountWithBalance = new Hashtable<Integer, Integer>();
	private static List<Integer> accountIds = new ArrayList<Integer>(Arrays.asList(1,2,3,4,5,6,7,8,9,10));
	private static int threadCount=3;
	private static int  iterationCount = 20;
	private static RmiBankServer dateServer;
	private static PrintWriter writer;

	public static void setThreadCount(int threadCount) {
		RmiBankClient.threadCount = threadCount;
	}

	public static void setIterationCount(int iterationCount) {
		RmiBankClient.iterationCount = iterationCount;
	}

	//Manu

	public static Map<Integer, ServerDetail> serverDetails;
	//Map<Integer, ServerDetail> idToServer;
	public static Map<Integer, RmiBankServer> serverHandles;


	public void configInitialization(String configFile) throws InterruptedException {
		//int myServerId = Integer.parseInt(serverId);

		serverDetails = new HashMap<Integer, ServerDetail>();
		serverHandles = new HashMap<Integer, RmiBankServer>();

		serverDetails.put(1, new ServerDetail(1,"localhost",4000));
		serverDetails.put(2, new ServerDetail(2,"localhost",4001));
		//serverDetails.put(3, new ServerDetail(3,"localhost",4002));

		lookupServer();

		//int threadCount = 2;
		//Start 24 thread processes

		for (int i = 0; i < threadCount; i++) {
			RmiBankClient client = new RmiBankClient();
			client.start();
			client.join();
		}


		//myDetail = idToServer.get();
	}

	class ServerDetail{
		String hostname;
		int port;
		int id;

		ServerDetail(int id, String hostname, int port){
			this.hostname = hostname;
			this.port = port;
			this.id = id;
		}
	}


	public void lookupServer() {
		for( Integer hostId : serverDetails.keySet()){

			ServerDetail remoteHost = serverDetails.get(hostId);
			String location = "//" + remoteHost.hostname + ":" + Registry.REGISTRY_PORT + "/RmiBankServer" + remoteHost.id;
			System.out.println("Looking for peer:" + location);
			while(true) {
				try {
					Thread.sleep(50);
					RmiBankServer peerHandle = (RmiBankServer)Naming.lookup(location);
					serverHandles.put(hostId, peerHandle);
					System.out.println("Found peer " + location);
					//wpoolHandles.put(host, peerHandle);
					//L..oadInfo peerLoadInfo = new LoadInfo(0,host);
					//peerLoadInfo.load = 0;
					//loadInfoTable.put(host, peerLoadInfo);
					break;
				} catch(Exception e) {
					System.out.println("Error locating peer " + location);
				}
			}
		}
	}




	public static void main(String args[]) throws Exception {
		//host = args[0];
		writer = new PrintWriter("clientLogfile");
		RmiBankClient client = new RmiBankClient();
		//portNumber = Integer.parseInt(args[1]);
		//RmiBankClient.setThreadCount(Integer.parseInt(args[2]));
		//RmiBankClient.setIterationCount(Integer.parseInt(args[3]));
//		System.setSecurityManager(new RMISecurityManager());
//		//dateServer = new RmiBankServerImpl();
//		//dateServer = (RmiBankServer) Naming.lookup("//" + host + ":" + portNumber + "/RmiBankServer");
//		RmiBankClient client = new RmiBankClient();
//		for (int i = 0; i < 100; i++) {
//			CreateAccountResponse response = (CreateAccountResponse) client.createAccount();
//			accountWithBalance.put(response.getUid(), 0);
//		}
//		for (Integer accountId : accountWithBalance.keySet()) {
//			DepositResponse response = (DepositResponse) client.depositAmount(accountId, 100);
//		}
//		client.checkTotalBalanceInAllAccounts(client, false);
//		client.createAcoountIdList();
//		client.runMultipleClients();
//		System.out.println("After multiple transfer of $10");
//		client.checkTotalBalanceInAllAccounts(client, false);
		client.configInitialization("ConfigFile");
		writer.close();
		System.exit(0);
	}

	public void run() {

		//int iterationCount = 5;
		for (int i = 0; i < iterationCount; i++) {

			//Selecting random server
			List<Integer> keysAsArray = new ArrayList<Integer>(serverDetails.keySet());
			Random r = new Random();
			int serverId = keysAsArray.get(r.nextInt(keysAsArray.size()));



			Random random = new Random();
			int sourceAccount = random.nextInt(10) + 1;
			int destinationAccount;
			while ((destinationAccount = (random.nextInt(10) + 1)) == sourceAccount) {
			}
			TransferResponse response = null;
			try {
				response = (TransferResponse) transferMoney(sourceAccount,
						destinationAccount, 600, serverId);
				if (response.status.equals("FAILED"))
					log("Response:{" + response.status + "}" + "  {Source Account:" + sourceAccount
							+ " Destination Account:" + destinationAccount + "}");
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}

	private TransferResponse transferMoney(int sourceAccount, int destinationAccount, int amount, int serverId)
			throws RemoteException, InterruptedException {
		TransferRequest request = new TransferRequest("Transfer", sourceAccount, destinationAccount, amount, "Client");
		TransferResponse response = (TransferResponse) serverHandles.get(serverId).sendRequest(request);
		return response;
	}

	private DepositResponse depositAmount(Integer accountId, int amount) throws RemoteException, InterruptedException {
		DepositRequest request = new DepositRequest("Deposit", accountId, amount, "Client");
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
		GetBalanceRequest request = new GetBalanceRequest("Balance", accountId, "Client");
		BalanceResponse response = (BalanceResponse) dateServer.sendRequest(request);
		return response;
	}

	private CreateAccountResponse createAccount() throws RemoteException, InterruptedException {
		CreateAccountRequest request = new CreateAccountRequest("CreateAcccount", "Client");
		CreateAccountResponse response = (CreateAccountResponse) dateServer.sendRequest(request);
		return response;
	}

	private synchronized void log(String message) {
		writer.println(message);
	}
}
