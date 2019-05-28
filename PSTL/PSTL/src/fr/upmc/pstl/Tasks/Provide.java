package fr.upmc.pstl.Tasks;
import java.util.concurrent.CompletableFuture;

import fr.upmc.pstl.TaskCommand;
import fr.upmc.pstl.exemples.basic.components.Provider;

public class Provide extends TaskCommand {

	public Provide(Provider compRT, Object[] params, CompletableFuture<Object> cf) {
		super(compRT, params, cf);
	}

	@Override
	public void execute()  throws Exception {
		((Provider)compRT).provide(params, cf);
	}
}
