package com.bank.request;

import java.io.Serializable;
import java.util.Comparator;

public abstract class Request implements Serializable, Comparable<Request>{
	protected String requestName;
	protected String requestOrigin;
	protected LamportClock lamportClock = new LamportClock(-1,-1);

	protected abstract void setRequestName(String requestName);
	protected abstract void setRequestOrigin(String requestOrigin);

	public void setLamportClock(LamportClock lamportClock){
		this.lamportClock = lamportClock;
	}

	@Override
	public int compareTo(Request two) {
		if( this.lamportClock.timestamp > two.lamportClock.timestamp){
			return 1;
		} else if( this.lamportClock.timestamp < two.lamportClock.timestamp ){
			return -1;
		} else {
			if( this.lamportClock.serverId > two.lamportClock.serverId ){
				return 1;
			} else if (this.lamportClock.serverId < two.lamportClock.serverId) {
				return -1;
			} else {
				return 0;
			}
		}
	}

	public String getRequestName() {
		return requestName;
	}
	public String getRequestOrigin() {
		return requestOrigin;
	}

	public LamportClock getLamportClock(){
		return lamportClock;
	}

	public abstract String toString();
}
