package fr.upmc.pstl.exemples.basic.ports;

import java.util.concurrent.CompletableFuture;

import fr.sorbonne_u.components.ComponentI;
import fr.sorbonne_u.components.ports.AbstractOutboundPort;
import fr.upmc.pstl.exemples.basic.interfaces.ConsumerI;

public class ConsumerOutBoundPort 
extends AbstractOutboundPort 
implements ConsumerI {

	public ConsumerOutBoundPort(String uri, ComponentI owner) throws Exception {
		super(uri, ConsumerI.class , owner);
	}

	@Override
	public void get(Object[] params, CompletableFuture<Object> cf) throws Exception{
		((ConsumerI) this.connector).get(params, cf);
	}

}
