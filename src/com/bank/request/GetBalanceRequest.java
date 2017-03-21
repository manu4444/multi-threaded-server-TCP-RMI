package com.bank.request;

import java.io.Serializable;

public class GetBalanceRequest extends Request implements Serializable {
	int uid;

	@Override
	protected void setRequestName(String requestName) {
		this.requestName = requestName;
	}

	public int getUid() {
		return uid;
	}

	public GetBalanceRequest(String requestName, int uid) {
		this.setRequestName(requestName);
		this.uid = uid;
	}

	@Override
	public String toString() {
		return this.requestName + ":{" + this.uid + "}";

	}
}
