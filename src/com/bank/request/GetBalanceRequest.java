package com.bank.request;

import java.io.Serializable;

public class GetBalanceRequest extends Request implements Serializable {
	int uid;

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

	public GetBalanceRequest(String requestName, int uid, String requestOrigin) {
		this.setRequestName(requestName);
		this.uid = uid;
		this.requestOrigin = requestOrigin;
	}

	@Override
	public String toString() {
		return this.requestName + ":{" + this.uid + "}";

	}
}
