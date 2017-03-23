package com.bank.rmi;

import com.bank.request.*;
import com.bank.response.*;
import com.bank.socket.*;

import java.io.*;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;

import com.bank.*;
import com.bank.*;
import jdk.management.resource.ResourceRequest;

public class RmiBankServerImpl extends UnicastRemoteObject implements RmiBankServer, Runnable {


	//Newly Added variables
	public static Integer timestamp = 0;
	public static int accountId = 1;
	public static List<ServerDetail> serverDetails;
	public static Map<Integer, ServerDetail> peerList;
	//Map<Integer, ServerDetail> idToServer;
	public static ServerDetail myDetail;
	public static Map<Integer, RmiBankServer> peerHandles;

	public static Map<Integer, PriorityBlockingQueue<Request>> pendingRequestQueue = new HashMap<Integer, PriorityBlockingQueue<Request>>();
	private static Hashtable<Integer, Account> accounts = new Hashtable<Integer, Account>();
	private Request clientRequest;
	private static PrintWriter writer,writerForAllReq;
	private List<Response> responseList = null;;

	public synchronized void  addToExecutionQueue(Request req){
		PriorityBlockingQueue<Request> reqQueue = pendingRequestQueue.get(req.getLamportClock().serverId);

		try {
			reqQueue.add(req);
			writerForAllReq.println(req.toString());
		}catch (Exception e){
			e.printStackTrace();
		}

	}

	class RequestExecutor implements Runnable {

		@Override
		public void run() {

			PriorityBlockingQueue<Request> tempReq = new PriorityBlockingQueue<Request>();
			while( true ){
				try {
					Thread.sleep(10);
					boolean sizeFull = true;
					tempReq.clear();
					//checking size of each
					for( int serverId : pendingRequestQueue.keySet()){
						if( pendingRequestQueue.get(serverId).size() <= 0){
							sizeFull = false;
							break;
						} else {
							tempReq.add(pendingRequestQueue.get(serverId).peek());
						}
					}

					if(sizeFull){
						//PriorityQueue<Request> tempReq = new PriorityQueue<Request>();
						Request req = tempReq.peek();
						switch( req.getRequestName()){
							case "Transfer": transferAmount((TransferRequest) req );
											break;
							case "Halt":
								System.out.println("Server received halt message and would stop");
								break;
						}

						//remove that request from the queue
						pendingRequestQueue.get(req.getLamportClock().serverId).remove();
					}


				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}


		}
	}


	public RmiBankServerImpl(Request request, List<Response> responseList) throws RemoteException {
		this.clientRequest = request;
		this.responseList = responseList;
	}

	public RmiBankServerImpl(String serverId) throws IOException {
		writer = new PrintWriter(new FileWriter("severLogfile"+Integer.parseInt(serverId)),true);
		writerForAllReq = new PrintWriter(new FileWriter("severLogfile"+Integer.parseInt(serverId)+"AllReq"),true);
	}

	public Response sendRequest(Request request) throws RemoteException, InterruptedException {
		RmiBankServerImpl server = new RmiBankServerImpl(request, new ArrayList<Response>());
		Thread thread = new Thread(server);
		thread.start();
		thread.join();
		return new TransferResponse("OK");//TO-DO
	}

