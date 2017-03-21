package com.bank.response;

import java.io.Serializable;

public class DepositResponse extends Response implements Serializable {
	String status;

	public String getStatus() {
		return status;
	}

	public DepositResponse(String status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "Deposit:{" + this.status + "}";
	}
}
