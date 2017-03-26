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

import static java.lang.System.exit;

public class RmiBankServerImpl extends UnicastRemoteObject implements RmiBankServer, Runnable {

	//Newly Added variables
	public static Map<String, Long> requestProcessingTime = new HashMap<String, Long>();
	public static Integer timestamp = 0;
	public static int accountId = 1;
	public static List<ServerDetail> serverDetails;
	public static Map<Integer, ServerDetail> peerList;
	public static ServerDetail myDetail;
	public static Map<Integer, RmiBankServer> peerHandles;

	public static Map<Integer, PriorityBlockingQueue<Request>> pendingRequestQueue = new HashMap<Integer, PriorityBlockingQueue<Request>>();
	private static Hashtable<Integer, Account> accounts = new Hashtable<Integer, Account>();
	private Request clientRequest;
	private static PrintWriter writer;
	private List<Response> responseList = null;;

	public synchronized void  addToExecutionQueue(Request req){

		if( req.getRequestName().equals("Transfer") ) {
			requestProcessingTime.put(req.getLamportClock().toString(), System.currentTimeMillis());
		}
		PriorityBlockingQueue<Request> reqQueue = pendingRequestQueue.get(req.getLamportClock().serverId);

		try {
			reqQueue.add(req);
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
						Request req = tempReq.peek();
						switch( req.getRequestName()){
							case "Transfer":
								TransferRequest treq = (TransferRequest)req;
								Response rs;
								log(treq.clientId + "\t" +"PROCESS" + "\t" + System.currentTimeMillis() + "\t"
										+ treq.getLamportClock().toString());
								rs = (TransferResponse)transferAmount(treq);
								long startTime = requestProcessingTime.get(treq.getLamportClock().toString());
								requestProcessingTime.put(treq.getLamportClock().toString(), System.currentTimeMillis() - startTime);
								treq.response = rs;
								break;
							case "Halt":
								HaltRequest hreq = (HaltRequest)req;
								System.out.println("Server received halt message and would stop");
								hreq.response = new TransferResponse("Halt received");

								log("-------------------------------------------------------------------");
								log("Balance in each account");
								log("-------------------------------------------------------------------");
								for(int sid : accounts.keySet()){
									log("account id:" + sid + "\tAmount :" +accounts.get(sid).balance);
									System.out.println("account id:" + sid + "\tAmount :" +accounts.get(sid).balance);
								}

								log("-------------------------------------------------------------------");
								log("Pending Requests");
								log("-------------------------------------------------------------------");
								pendingRequestQueue.get(req.getLamportClock().serverId).remove();
								for( int server: pendingRequestQueue.keySet()){
									log("Server : "+server+pendingRequestQueue.get(server).toString());
								}

								log("-------------------------------------------------------------------");
								long t = 0;
								for( String lamportClock : requestProcessingTime.keySet() ){
									t += requestProcessingTime.get(lamportClock);
								}
								log("Average Request Processing Time (milliseconds):"+(float)t/requestProcessingTime.size());
								System.out.println("Average Request Processing Time (milliseconds):"+(float)t/requestProcessingTime.size() );

								exit(0);
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
	}

	public Response sendRequest(Request request) throws RemoteException, InterruptedException {

		RmiBankServerImpl server = new RmiBankServerImpl(request, new ArrayList<Response>());
		Thread thread = new Thread(server);
		thread.start();
		thread.join();
		if( this.myDetail.id == request.serverId &&
				(request.getRequestName().equals("Transfer") || request.getRequestName().equals("Halt")) ){
			while( request.response == null){
				Thread.sleep(1);
			}
			return request.response;
		}
		return new TransferResponse("OK");

	}

	public void lookupPeer() {
		for( Integer hostId : peerList.keySet()){

			ServerDetail remoteHost = peerList.get(hostId);
			String location = "//" + remoteHost.hostname + ":" + remoteHost.rmiPort + "/RmiBankServer";
			System.out.println("Looking for peer:" + location);
			while(true) {
				try {
					Thread.sleep(200);
					RmiBankServer peerHandle = (RmiBankServer)Naming.lookup(location);
					peerHandles.put(hostId, peerHandle);
					System.out.println("Found peer " + location);
					break;
				} catch(Exception e) {
					System.out.print(".");
				}
			}
		}
		System.out.println("Successfully found all peer server. Waiting for client requests.");
	}
	public void init()
			throws IOException, AlreadyBoundException {

		System.setSecurityManager(new RMISecurityManager());
		RmiBankServerImpl bankServer = new RmiBankServerImpl(Integer.toString(myDetail.id));
		Registry localRegistry;
		try {
			LocateRegistry.createRegistry(myDetail.rmiPort);
		} catch (ExportException e){
			//registry already running and do nothing
		}
		localRegistry = LocateRegistry.getRegistry(myDetail.rmiPort);
		localRegistry.rebind("RmiBankServer", bankServer);

		for( ServerDetail sd : serverDetails){
			pendingRequestQueue.put(sd.id, new PriorityBlockingQueue<Request>());
		}

		//Create 10 accounts
		for( int i=0; i<10; i++ ){
			CreateAccountResponse res = (CreateAccountResponse)createAccount ( new CreateAccountRequest("CreateAcccount", "Server") );
			depositAmount( new DepositRequest("Deposit", res.getUid(), 1000, "Server"));
		}

		System.out.println("Successfully deposited 1000 to 10 account");

		Thread req = new Thread(new RequestExecutor());
		req.start();

		lookupPeer();
	}

	public void processTransferRequest(TransferRequest transferRequest) throws RemoteException, InterruptedException {

		if( transferRequest.getRequestOrigin().equals("Client")) { //Send Request to all the server
			synchronized (timestamp) { //Increment timestamp)
				timestamp++;
			}
			transferRequest.setRequestOrigin("Server");
			transferRequest.setLamportClock(new LamportClock(timestamp, myDetail.id));
			transferRequest.serverId = myDetail.id;
			log(transferRequest.clientId + "\t" +"CLIENT-REQ" + "\t" + System.currentTimeMillis() + "\t"
					+ transferRequest.getLamportClock().toString() + "\t" + transferRequest.getRequestName()
					+ "\t" + "{" + transferRequest.getSourceUid() + "," + transferRequest.getDestinationUid()+ "," + transferRequest.getAmount() +"}");

			for (int serverId : peerHandles.keySet()) {
					peerHandles.get(serverId).sendRequest(transferRequest);
			}
		} else if( transferRequest.getRequestOrigin().equals("Server")) { //Send Ack to all the server

			log(transferRequest.clientId + "\t" +"SER-REQ" + "\t" + transferRequest.serverId + "\t" + System.currentTimeMillis() + "\t"
					+ transferRequest.getLamportClock().toString() + "\t" + transferRequest.getRequestName()
					+ "\t" + "{" + transferRequest.getSourceUid() + "," + transferRequest.getDestinationUid()+ "," + transferRequest.getAmount() +"}");
			;

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

	public void processHaltRequest(HaltRequest haltRequest) throws RemoteException, InterruptedException {


		if( haltRequest.getRequestOrigin().equals("Client")) { //Send Request to all the server

			synchronized (timestamp) { //Increment timestamp)
				timestamp++;
			}
			haltRequest.setRequestOrigin("Server");
			haltRequest.setLamportClock(new LamportClock(timestamp, myDetail.id));
			haltRequest.serverId = myDetail.id;
			log(haltRequest.clientId + "\t" +"CLIENT-REQ" + "\t" + System.currentTimeMillis() + "\t"
					+ haltRequest.getLamportClock().toString() + "\t" + haltRequest.getRequestName());

			for (int serverId : peerHandles.keySet()) {
				peerHandles.get(serverId).sendRequest(haltRequest);
			}
		} else if( haltRequest.getRequestOrigin().equals("Server")) { //Send Ack to all the server

			log(haltRequest.clientId + "\t" +"SER-REQ" + "\t" + haltRequest.serverId + "\t" + System.currentTimeMillis() + "\t"
					+ haltRequest.getLamportClock().toString() + "\t" + haltRequest.getRequestName());
			;

			AckRequest ackReq = new AckRequest("Ack", "Server");
			synchronized (timestamp){
				timestamp = Math.max(timestamp, haltRequest.getLamportClock().timestamp);
				timestamp++;
			}
			ackReq.setLamportClock(new LamportClock(timestamp, myDetail.id));
			addToExecutionQueue(ackReq); // Also add acknowledgement to your queue -- very important
			for (int serverId : peerHandles.keySet()) {
				peerHandles.get(serverId).sendRequest(ackReq);
			}
		}
		addToExecutionQueue(haltRequest);
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
		}
		if (clientRequest instanceof AckRequest) {

			addToExecutionQueue(clientRequest);
		}
		if (clientRequest instanceof HaltRequest) {

			try {
				processHaltRequest((HaltRequest) clientRequest);
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
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
		return response;
	}

	private synchronized void log(String msg) {
		writer.println(msg);
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
		return response;
	}

	private synchronized Response depositAmount(DepositRequest clientRequest) {
		DepositResponse response;
		if (accounts.containsKey(clientRequest.getUid())) {
			accounts.get(clientRequest.getUid()).balance += clientRequest.getAmount();
			response = new DepositResponse("OK");
		} else
			response = new DepositResponse("FAILED");
		return response;
	}

	private synchronized Response createAccount(CreateAccountRequest clientRequest) {
		int accountNumber = generateAccountId();
		accounts.put(accountNumber, new Account(accountNumber));
		CreateAccountResponse response = new CreateAccountResponse(accountNumber);
		return response;
	}

	public int generateAccountId() {
		return accountId++;
	}


	public static void main(String args[])
			throws IOException, AlreadyBoundException {

		RmiBankServerImpl server = new RmiBankServerImpl(args[0]);
		server.configInitialization(args[0], args[1]);
		server.init();
	}

	public void configInitialization(String serverId, String configFile) throws IOException {


		int myServerId = Integer.parseInt(serverId);

		serverDetails = new ArrayList<ServerDetail>();
		peerList = new HashMap<Integer, ServerDetail>();
		peerHandles = new HashMap<Integer, RmiBankServer>();

		FileInputStream fis = new FileInputStream(configFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] splited = line.split("\\s+");
			if( splited[0].charAt(0) == '#' ) continue;
			serverDetails.add(new ServerDetail(Integer.parseInt(splited[1]),splited[0],Integer.parseInt(splited[2])));
		}
		br.close();

		for( ServerDetail sd : serverDetails){
			if( sd.id == myServerId ){
				myDetail = sd;
			} else {
				peerList.put(sd.id, sd);
			}
		}
	}
}