	public void lookupPeer() {
		for( Integer hostId : peerList.keySet()){

			ServerDetail remoteHost = peerList.get(hostId);
			String location = "//" + remoteHost.hostname + ":" + Registry.REGISTRY_PORT + "/RmiBankServer" + remoteHost.id;
			System.out.println("Looking for peer:" + location);
			while(true) {
				try {
					Thread.sleep(50);
					RmiBankServer peerHandle = (RmiBankServer)Naming.lookup(location);
					peerHandles.put(hostId, peerHandle);
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
	public void init()
			throws IOException, AlreadyBoundException {

		//String location = "//" + myDetail.hostname + ":" + myDetail.port + "/RmiBankServer";
		System.setSecurityManager(new RMISecurityManager());
		RmiBankServerImpl bankServer = new RmiBankServerImpl(Integer.toString(myDetail.id));
		Registry localRegistry;
		try {
			LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
		} catch (ExportException e){
			//registry already running and do nothing
		}
		localRegistry = LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
		localRegistry.rebind("RmiBankServer" + myDetail.id, bankServer);

		for( ServerDetail sd : serverDetails){
			pendingRequestQueue.put(sd.id, new PriorityBlockingQueue<Request>());
		}


		//Create 10 accounts
		for( int i=0; i<10; i++ ){
			CreateAccountResponse res = (CreateAccountResponse)createAccount ( new CreateAccountRequest("CreateAcccount", "Server") );
			depositAmount( new DepositRequest("Deposit", res.getUid(), 1000, "Server"));
		}

		System.out.println("Successfully deposited 1000 to 10 account");
		lookupPeer();

		Thread req = new Thread(new RequestExecutor());
		req.start();
	}

	public void processTransferRequest(TransferRequest transferRequest) throws RemoteException, InterruptedException {


		if( transferRequest.getRequestOrigin().equals("Client")) { //Send Request to all the server

			synchronized (timestamp) { //Increment timestamp)
				timestamp++;
			}
			transferRequest.setRequestOrigin("Server");
			transferRequest.setLamportClock(new LamportClock(timestamp, myDetail.id));
				for (int serverId : peerHandles.keySet()) {
					peerHandles.get(serverId).sendRequest(transferRequest);
			}
		} else if( transferRequest.getRequestOrigin().equals("Server")) { //Send Ack to all the server

			AckRequest ackReq = new AckRequest("Ack", "Server");
			synchronized (timestamp){
				timestamp = Math.max(timestamp, transferRequest.getLamportClock().timestamp);
				timestamp++;
			}
			ackReq.setLamportClock(new LamportClock(timestamp, myDetail.id));
			addToExecutionQueue(ackReq); // Also add acknowledgement to your queue -- very important
				for (int serverId : peerHandles.keySet()) {
					peerHandles.get(serverId).sendRequest(ackReq);
				}
		}
		addToExecutionQueue(transferRequest);
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
			try {
				processTransferRequest((TransferRequest)clientRequest);
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}


			//Response response = transferAmount((TransferRequest) clientRequest);
			//responseList.add(response);
		}
		if (clientRequest instanceof AckRequest) {

			addToExecutionQueue(clientRequest);
			//Response response = transferAmount((TransferRequest) clientRequest);
			//responseList.add(response);
		}
		if (clientRequest instanceof HaltRequest) {

			addToExecutionQueue(clientRequest);
			//Response response = transferAmount((TransferRequest) clientRequest);
			//responseList.add(response);
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
		//log(clientRequest, response);
		return response;
	}

	private synchronized void log(Request clientRequest, Response serverResponse) {
		writer.println("Request:{" + clientRequest + "}" + "  Response:{" + serverResponse + "}");
		//writer.flush();
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
		//log(clientRequest, response);
		return response;
	}

	private synchronized Response createAccount(CreateAccountRequest clientRequest) {
		int accountNumber = generateAccountId();
		accounts.put(accountNumber, new Account(accountNumber));
		CreateAccountResponse response = new CreateAccountResponse(accountNumber);
		//log(clientRequest, response);
		return response;
	}

	public int generateAccountId() {
		return accountId++;
//		while (accounts.containsKey((int) System.currentTimeMillis())) {
//		}
//		return (int) System.currentTimeMillis();
	}






	public static void main(String args[])
			throws IOException, AlreadyBoundException {





		//args[0] = "0";
		//args[1] = "configFile";

		RmiBankServerImpl server = new RmiBankServerImpl(args[0]);
		server.configInitialization(args[0], "configFile");
		server.init();
	}




	public void configInitialization(String serverId, String configFile){
		int myServerId = Integer.parseInt(serverId);

		serverDetails = new ArrayList<ServerDetail>();
		peerList = new HashMap<Integer, ServerDetail>();
		peerHandles = new HashMap<Integer, RmiBankServer>();

		serverDetails.add(new ServerDetail(1,"localhost",4000));
		serverDetails.add(new ServerDetail(2,"localhost",4001));
		//serverDetails.add(new ServerDetail(3,"localhost",4002));

		for( ServerDetail sd : serverDetails){
			if( sd.id == myServerId ){
				myDetail = sd;
			} else {
				peerList.put(sd.id, sd);
			}
			//idToServer.put(sd.id, sd);
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
}
