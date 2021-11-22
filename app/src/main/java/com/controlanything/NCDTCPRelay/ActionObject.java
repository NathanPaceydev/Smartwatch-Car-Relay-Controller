package com.controlanything.NCDTCPRelay;

public class ActionObject {
	private String command;
	private int delay;
	
	public ActionObject(String actionCommand, int actionDelay){
		command = actionCommand;
		delay = actionDelay;
	}
	
	public String getActionCommand(){
		return command;
	}
	
	public int getActionDelay(){
		return delay;
	}
	
	public void setActionCommand(String newCommand){
		command = newCommand;
	}
	
	public void setActionDelay(int newDelay){
		delay = newDelay;
	}
}
