package fr.upmc.pstl.exemples.basic.ports;

import java.util.concurrent.CompletableFuture;

import fr.sorbonne_u.components.AbstractComponent;
import fr.sorbonne_u.components.ComponentI;
import fr.sorbonne_u.components.ports.AbstractInboundPort;
import fr.upmc.pstl.AbstractComponentRT;
import fr.upmc.pstl.TaskCommand;
import fr.upmc.pstl.TaskCommands.Provide;
import fr.upmc.pstl.exemples.basic.components.Provider;
import fr.upmc.pstl.exemples.basic.interfaces.ProviderI;

public class ProviderInboundPort 
extends AbstractInboundPort 
implements ProviderI {

	public ProviderInboundPort(String uri, ComponentI owner) throws Exception {
		super(uri, ProviderI.class ,owner);
	}

	@Override
	public void provide(Object[] params, CompletableFuture<Object> cf) throws Exception {
		
		final ProviderI provider = (ProviderI) this.getOwner();
		
		// appel asynchrone car l'appellant est un composant temps réel
		((AbstractComponentRT)provider).handleRequestAsync( 
				new AbstractComponent.AbstractService<CompletableFuture>()  { 
					@Override public CompletableFuture call() throws Exception 
					{ 
						// creation de la commande a ajouter
						TaskCommand task = new Provide ((Provider)provider ,params , cf);
						((AbstractComponentRT) provider).addCall(task);
						return cf; 
						} 
					});
	}

}
