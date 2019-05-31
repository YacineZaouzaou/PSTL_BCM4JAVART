package fr.upmc.pstl.exemples.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import fr.sorbonne_u.components.cvm.AbstractCVM;
import fr.upmc.pstl.TaskCommand;
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
	
	protected Map<String,Object> varsP = new HashMap<String,Object>();
	protected Map<String,Object> varsC = new HashMap<String,Object>();
	protected int var1,var2;
	
	
	public void deploy() throws Exception {
		
		provider = new Provider(CVM.PROVIDER_COMPONENT_URI,CVM.URIProviderInboundPortURI,
								varsP,2);
		consumer = new Consumer(CVM.CONSUMER_COMPONENT_URI, CVM.URIGetterOutboundPortURI,
							varsC,1);
		
		varsP.put("var1",var1); varsC.put("var2",var2);				
		
		this.deployedComponents.add(provider);
		this.deployedComponents.add(consumer);
		

		this.provider.toggleTracing() ;
		this.provider.toggleLogging() ;

		this.consumer.toggleTracing() ;
		this.consumer.toggleLogging() ;
		
		
		consumer.doPortConnection(
				URIGetterOutboundPortURI, 
				URIProviderInboundPortURI, ServiceConnector.class.getCanonicalName());
		
		super.deploy();
		assert this.deploymentDone();
	}
	
	public void shutdown() throws Exception{
		super.shutdown();
	}

	public static void main(String[] args) throws Exception {
		CVM cvm = new CVM();
		cvm.startStandardLifeCycle(20000L) ;
		Thread.sleep(15000);
		cvm.shutdown();
		System.exit(0);
	}
}
