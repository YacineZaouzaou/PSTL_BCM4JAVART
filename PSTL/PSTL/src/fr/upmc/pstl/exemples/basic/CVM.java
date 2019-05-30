package fr.upmc.pstl.exemples.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import fr.sorbonne_u.components.cvm.AbstractCVM;
import fr.upmc.pstl.TaskCommand;
import fr.upmc.pstl.Tasks.Get;
import fr.upmc.pstl.Tasks.Incremente;
import fr.upmc.pstl.Tasks.Provide;
import fr.upmc.pstl.exemples.basic.components.Consumer;
import fr.upmc.pstl.exemples.basic.components.Provider;

public class CVM extends AbstractCVM {
	protected static final String	PROVIDER_COMPONENT_URI = "my-provider" ;
	protected static final String	CONSUMER_COMPONENT_URI = "my-consumer" ;
	
	protected static final String	URIGetterOutboundPortURI = "oport" ;
	protected static final String	URIProviderInboundPortURI = "iport" ;
	
	protected Provider provider;
	protected Consumer consumer;
	

	public CVM() throws Exception {
		super();
	}
	
	protected ArrayList<TaskCommand> tasksP = new ArrayList<TaskCommand>();
	protected ArrayList<TaskCommand> tasksC = new ArrayList<TaskCommand>();
	protected Map<String,Object> varsP = new HashMap<String,Object>();
	protected Map<String,Object> varsC = new HashMap<String,Object>();
	protected TaskCommand t1 ;//= new Incremente(provider,null,null); // Incremente la variable
	protected TaskCommand t2 ;//= new Provide(provider,null,null); // get la variable
	protected TaskCommand t3;// = new Get(consumer,null,null); // appel 
	protected int var1,var2;//,var3,var4;
	
	
	public void deploy() throws Exception {
		
		 //vars.add(var3); vars.add(var4);
		
		provider = new Provider(CVM.PROVIDER_COMPONENT_URI,CVM.URIProviderInboundPortURI,
								varsP);
		consumer = new Consumer(CVM.CONSUMER_COMPONENT_URI, CVM.URIGetterOutboundPortURI,
							varsC);
		
		
		t1 = new Incremente(provider,null,null); // Incremente la variable
		t2 = new Provide(provider,null,null); // get la variable
		t3 = new Get(consumer,null,null);
		
		tasksP.add(t1); tasksP.add(t2); tasksC.add(t3);
		varsP.put("var1",var1); varsC.put("var2",var2);				
		
		this.deployedComponents.add(provider);
		this.deployedComponents.add(consumer);
		
		consumer.doPortConnection(
				URIGetterOutboundPortURI, 
				URIProviderInboundPortURI, ServiceConnector.class.getCanonicalName());
		
		super.deploy();
	}
	
	public void shutdown() throws Exception{
		super.shutdown();
	}

	public static void main(String[] args) throws Exception {
		CVM cvm = new CVM();
		cvm.deploy();
		cvm.start();
//		Thread.sleep(5000);
//		cvm.shutdown();
//		System.exit(0);
	}
}
