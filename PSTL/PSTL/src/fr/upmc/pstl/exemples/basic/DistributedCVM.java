package fr.upmc.pstl.exemples.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import fr.sorbonne_u.components.cvm.AbstractDistributedCVM;
import fr.upmc.pstl.TaskCommand;
import fr.upmc.pstl.Tasks.Get;
import fr.upmc.pstl.Tasks.Incremente;
import fr.upmc.pstl.Tasks.Provide;
import fr.upmc.pstl.exemples.basic.components.Consumer;
import fr.upmc.pstl.exemples.basic.components.Provider;

public class DistributedCVM 
extends		AbstractDistributedCVM
{
	protected static final String	PROVIDER_COMPONENT_URI = "my-provider" ;
	protected static final String	CONSUMER_COMPONENT_URI = "my-consumer" ;

	// URI of the CVM instances as defined in the config.xml file
	protected static String			PROVIDER_JVM_URI = "provider" ;
	protected static String			CONSUMER_JVM_URI = "consumer" ;

	protected static String			URIConsumerOutboundPortURI = "oport" ;
	protected static String			URIProviderInboundPortURI = "iport" ;

	protected Provider	provider ;
	protected Consumer	consumer ;

	public				DistributedCVM(String[] args)
	throws Exception
	{
		super(args);
	}
	
	
	protected ArrayList<TaskCommand> tasksP = new ArrayList<TaskCommand>();
	protected ArrayList<TaskCommand> tasksC = new ArrayList<TaskCommand>();
	protected Map<String,Object> varsP = new HashMap<String,Object>();
	protected Map<String,Object> varsC = new HashMap<String,Object>();
	protected TaskCommand t1 ;//= new Incremente(provider,null,null); // Incremente la variable
	protected TaskCommand t2 ;//= new Provide(provider,null,null); // get la variable
	protected TaskCommand t3;// = new Get(consumer,null,null); // appel 
	protected int var1,var2;//,var3,var4;
	
	
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
								varsP, tasksP) ;
			
			t1 = new Incremente(provider,null,null); // Incremente la variable
			t2 = new Provide(provider,null,null); // get la variable
			
			tasksP.add(t1); tasksP.add(t2);
			varsP.put("var1",var1);
			
			provider.schedul();
			
			// make it trace its operations; comment and uncomment the line to see
			// the difference
			// uriProvider.toggleTracing() ;
			provider.toggleLogging() ;
			// add it to the deployed components
			this.addDeployedComponent(provider) ;
			assert	this.consumer == null && this.provider != null ;

		} else if (thisJVMURI.equals(CONSUMER_JVM_URI)) {

			// create the consumer component
			this.consumer = new Consumer(CONSUMER_COMPONENT_URI,
											   URIConsumerOutboundPortURI,
											   varsC) ;
			
			t3 = new Get(consumer,null,null);
			tasksC.add(t3);
			varsC.put("var2",var2);	
			
			provider.schedul();
			
			// make it trace its operations; comment and uncomment the line to see
			// the difference
			// uriConsumer.toggleTracing() ;
			consumer.toggleLogging() ;
			// add it to the deployed components
			this.addDeployedComponent(consumer) ;
			assert	this.consumer != null && this.provider == null ;

		} else {

			System.out.println("Unknown JVM URI... " + thisJVMURI) ;

		}

		super.instantiateAndPublish();
	}
	
}
