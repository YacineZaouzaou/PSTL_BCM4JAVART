package fr.upmc.pstl.exemples.basic.interfaces;

import java.util.concurrent.CompletableFuture;

import fr.sorbonne_u.components.interfaces.RequiredI;

public interface ConsumerI 
extends RequiredI
{
	public void get(Object[] params, CompletableFuture<Object> cf) throws Exception;
}
