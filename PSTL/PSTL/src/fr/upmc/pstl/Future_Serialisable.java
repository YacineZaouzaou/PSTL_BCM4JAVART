package fr.upmc.pstl;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.Remote;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Future_Serialisable <T> implements Serializable , Future<T>, Remote {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private T t;
	
	private boolean isDone = false;
	
	private boolean isCanceled = false;
	
	
	private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException
	{      
		System.out.println("passe ici ********* passse ici");
		this.t = (T)aInputStream.readObject();
		isDone = aInputStream.readBoolean();
		isCanceled = aInputStream.readBoolean();
		System.out.println("infalting "+t);
	}

	private void writeObject(ObjectOutputStream aOutputStream) throws Exception
	{
		aOutputStream.writeObject(t);
		aOutputStream.writeBoolean(isDone);
		aOutputStream.writeBoolean(isCanceled);
	}

	
	public boolean complete (T val) {
		
		this.t = val;
		this.isDone = true;
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter("/home/zineddine/Bureau/Basic_cs/complete.txt"));
		    writer.write("OKOKOK");
		    writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		synchronized (t) {
			notify();
		}

		
		return true;
	}
	
	
	@Override
	public boolean cancel(boolean arg0) {
		return this.isCanceled = true;
	}



	@Override
	public T get() throws InterruptedException, ExecutionException {
		if(!isDone) {
			synchronized (t) {
				wait();
			}
		}
		return t;
	}



	@Override
	public T get(long arg0, TimeUnit arg1) throws InterruptedException, ExecutionException, TimeoutException {
		return t;
	}



	@Override
	public boolean isCancelled() {
		return isCanceled;
	}



	@Override
	public boolean isDone() {
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter("/home/zineddine/Bureau/Basic_cs/isDone.txt"));
		    writer.write(((Boolean)isDone).toString());
		    writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return isDone;
	}
}