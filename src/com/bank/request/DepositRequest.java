package com.bank.request;

import java.io.Serializable;

public class DepositRequest extends Request implements Serializable {
	protected int uid, amount;

	@Override
	protected void setRequestName(String requestName) {
		this.requestName = requestName;
	}

	@Override
	protected void setRequestOrigin(String requestOrigin) {
		this.requestOrigin = requestOrigin;
	}


	public int getUid() {
		return uid;
	}

	public int getAmount() {
		return amount;
	}

	public DepositRequest(String requestName, int uid, int amount, String requestOrigin) {
		this.setRequestName(requestName);
		this.amount = amount;
		this.uid = uid;
		this.requestOrigin = requestOrigin;
	}

	@Override
	public String toString() {
		return this.requestName + ":{" + this.uid + "," + this.amount + "}";
	}
}
