package fr.upmc.pstl.exemples.basic.components;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import fr.sorbonne_u.components.annotations.RequiredInterfaces;
import fr.sorbonne_u.components.cvm.AbstractCVM;
import fr.sorbonne_u.components.exceptions.ComponentStartException;
import fr.upmc.pstl.AbstractComponentRT;
import fr.upmc.pstl.annotations.AccessType;
import fr.upmc.pstl.annotations.AccessedVars;
import fr.upmc.pstl.annotations.CyclePeriod;
import fr.upmc.pstl.annotations.Semantique;
import fr.upmc.pstl.annotations.TaskAnnotation;
import fr.upmc.pstl.exemples.basic.interfaces.ConsumerI;
import fr.upmc.pstl.exemples.basic.ports.ConsumerOutBoundPort;

@CyclePeriod(period = 2000)
@RequiredInterfaces(required = {ConsumerI.class})
public class Consumer 
extends AbstractComponentRT{
	
	protected ConsumerOutBoundPort uriGetterPort;

	public Consumer(
			String uri, 
			String consumerPortURI,
			Map<String,Object> vars,
			int nbThreads) throws Exception
	{
		super(uri,vars,nbThreads);
		
		
		this.uriGetterPort = new ConsumerOutBoundPort(consumerPortURI, this);
		
		this.addPort(this.uriGetterPort);
		
		this.uriGetterPort.localPublishPort() ;
		
		if (AbstractCVM.isDistributed) {
			this.executionLog.setDirectory(System.getProperty("user.dir")) ;
		} else {
			this.executionLog.setDirectory(System.getProperty("user.home")) ;
		}
		
	}
		
	public ConsumerOutBoundPort getUriGetterPort () {
		return this.uriGetterPort;
	}
	
	
	
	public void start() throws ComponentStartException {
		this.logMessage("starting : "+this.getClass().getSimpleName());
		super.start();
		super.scheduler_multi_thread(this);
		try {
			cycle();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	CompletableFuture<Object> cf = new CompletableFuture<>();
	boolean first = true;
	
	@AccessedVars(accessType = { AccessType.WRITE }, vars = { "var2" })
    @TaskAnnotation(timeLimit = 9, wcet = 3 , startTime = 0)
	@Semantique
    public void get ()  {
            try {
            	if (first) {
            		first = false;
                    this.uriGetterPort.get(null, cf);
            	}
            	if (cf.isDone()) {
            		this.logMessage("getting value "+(Integer) cf.get());
            		cf = new CompletableFuture<>();
                    this.uriGetterPort.get(null, cf);
                    
            	}
            	
            } catch (Exception e) {	
            	this.logMessage(e.getMessage()+" "+e.getCause());
                e.printStackTrace();
            }
            
    }

}
