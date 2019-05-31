package fr.upmc.pstl.exemples.basic;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.concurrent.CompletableFuture;

import fr.sorbonne_u.components.connectors.AbstractConnector;
import fr.upmc.pstl.exemples.basic.interfaces.ConsumerI;
import fr.upmc.pstl.exemples.basic.interfaces.ProviderI;

public class ServiceConnector 
extends AbstractConnector 
implements ConsumerI {
	
	@Override
	public void get(Object[] params, CompletableFuture<Object> cf) throws Exception {
		((ProviderI) this.offering).provide(params, cf);
	}
	
}
