package fr.upmc.pstl.exemples.basic.components;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import fr.sorbonne_u.components.annotations.RequiredInterfaces;
import fr.sorbonne_u.components.exceptions.ComponentShutdownException;
import fr.sorbonne_u.components.exceptions.ComponentStartException;
import fr.upmc.pstl.AbstractComponentRT;
import fr.upmc.pstl.annotations.AccessType;
import fr.upmc.pstl.annotations.AccessedVars;
import fr.upmc.pstl.annotations.CyclePeriod;
import fr.upmc.pstl.annotations.Semantique;
import fr.upmc.pstl.annotations.TaskAnnotation;
import fr.upmc.pstl.exceptions.SchedulingException;
import fr.upmc.pstl.exemples.basic.interfaces.ConsumerI;
import fr.upmc.pstl.exemples.basic.ports.ConsumerOutBoundPort;

@CyclePeriod(period = 1500)
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
		
		this.uriGetterPort.publishPort();
		
		
	}
		
	public ConsumerOutBoundPort getUriGetterPort () {
		return this.uriGetterPort;
	}
	
	
	
	public void start() throws ComponentStartException {
		System.out.println("starting : "+this);
		super.start();
		this.schedul();
		try {
			cycle();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void schedul() {
		try  {
			super.scheduler_multi_thread(this);
		}catch (SchedulingException e) {
			try {
				this.shutdown();
			} catch (ComponentShutdownException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}
	
	
	CompletableFuture<Object> cf = new CompletableFuture<>();
	boolean first = true;
	
	@AccessedVars(accessType = { AccessType.WRITE }, vars = { "var2" })
    @TaskAnnotation(timeLimit = 9, wcet = 3 , startTime = 0)
	@Semantique
    public void get () {
            try {
            	if (first) {
            		first = false;
                    this.getUriGetterPort().get(null, cf);

            	}
            	if (cf.isDone()) {
            		System.out.println("getting value "+(Integer) cf.get());
            		cf = new CompletableFuture<>();
                    this.getUriGetterPort().get(null, cf);
                    
            	}else {
            		System.out.println("no response from provider");
            	}
                    System.out.flush();
            } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
            }
            
    }
	
//	@AccessedVars(accessType = { AccessType.WRITE }, vars = { "var2" })
//    @TaskAnnotation(timeLimit = 9, wcet = 3 , startTime = 0)
//	@Semantique
//    public void get () {
//            try {
//            	CompletableFuture<Object> cf = new CompletableFuture<>();
//            	this.getUriGetterPort().get(null, cf);
//                    System.out.println("trying to print");
//            		System.out.println("getting value "+(Integer) cf.get());
//                    
//            } catch (Exception e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//            }
//            
//    }



}
