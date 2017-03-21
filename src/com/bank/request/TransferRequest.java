package com.bank.request;

import java.io.Serializable;

public class TransferRequest extends Request implements Serializable {
	int sourceUid, destinationUid, amount;

	@Override
	protected void setRequestName(String requestName) {
		this.requestName = requestName;
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

	public TransferRequest(String requestName, int sourceUid, int destinationUid, int amount) {
		this.setRequestName(requestName);
		this.sourceUid = sourceUid;
		this.destinationUid = destinationUid;
		this.amount = amount;
	}

	@Override
	public String toString() {
		return this.requestName + ":{" + this.sourceUid + "," + this.destinationUid + "," + this.amount + "}";

	}
}
