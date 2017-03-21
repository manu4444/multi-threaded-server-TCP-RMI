package com.bank.response;

import java.io.Serializable;

public class BalanceResponse extends Response implements Serializable {
	int balance;

	public BalanceResponse(int balance) {
		this.balance = balance;
	}

	public int getBalance() {
		return balance;
	}

	@Override
	public String toString() {
		return "Balance:{" + this.getBalance() + "}";
	}
}
