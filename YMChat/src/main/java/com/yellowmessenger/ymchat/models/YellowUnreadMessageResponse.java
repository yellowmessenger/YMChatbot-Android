package com.yellowmessenger.ymchat.models;

import java.util.List;

public class YellowUnreadMessageResponse{
	private int unreadCount;
	private List<YellowMessagesItem> messages;

	public int getUnreadCount(){
		return unreadCount;
	}

	public List<YellowMessagesItem> getMessages(){
		return messages;
	}
}
