package fr.upmc.pstl.Tasks;
import java.util.concurrent.CompletableFuture;

import fr.upmc.pstl.AbstractComponentRT;
import fr.upmc.pstl.TaskCommand;
import fr.upmc.pstl.exemples.basic.components.Consumer;


public class Get extends TaskCommand {

	public Get(AbstractComponentRT compRT, Object[] params, CompletableFuture<Object> cf) {
		super(compRT, params, cf);
	}

	@Override
	public void execute() throws Exception {
		CompletableFuture<Object> cf = new CompletableFuture<>();
		System.out.println("Dans get");
		((Consumer) this.compRT).getUriGetterPort().get(null, cf);
		System.out.println("trying to print");
		System.out.println("getting value "+(Integer) cf.get());
	}

}
