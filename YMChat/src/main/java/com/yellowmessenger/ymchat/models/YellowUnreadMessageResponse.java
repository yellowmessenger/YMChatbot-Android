package com.yellowmessenger.ymchat.models;

import java.util.List;

public class YellowUnreadMessageResponse{
	private String unreadCount;
	private List<YellowMessagesItem> messages;

	public String getUnreadCount(){
		return unreadCount;
	}

	public List<YellowMessagesItem> getMessages(){
		return messages;
	}
}
