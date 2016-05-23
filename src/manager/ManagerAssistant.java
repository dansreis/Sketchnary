package manager;

import java.util.ArrayList;

import tcpConnection.Channel;
import utilities.Constants;

public class ManagerAssistant implements Runnable {
	
	private Channel channel;
	private FailureHandler handler;
	
	private ArrayList<String> confirmation_queue;
	
	
	public ManagerAssistant(Channel channel, FailureHandler handler){
		
		this.channel = channel;
		this.handler = handler;
		
		confirmation_queue = new ArrayList<String>();
	}
	
	public void requiresConfirmation(String query){
		confirmation_queue.add(query);
	}
	
	public void start(){
		
		new Thread(this).start();
		
	}

	@Override
	public void run() {
		
		String exchange_status;
		
		while(true){
			
			for(int i = 0; i < confirmation_queue.size(); i++){
				
				exchange_status = channel.exchangeC2(false, confirmation_queue.get(i));
				if(exchange_status.equals(Constants.OK)){
					System.out.println("[MANAGER ASSISTANT] Confirmed a query | [STATUS] Code " + exchange_status);
					confirmation_queue.remove(i);
				}else{
					System.out.println("[MANAGER ASSISTANT] Confirmation failed | [STATUS] Code " + exchange_status);
					handler.addFailedQuery(confirmation_queue.get(i));
					confirmation_queue.remove(i);
				}
				
			}
		
			try {
				Thread.sleep(400);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		
	}
	
	public void freezeManager(){
		try {
			this.wait();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void defrostManager(){
		this.notify();
	}

}
