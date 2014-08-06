package com.scpi;

public class Common {
	public String command=new String(),ip=new String(),id=new String();
	public int port;
	private static Common instance = null;
    
    public static Common getInstance() {
      if(instance == null) {
         instance = new Common();
      }
      return instance;
   }
    
	
	protected Common(){
		
	}

	public void add_params(String ip,int port,String cmd){
		command=cmd;
		this.port=port;
		this.ip=ip;
		
	}
}
