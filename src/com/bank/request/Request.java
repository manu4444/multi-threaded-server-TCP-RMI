package com.bank.request;

import java.io.Serializable;

public abstract class Request implements Serializable {
	protected String requestName;

	protected abstract void setRequestName(String requestName);

	public String getRequestName() {
		return requestName;
	}

	public abstract String toString();
}
