package com.bank.rmi;

import com.bank.request.*;
import com.bank.response.*;
import com.bank.socket.*;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

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
			throws RemoteException, AlreadyBoundException, FileNotFoundException, UnsupportedEncodingException {

		//String location = "//" + myDetail.hostname + ":" + myDetail.port + "/RmiBankServer";
		System.setSecurityManager(new RMISecurityManager());
		RmiBankServerImpl bankServer = new RmiBankServerImpl();
		Registry localRegistry;
		try {
			LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
		} catch (ExportException e){
			//registry already running and do nothing
		}
		localRegistry = LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
		localRegistry.rebind("RmiBankServer" + myDetail.id, bankServer);

		//Create 10 accounts

		lookupPeer();
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


	List<ServerDetail> serverDetails;
	Map<Integer, ServerDetail> peerList;
	//Map<Integer, ServerDetail> idToServer;
	ServerDetail myDetail;
	Map<Integer, RmiBankServer> peerHandles;



	public static void main(String args[])
			throws RemoteException, AlreadyBoundException, FileNotFoundException, UnsupportedEncodingException {





		//args[0] = "0";
		//args[1] = "configFile";

		RmiBankServerImpl server = new RmiBankServerImpl();
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
		serverDetails.add(new ServerDetail(3,"localhost",4002));

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
