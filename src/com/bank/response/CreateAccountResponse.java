package com.bank.response;

import java.io.Serializable;

public class CreateAccountResponse extends Response implements Serializable {
	private int uid;

	public int getUid() {
		return uid;
	}

	public CreateAccountResponse(int uid) {
		this.uid = uid;
	}

	@Override
	public String toString() {
		return "CreateAccount:{" + this.uid + "}";
	}

}
