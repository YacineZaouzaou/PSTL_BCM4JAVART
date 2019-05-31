package fr.upmc.pstl.exemples.basic.interfaces;

import java.util.concurrent.CompletableFuture;

import fr.sorbonne_u.components.interfaces.RequiredI;
import fr.upmc.pstl.Future_Serialisable;

public interface ConsumerI 
extends RequiredI
{
	public void get(Object[] params, CompletableFuture<Object> cf) throws Exception;
}
