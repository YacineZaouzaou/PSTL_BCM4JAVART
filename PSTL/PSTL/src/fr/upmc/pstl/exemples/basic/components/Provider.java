package fr.upmc.pstl.exemples.basic.components;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import fr.sorbonne_u.components.annotations.OfferedInterfaces;
import fr.sorbonne_u.components.exceptions.ComponentShutdownException;
import fr.sorbonne_u.components.exceptions.ComponentStartException;
import fr.upmc.pstl.AbstractComponentRT;
import fr.upmc.pstl.ICommand;
import fr.upmc.pstl.TaskCommand;
import fr.upmc.pstl.Tasks.Provide;
import fr.upmc.pstl.annotations.AccessType;
import fr.upmc.pstl.annotations.AccessedVars;
import fr.upmc.pstl.annotations.CyclePeriod;
import fr.upmc.pstl.annotations.Semantique;
import fr.upmc.pstl.annotations.TaskAnnotation;
import fr.upmc.pstl.exceptions.SchedulingException;
import fr.upmc.pstl.exemples.basic.interfaces.ProviderI;
import fr.upmc.pstl.exemples.basic.ports.ProviderInboundPort;

@CyclePeriod(period = 15)
@OfferedInterfaces(offered = {ProviderI.class})
public class Provider 
extends AbstractComponentRT 
implements ProviderI{
	
	
	protected ProviderInboundPort uriGetterPort;

	public Provider(
			String uri, 
			String providerportURI, 
			Map<String,Object> vars) throws Exception
	{
		super(uri,vars,1);
		
		
		this.uriGetterPort = new ProviderInboundPort(providerportURI, this);
		
		this.addPort(this.uriGetterPort);
		
		this.uriGetterPort.publishPort();
	}
	
	
	public void start() throws ComponentStartException {
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
	
	
	@AccessedVars(accessType = { AccessType.WRITE }, vars = { "var1" })
    @TaskAnnotation(timeLimit = 9, wcet = 6 , startTime = 0)
	@Semantique
    public void incremente () {
            try {
                    int var1 = (int) this.getVars().get("var1");
                    this.setVar("var1", var1+1);
            } catch (Exception e) {
                    e.printStackTrace();
            }
    }
    
	@AccessedVars(accessType = { AccessType.READ }, vars = { "var1" })
    @TaskAnnotation(timeLimit = 9, wcet = 3 , startTime = 0)
    public void provide (Object[] params, CompletableFuture<Object> cf) {
            System.out.println("completing the cf");
            int var1 = (int) this.getVars().get("var1");
            System.out.flush();
            cf.complete(var1);
    }

	
}
