package fr.upmc.pstl.Tasks;
import java.util.concurrent.CompletableFuture;

import fr.upmc.pstl.AbstractComponentRT;
import fr.upmc.pstl.TaskCommand;
import fr.upmc.pstl.annotations.AccessType;
import fr.upmc.pstl.annotations.AccessedVars;
import fr.upmc.pstl.annotations.TaskAnnotation;
import fr.upmc.pstl.exemples.basic.components.Provider;

public class Incremente extends TaskCommand{

	public Incremente(Provider provider, Object[] params, CompletableFuture<Object> cf) {
		super(provider, params, cf);
	}

	@Override
	public void execute() throws Exception{
		int var1 = (int)super.compRT.getVars().get("var1");
		super.compRT.setVar("var1", var1+1);
	}
}
