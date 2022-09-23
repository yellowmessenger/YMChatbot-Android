package com.yellowmessenger.ymchat.models;

public class YellowMessagesItem {
	private String messageType;
	private String created;
	private YellowMessage yellowMessage;

	public String getMessageType(){
		return messageType;
	}

	public String getCreated(){
		return created;
	}

	public YellowMessage getMessage(){
		return yellowMessage;
	}
}
