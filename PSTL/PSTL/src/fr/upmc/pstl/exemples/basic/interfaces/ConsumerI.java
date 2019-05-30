package fr.upmc.pstl.exemples.basic.interfaces;

import java.util.concurrent.CompletableFuture;

import fr.sorbonne_u.components.interfaces.RequiredI;
import fr.upmc.pstl.annotations.AccessType;
import fr.upmc.pstl.annotations.AccessedVars;
import fr.upmc.pstl.annotations.TaskAnnotation;

public interface ConsumerI 
extends RequiredI
{
	public void get(Object[] params, CompletableFuture<Object> cf) throws Exception;
}
