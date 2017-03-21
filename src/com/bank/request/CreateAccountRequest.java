package com.bank.request;

import java.io.Serializable;

public class CreateAccountRequest extends Request implements Serializable{
	@Override
	protected void setRequestName(String requestName) {
		this.requestName = requestName;
	}
	public CreateAccountRequest(String requestName)
	{
		this.setRequestName(requestName);
	}
	@Override
	public String toString() {
		return this.requestName;

	}
}
