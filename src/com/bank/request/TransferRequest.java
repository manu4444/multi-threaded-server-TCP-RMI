package com.bank.request;

import com.bank.response.Response;
import com.bank.response.TransferResponse;

import java.io.Serializable;
import java.util.concurrent.locks.Lock;

public class TransferRequest extends Request implements Serializable {
	int sourceUid, destinationUid, amount;
	public int clientId;



	@Override
	protected void setRequestName(String requestName) {
		this.requestName = requestName;
	}

	@Override
	public void setRequestOrigin(String requestOrigin) {
		this.requestOrigin = requestOrigin;
	}


	public int getSourceUid() {
		return sourceUid;
	}

	public int getDestinationUid() {
		return destinationUid;
	}

	public int getAmount() {
		return amount;
	}

	public TransferRequest(String requestName, int sourceUid, int destinationUid, int amount, String requestOrigin, int clientId) {
		this.setRequestName(requestName);
		this.sourceUid = sourceUid;
		this.destinationUid = destinationUid;
		this.amount = amount;
		this.requestOrigin = requestOrigin;
		this.clientId = clientId;
	}

	@Override
	public String toString() {
		return this.requestName + ":{" + this.sourceUid + "," + this.destinationUid + "," + this.amount + "}" + " Lamport Clock :" + this.lamportClock.toString();

	}
}
