package com.bank.request;

import java.io.Serializable;

public class DepositRequest extends Request implements Serializable {
	protected int uid, amount;

	@Override
	protected void setRequestName(String requestName) {
		this.requestName = requestName;
	}

	public int getUid() {
		return uid;
	}

	public int getAmount() {
		return amount;
	}

	public DepositRequest(String requestName, int uid, int amount) {
		this.setRequestName(requestName);
		this.amount = amount;
		this.uid = uid;
	}

	@Override
	public String toString() {
		return this.requestName + ":{" + this.uid + "," + this.amount + "}";
	}
}
