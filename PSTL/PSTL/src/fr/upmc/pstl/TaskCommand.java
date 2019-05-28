package fr.upmc.pstl;

import java.util.concurrent.CompletableFuture;

public abstract class TaskCommand implements ICommand{
	protected AbstractComponentRT compRT; 
	protected Object[] params;
	protected CompletableFuture<Object> cf;
	
	public TaskCommand(AbstractComponentRT compRT,Object[] params, CompletableFuture<Object> cf) {
		this.compRT = compRT;
		this.params = params;
		this.cf = cf;	
	}

	public abstract void execute() throws Exception;
	
}
