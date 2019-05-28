package fr.upmc.pstl.exemples.basic.components;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import fr.sorbonne_u.components.annotations.RequiredInterfaces;
import fr.sorbonne_u.components.exceptions.ComponentStartException;
import fr.upmc.pstl.AbstractComponentRT;
import fr.upmc.pstl.annotations.AccessType;
import fr.upmc.pstl.annotations.AccessedVars;
import fr.upmc.pstl.annotations.CyclePeriod;
import fr.upmc.pstl.annotations.TaskAnnotation;
import fr.upmc.pstl.exemples.basic.interfaces.ConsumerI;
import fr.upmc.pstl.exemples.basic.ports.ConsumerOutBoundPort;

@CyclePeriod(period = 15)
@RequiredInterfaces(required = {ConsumerI.class})
public class Consumer 
extends AbstractComponentRT{
	
	protected ConsumerOutBoundPort uriGetterPort;

	public Consumer(
			String uri, 
			String consumerPortURI,
			Map<String,Object> vars) throws Exception
	{
		super(uri,vars);
		
		
		this.uriGetterPort = new ConsumerOutBoundPort(consumerPortURI, this);
		
		this.addPort(this.uriGetterPort);
		
		this.uriGetterPort.publishPort();
		
		
	}
		
	public ConsumerOutBoundPort getUriGetterPort () {
		return this.uriGetterPort;
	}
	
	
	
	public void start() throws ComponentStartException {
		super.start();
		try {
			cycle();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void schedul() {
        super.scheduler_multi_thread(this);
	}
	
	
    @AccessedVars(accessType = { AccessType.WRITE }, vars = { "var2" })
    @TaskAnnotation(timeLimit = 9, wcet = 3 , startTime = 0)
    public void get () {
            CompletableFuture<Object> cf = new CompletableFuture<>();
            try {
                    this.getUriGetterPort().get(null, cf);
                    System.out.println("trying to print");
                    System.out.println("getting value "+(Integer) cf.get());
            } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
            }
            
    }


}
