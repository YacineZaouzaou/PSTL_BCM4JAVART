package fr.upmc.pstl.exemples.basic.interfaces;

import java.util.concurrent.CompletableFuture;

import fr.sorbonne_u.components.interfaces.OfferedI;

public interface ProviderI 
extends OfferedI{
	
	
	public void provide(Object[] params, CompletableFuture<Object> cf) throws Exception;
}
