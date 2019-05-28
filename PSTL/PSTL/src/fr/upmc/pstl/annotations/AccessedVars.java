package fr.upmc.pstl.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(ElementType.METHOD)
public @interface		AccessedVars
{
	String[] vars();	 // noms des variables partagées
	AccessType[] accessType();  
}
