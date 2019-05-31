package fr.upmc.pstl.exemples.basic.components;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import fr.sorbonne_u.components.annotations.OfferedInterfaces;
import fr.sorbonne_u.components.cvm.AbstractCVM;
import fr.sorbonne_u.components.exceptions.ComponentStartException;
import fr.upmc.pstl.AbstractComponentRT;
import fr.upmc.pstl.annotations.AccessType;
import fr.upmc.pstl.annotations.AccessedVars;
import fr.upmc.pstl.annotations.CyclePeriod;
import fr.upmc.pstl.annotations.Semantique;
import fr.upmc.pstl.annotations.TaskAnnotation;
import fr.upmc.pstl.exemples.basic.interfaces.ProviderI;
import fr.upmc.pstl.exemples.basic.ports.ProviderInboundPort;

@CyclePeriod(period = 1000)
@OfferedInterfaces(offered = {ProviderI.class})
public class Provider 
extends AbstractComponentRT 
implements ProviderI{
	
	
	protected ProviderInboundPort uriGetterPort;
	protected String uri;

	public Provider(
			String uri, 
			String providerPortURI, 
			Map<String,Object> vars,
			int nbThreads) throws Exception
	{
		super(uri,vars,nbThreads);
		
		this.uri = uri;
		
		this.uriGetterPort = new ProviderInboundPort(providerPortURI, this);
		
		this.addPort(this.uriGetterPort);
		
		this.uriGetterPort.publishPort();
		
		this.tracer.setTitle("provider") ;
		this.tracer.setRelativePosition(1, 0) ;

		if (AbstractCVM.isDistributed) {
			this.executionLog.setDirectory(System.getProperty("user.dir")) ;
		} else {
			this.executionLog.setDirectory(System.getProperty("user.home")) ;
		}
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
	
	
	/**
	 * Incremente la variable var1 
	 */
	@AccessedVars(accessType = { AccessType.WRITE }, vars = { "var1" })
    @TaskAnnotation(timeLimit = 600, wcet = 300 , startTime = 0)
	@Semantique
    public void incremente () {
            try {
                int var1 = (int) super.getVars().get("var1");
                this.setVar("var1", var1+1);
            	this.logMessage("Incrementing");
            } catch (Exception e) {
            	
                e.printStackTrace();
            }
    }
    
	/**
	 * Fournie la variable var1 
	 */
	@AccessedVars(accessType = { AccessType.READ }, vars = { "var1" })
    @TaskAnnotation(timeLimit = 900, wcet = 300 , startTime = 0)
    public void provide (Object[] params, CompletableFuture<Object> cf) {
	    	
            int var1 = (int) super.getVars().get("var1");
            this.logMessage("Providing "+var1);
            cf.complete(var1);
			
    }

	
}
