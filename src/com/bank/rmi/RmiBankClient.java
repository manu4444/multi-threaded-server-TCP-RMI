package com.bank.rmi;

import com.bank.request.*;
import com.bank.response.TransferResponse;

import java.io.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.*;

public class RmiBankClient extends Thread {
	private static String host;
	private static int portNumber;
	private static Hashtable<Integer, Integer> accountWithBalance = new Hashtable<Integer, Integer>();
	private static List<Integer> accountIds = new ArrayList<Integer>(Arrays.asList(1,2,3,4,5,6,7,8,9,10));
	private static int threadCount=5;
	private static int  iterationCount = 100;
	private static RmiBankServer dateServer;
	private static PrintWriter writer;
	public int clientId;

	public RmiBankClient(int clientId){
		this.clientId = clientId;
	}
	public static void setThreadCount(int threadCount) {
		RmiBankClient.threadCount = threadCount;
	}

	public static void setIterationCount(int iterationCount) {
		RmiBankClient.iterationCount = iterationCount;
	}

	public static Map<Integer, ServerDetail> serverDetails;
	public static Map<Integer, RmiBankServer> serverHandles;


	public void configInitialization(String configFile) throws InterruptedException, IOException {

		serverDetails = new HashMap<Integer, ServerDetail>();
		serverHandles = new HashMap<Integer, RmiBankServer>();

		FileInputStream fis = new FileInputStream(configFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		String line = null;
		while ((line = br.readLine()) != null) {
			System.out.println(line);
			String[] splited = line.split("\\s+");
			if( splited[0].charAt(0) == '#' ) continue;
			serverDetails.put(Integer.parseInt(splited[1]),new ServerDetail(Integer.parseInt(splited[1]),splited[0],Integer.parseInt(splited[2])));
		}
		br.close();
		lookupServer();

		long startTime = System.currentTimeMillis();
		for (int i = 1; i <= threadCount; i++) {
			RmiBankClient client = new RmiBankClient(i);
			client.start();
			client.join();
		}
		TransferResponse response = (TransferResponse) serverHandles.get(0).sendRequest(new HaltRequest("Halt", "Client", 0));

		System.out.println("Everything done. Please check log file clientLogfile for more detail.");
		System.out.println("Everything done. Total execution time: " + (System.currentTimeMillis() - startTime)/1000 + "secs");
	}


	public void lookupServer() {
		for( Integer hostId : serverDetails.keySet()){

			ServerDetail remoteHost = serverDetails.get(hostId);
			String location = "//" + remoteHost.hostname + ":" + remoteHost.rmiPort + "/RmiBankServer";
			System.out.println("Looking for peer:" + location);
			while(true) {
				try {
					Thread.sleep(50);
					RmiBankServer peerHandle = (RmiBankServer)Naming.lookup(location);
					serverHandles.put(hostId, peerHandle);
					System.out.println("Found peer " + location);
					break;
				} catch(Exception e) {
					System.out.println("Error locating peer " + location);
				}
			}
		}
	}

	public static void main(String args[]) throws Exception {
		writer = new PrintWriter("clientLogfile");
		RmiBankClient client = new RmiBankClient(0);
		setThreadCount(Integer.parseInt(args[0]));
		client.configInitialization(args[1]);
		writer.close();
		System.exit(0);
	}

	public void run() {

		for (int i = 0; i < iterationCount; i++) {

			//Selecting random server
			List<Integer> keysAsArray = new ArrayList<Integer>(serverDetails.keySet());
			Random r = new Random();
			int serverId = keysAsArray.get(r.nextInt(keysAsArray.size()));

			Random random = new Random();
			int sourceAccount = random.nextInt(10) + 1;
			int destinationAccount;
			int transferAmount = 10;
			while ((destinationAccount = (random.nextInt(10) + 1)) == sourceAccount) {
			}
			TransferResponse response = null;
			try {
				System.out.println(" REQ Transfer"+"{"+sourceAccount+","+destinationAccount+","+transferAmount+"}");
				log(clientId+"\t"+serverId+"\t"+"REQ"+"\t"+System.currentTimeMillis()
						+"\t"+"Transfer"+"{"+sourceAccount+","+destinationAccount+","+transferAmount+"}");
				response = (TransferResponse) transferMoney(sourceAccount,
						destinationAccount, transferAmount, serverId);
				log(clientId+"\t"+serverId+"\t"+"REP"+"\t"+System.currentTimeMillis()+"\t"
						+response.status);

			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}

	private TransferResponse transferMoney(int sourceAccount, int destinationAccount, int amount, int serverId)
			throws RemoteException, InterruptedException {
		TransferRequest request = new TransferRequest("Transfer", sourceAccount, destinationAccount, amount, "Client", clientId);
		TransferResponse response = (TransferResponse) serverHandles.get(serverId).sendRequest(request);
		return response;
	}

	private void createAcoountIdList() {
		for (Integer accountId : accountWithBalance.keySet()) {
			accountIds.add(accountId);
		}
	}

	private synchronized void log(String message) {
		writer.println(message);
	}
}
