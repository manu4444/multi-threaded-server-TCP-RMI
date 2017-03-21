package com.bank.response;

import java.io.Serializable;

public class TransferResponse extends Response implements Serializable {
	public String status;

	public String getStatus() {
		return status;
	}

	public TransferResponse(String status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "Transfer:{" + this.status + "}";
	}
}
