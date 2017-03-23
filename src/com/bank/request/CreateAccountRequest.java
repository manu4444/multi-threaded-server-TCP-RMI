package com.bank.request;

import java.io.Serializable;

public class CreateAccountRequest extends Request implements Serializable{
	@Override
	protected void setRequestName(String requestName) {
		this.requestName = requestName;
	}

	@Override
	protected void setRequestOrigin(String requestOrigin) {
		this.requestOrigin = requestOrigin;
	}

	public CreateAccountRequest(String requestName, String requestOrigin)
	{
		this.setRequestName(requestName);
		this.setRequestOrigin(requestOrigin);
	}
	@Override
	public String toString() {
		return this.requestName;

	}
}
