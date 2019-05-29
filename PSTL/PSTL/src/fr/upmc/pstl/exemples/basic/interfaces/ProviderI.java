package fr.upmc.pstl.exemples.basic.interfaces;

import java.util.concurrent.CompletableFuture;

import fr.sorbonne_u.components.interfaces.OfferedI;
import fr.upmc.pstl.annotations.AccessType;
import fr.upmc.pstl.annotations.AccessedVars;
import fr.upmc.pstl.annotations.TaskAnnotation;

public interface ProviderI 
extends OfferedI{
	
	@AccessedVars(accessType = { AccessType.READ }, vars = { "var1" })
    @TaskAnnotation(timeLimit = 9, wcet = 3 , startTime = 0)
	public void provide(Object[] params, CompletableFuture<Object> cf) throws Exception;
}
