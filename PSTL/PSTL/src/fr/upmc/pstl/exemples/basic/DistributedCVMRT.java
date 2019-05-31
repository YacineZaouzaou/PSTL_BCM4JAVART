package fr.upmc.pstl.exemples.basic;

import java.util.HashMap;
import java.util.Map;

import fr.sorbonne_u.components.cvm.AbstractCVM;
import fr.sorbonne_u.components.cvm.AbstractDistributedCVM;
import fr.sorbonne_u.components.examples.basic_cs.connectors.URIServiceConnector;
import fr.sorbonne_u.components.helpers.CVMDebugModes;
import fr.upmc.pstl.exemples.basic.components.Consumer;
import fr.upmc.pstl.exemples.basic.components.Provider;

public class DistributedCVMRT 
extends		AbstractDistributedCVM
{
	protected static final String	PROVIDER_COMPONENT_URI = "my-provider" ;
	protected static final String	CONSUMER_COMPONENT_URI = "my-consumer" ;

	protected static String			PROVIDER_JVM_URI = "provider" ;
	protected static String			CONSUMER_JVM_URI = "consumer" ;

	protected static String			URIConsumerOutboundPortURI = "oport" ;
	protected static String			URIProviderInboundPortURI = "iport" ;

	protected Provider	provider ;
	protected Consumer	consumer ;

	public	DistributedCVMRT(String[] args, int xLayout, int yLayout)
	throws Exception 
	{
		super(args, xLayout, yLayout);
	}
	
	
	protected Map<String,Object> varsP = new HashMap<String,Object>();
	protected Map<String,Object> varsC = new HashMap<String,Object>();
	protected int var1,var2;
	
	
	@Override
	public void			initialise() throws Exception
	{
		super.initialise() ;
	}
	
	
	@Override
	public void			instantiateAndPublish() throws Exception
	{
		if (thisJVMURI.equals(PROVIDER_JVM_URI)) {

			// create the provider component
			this.provider =
				new Provider(PROVIDER_COMPONENT_URI,
								URIProviderInboundPortURI,
								varsP,1) ;
			varsP.put("var1",var1);	

			provider.toggleTracing() ;
			provider.toggleLogging() ;
			
			this.addDeployedComponent(provider) ;
			assert	this.consumer == null && this.provider != null ;

		} else if (thisJVMURI.equals(CONSUMER_JVM_URI)) {

			this.consumer = new Consumer(CONSUMER_COMPONENT_URI,
											   URIConsumerOutboundPortURI,
											   varsC,1) ;
			

			varsC.put("var2",var2);	
			
			consumer.toggleTracing() ;
			consumer.toggleLogging() ;
			
			this.addDeployedComponent(consumer) ;
			assert	this.consumer != null && this.provider == null ;

		} else {
			System.out.println("Unknown JVM URI... " + thisJVMURI) ;
		}

		super.instantiateAndPublish();
	}
	
	
	@Override
	public void			interconnect() throws Exception
	{
		assert	this.isIntantiatedAndPublished() ;

		if (thisJVMURI.equals(PROVIDER_JVM_URI)) {

			assert	this.consumer == null && this.provider != null ;

		} else if (thisJVMURI.equals(CONSUMER_JVM_URI)) {

			assert	this.consumer != null && this.provider == null ;
			// do the connection
			this.consumer.doPortConnection(
				URIConsumerOutboundPortURI,
				URIProviderInboundPortURI,
				ServiceConnector.class.getCanonicalName()) ;
			assert	this.consumer.isPortConnected(
												URIConsumerOutboundPortURI) ;

		} else {

			System.out.println("Unknown JVM URI... " + thisJVMURI) ;

		}

		super.interconnect();
	}


	@Override
	public void			finalise() throws Exception
	{

		if (thisJVMURI.equals(PROVIDER_JVM_URI)) {


		} else if (thisJVMURI.equals(CONSUMER_JVM_URI)) {

			this.consumer.doPortDisconnection(URIConsumerOutboundPortURI) ;

		} else {

			System.out.println("Unknown JVM URI... " + thisJVMURI) ;

		}

		super.finalise() ;
	}

	@Override
	public void			shutdown() throws Exception
	{
		if (thisJVMURI.equals(PROVIDER_JVM_URI)) {

			assert	this.consumer == null && this.provider != null ;
			this.provider.printExecutionLogOnFile("provider") ;

		} else if (thisJVMURI.equals(CONSUMER_JVM_URI)) {

			assert	this.consumer != null && this.provider == null ;
			// print logs on files, if activated
			this.consumer.printExecutionLogOnFile("consumer") ;

			// any disconnection not done yet can be performed here

		} else {

			System.out.println("Unknown JVM URI... " + thisJVMURI) ;

		}

		super.shutdown();
	}

	public static void	main(String[] args)
	{
		try {
			DistributedCVMRT da  = new DistributedCVMRT(args, 2, 5) ;
			da.startStandardLifeCycle(15000L) ;
			Thread.sleep(100000L) ;
			System.exit(0) ;
		} catch (Exception e) {
			throw new RuntimeException(e) ;
		}
	}
	
}
