package fr.upmc.pstl;


import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import fr.sorbonne_u.components.AbstractComponent;
import fr.sorbonne_u.components.annotations.OfferedInterfaces;
import fr.sorbonne_u.components.interfaces.OfferedI;
import fr.upmc.pstl.annotations.AccessType;
import fr.upmc.pstl.annotations.AccessedVars;
import fr.upmc.pstl.annotations.CyclePeriod;
import fr.upmc.pstl.annotations.Semantique;
import fr.upmc.pstl.annotations.TaskAnnotation;
import fr.upmc.pstl.exceptions.CircularityException;
import fr.upmc.pstl.exceptions.PrecedanceException;
import fr.upmc.pstl.exceptions.SchedulingException;
import fr.upmc.pstl.exceptions.TimeException;


public abstract class AbstractComponentRT extends AbstractComponent
{
	protected Map<String , Object> vars;
	protected List<Map<Method , Long>> tasks_list;
	protected List<TaskCommand> to_execute; 
	protected List<ScheduledThreadPoolExecutor> executors;
	protected int Number_of_thread;
	
	public AbstractComponentRT(String uri, Map<String , Object> vars) {
		super(uri,1,0);
		this.vars = vars;
		this.to_execute = new ArrayList<TaskCommand>();
		this.executors = new ArrayList<>();
		this.Number_of_thread = 1;
		for (int i = 0 ; i < this.Number_of_thread ; i ++) {
			this.executors.add(new ScheduledThreadPoolExecutor(1));
		}
	}
	
	public Map<String,Object> getVars(){
		return this.vars;
	}
	
	public void setVar(String varName, Object newVal){
		this.vars.put(varName, newVal);
	}
	
	public void addCall(ICommand task) {
		this.to_execute.add((TaskCommand) task);
	}
	
	protected TaskCommand getNextTask() {
		if(to_execute.size()>0) {
			TaskCommand curr = this.to_execute.get(0);
			this.to_execute.remove(0);
			return curr;
		} return null;
	}
	
	
	
	
	protected final List<Map<Method , Long>> scheduler_multi_thread (AbstractComponentRT r) {
		
		
		this.tasks_list = new ArrayList<Map<Method , Long>>();
		// looking for semantic task ?!
		List<Method> tasks = getAllMethodsAsList(r);
		
		
		
		
		for (int i = 1 ; i <= this.Number_of_thread ; i ++) {
			try {
				List<List<Method>> lists = split_list_task(tasks, i);
//				lists = fix_start_time(lists);
//				boolean accepted
				for (List l : lists) {
					this.tasks_list.add(this.scheduler(r, l));	
				}
				
				
				
				/**
				 * TODO 
				 * on parcours toute les listes et on vérifie que les lectures se passe après les ecriture 
				 * dans le cas où ça passe rien à faire
				 * dans le cas contraire detecter les taches infecté corrigé leur startTime et relancer le scheduling 
				 * normalement en faisant ça une fois on peut aboutir à un ordre possible ou confirmer qu'il y en a pas.
				 * 
				 * à faire demain
				 */
				
				
				
				
				
				return tasks_list;
			} catch (TimeException 
					| PrecedanceException 
					| CircularityException 
					| SchedulingException 
					| NoSuchMethodException 
					| SecurityException e) {
				this.tasks_list = new ArrayList<Map<Method , Long>>();
				e.printStackTrace();
			}
		}
		return null;
	
	}
	
	

	protected final Map<Method , Long>  scheduler (AbstractComponentRT r , List<Method> tasks) throws  TimeException,
																				PrecedanceException, 
																				CircularityException, 
																				SchedulingException, 
																				NoSuchMethodException, 
																				SecurityException 
	{
	
			// looking for "taches liée à la sémantique"
			/**
			 * will be changed -> it will be given directly as a parameter -> list of Methods not Task ...
			 */
			
		
			long longer_time_offered_methods = 0;
			long smallest_time_limite_offered = ((CyclePeriod) r.getClass().getAnnotation(CyclePeriod.class)).period();
			long latest_start_time_offered = 0;
			List<Method> task_semantique = new ArrayList<>();
			Map<String , AccessType> allAccessedVar = new HashMap<>();
			for (Method t : tasks) {
				if (AbstractComponentRT.isSemantique (t)){
					task_semantique.add(t);
				}else {
					long et = ((TaskAnnotation) t.getAnnotation(TaskAnnotation.class)).wcet();
					long timeLimit = ((TaskAnnotation) t.getAnnotation(TaskAnnotation.class)).timeLimit();
					long startTime = ((TaskAnnotation) t.getAnnotation(TaskAnnotation.class)).startTime();
					if (timeLimit < smallest_time_limite_offered) {
						smallest_time_limite_offered = timeLimit;
					}
					if (startTime > latest_start_time_offered) {
						latest_start_time_offered = startTime;
					}
					if (et > longer_time_offered_methods) {
						longer_time_offered_methods = et;
					}
					String [] vars = ((AccessedVars) t.getAnnotation(AccessedVars.class)).vars();
					AccessType [] type = ((AccessedVars) t.getAnnotation(AccessedVars.class)).accessType();
					for (int i = 0 ; i < vars.length ; i ++) {
						if (allAccessedVar.containsKey(vars[i])) {
							if (!allAccessedVar.get(vars[i]).equals(type[i])) {
								allAccessedVar.put(vars[i], AccessType.BOTH);
							}
						}else {
							allAccessedVar.put(vars[i] , type[i]);
						}
					}
				}
			}
			
			
			// cas triviaux d'exception
			long totalTime = 0;
			Map<String, Map<Method, AccessType>> variables = new HashMap<>();
			Map<String, AccessType> variablesAccessType = new HashMap<>();
			for (Method task : task_semantique) {
				TaskAnnotation annotation = (TaskAnnotation) task.getAnnotation(TaskAnnotation.class); 
				AccessedVars annotationAccess = (AccessedVars) task.getAnnotation(AccessedVars.class);
				
				if (annotation.timeLimit() - annotation.startTime() <= 0) {
					throw new TimeException("timeLimite - startTime < 0 in task "+task);
				}
				
				
				for (int i = 0 ; i < annotationAccess.vars().length ; i ++) {
					if (!variables.containsKey(annotationAccess.vars()[i]))
						variables.put(annotationAccess.vars()[i], new HashMap<>());
					if (!variablesAccessType.containsKey(annotationAccess.vars()[i]))
						variablesAccessType.put(annotationAccess.vars()[i], annotationAccess.accessType()[i]);
					
					variables.get(annotationAccess.vars()[i]).put(task, annotationAccess.accessType()[i]);	
					
					if (variablesAccessType.get(annotationAccess.vars()[i]) != annotationAccess.accessType()[i])
						variablesAccessType.put(annotationAccess.vars()[i], AccessType.BOTH);
				}
				totalTime += annotation.wcet();
				if (annotation.timeLimit() < annotation.wcet())
					throw new TimeException("impossible de satisfy "+TaskAnnotation.class+" deadLine");	
			}
			
			if ((totalTime+longer_time_offered_methods) > r.getClass().getAnnotation(CyclePeriod.class).period())
				throw new TimeException("period exceeded");
			
			long remined_time = r.getClass().getAnnotation(CyclePeriod.class).period() - totalTime;
			int nb_ect = 0;
			if(longer_time_offered_methods!=0)
				nb_ect =(int) (remined_time / (longer_time_offered_methods));
			System.out.println("nb ect "+nb_ect);
			
			String [] accessedVars = new String [allAccessedVar.size()];
			AccessType [] accessedVarType = new AccessType [allAccessedVar.size()];
			int index = 0;
			for (Map.Entry<String , AccessType> e : allAccessedVar.entrySet()) {
				accessedVars[index] = e.getKey();
				accessedVarType[index ++] = e.getValue();
			}
			
			for( int i = 0 ; i < nb_ect ; i ++) {
				Method m = r.getClass().getMethod("executeCallTask");
				System.out.println(smallest_time_limite_offered+" ----");
				alterAnnotationValue(m , TaskAnnotation.class , AccessedVars.class , accessedVars , accessedVarType,
										smallest_time_limite_offered, latest_start_time_offered , longer_time_offered_methods  );
				tasks.add(m);
			}
			
			
			
		
			for (String key : variablesAccessType.keySet()) {
				if ( variablesAccessType.get(key) == AccessType.READ )
					throw new PrecedanceException("trying to read unwritten variable "+key);
			}
			
			
			
			// test circularité
			for ( String s1 : variables.keySet()) {
				for (String s2 : variables.keySet()) {
					if ( ! s2.equals(s1)) {
						for (Method t1 : variables.get(s1).keySet()) {
							for (Method t2 : variables.get(s2).keySet()) {
								if ((! t1.equals(t2)) && variables.get(s2).keySet().contains(t1) && variables.get(s1).keySet().contains(t2)) {
									if (variables.get(s1).get(t1) == AccessType.READ && variables.get(s2).get(t1) == AccessType.WRITE
											&& variables.get(s1).get(t2) == AccessType.WRITE && variables.get(s2).get(t2) == AccessType.READ
											)
										throw new CircularityException("the task "+t1+" read "+s2+" and write "+s1+" and the task "+t2+" does the symestrique operation");
									
									if (variables.get(s1).get(t1) == AccessType.WRITE && variables.get(s2).get(t1) == AccessType.READ
											&& variables.get(s1).get(t2) == AccessType.READ && variables.get(s2).get(t2) == AccessType.WRITE
											)
										throw new CircularityException("the task "+t2+" read "+s1+" and write "+s2+" and the task "+t1+" does the symestrique operation");	
								}
							}
						}
					}
				}
			}
			//calcul de l'ordonnanceur
			
			// placer dans la liste des tasks un ordre (une initilisation) ex : (le temps de terminaison croissant)
			// utiliser peut être une relation d'ordre partiel -tri topologique-
			tasks.sort(new MethodComparator());
			long cycle_of_component = ((CyclePeriod) r.getClass().getAnnotation(CyclePeriod.class)).period();
			Map<Method , Long> ord = this.sched(tasks , new HashMap<Method, Long> () , cycle_of_component ,0);
			if (ord.size() != tasks.size()) {
				String end_of_ord = "";
				if (ord.size() != 0) {
					System.out.println((ord.size()-1));
					end_of_ord = ord.get(ord.size()-1).toString();
				}
				throw new SchedulingException("impossible to schedul on one thread : stops at "+end_of_ord);
			}
			for(Method t : ord.keySet()) {
				System.out.println(t);
			}
			return ord;
	}
	
	private void alterAnnotationValue (	Method m , 
										Class <? extends Annotation> a, 
										Class <? extends Annotation> b, 
										String [] vars ,
										AccessType[] type, 
										long timeLimit , 
										long startTime , 
										long wcet) 
	{
		try {
			TaskAnnotation old_annotation_task = (TaskAnnotation) m.getClass().getAnnotation(TaskAnnotation.class);
			AccessedVars old_annotation_var = (AccessedVars) m.getClass().getAnnotation(AccessedVars.class);
			
			Annotation new_annotation_task = new TaskAnnotation() {
				
				@Override
				public Class<? extends Annotation> annotationType() {
					return old_annotation_task.annotationType();
				}
				
				@Override
				public long wcet() {
					return wcet;
				}
				
				@Override
				public long timeLimit() {
					return timeLimit;
				}
				
				@Override
				public long startTime() {
					return startTime;
				}
			};
			
			Annotation new_annotation_var = new AccessedVars() {
				
				@Override
				public Class<? extends Annotation> annotationType() {
					return old_annotation_var.annotationType();
				}
				
				@Override
				public String[] vars() {
					return vars;
				}
				
				@Override
				public AccessType[] accessType() {
					return type;
				}
			};
			
			
			Object handler_TaskAnnotation = Proxy.getInvocationHandler(AbstractComponentRT.class.getDeclaredMethod("executeCallTask").getAnnotation(a));
            Field memberValue_field_TaskAnnotation = handler_TaskAnnotation.getClass().getDeclaredField("memberValues");
            System.out.println("the class of handler is : "+handler_TaskAnnotation.getClass().getCanonicalName());
            memberValue_field_TaskAnnotation.setAccessible(true);
            Map <String , Object> memeberValues_map_TaskAnnotation = (Map<String , Object>) memberValue_field_TaskAnnotation.get(handler_TaskAnnotation);
            memeberValues_map_TaskAnnotation.put("wcet", wcet);
            memeberValues_map_TaskAnnotation.put("timeLimit", timeLimit);
            memeberValues_map_TaskAnnotation.put("startTime", startTime);
            
            
            
            Object handler_VarAnnotation = Proxy.getInvocationHandler(AbstractComponentRT.class.getDeclaredMethod("executeCallTask").getAnnotation(b));
            Field memberValue_field_VarAnnotation = handler_VarAnnotation.getClass().getDeclaredField("memberValues");
            memberValue_field_VarAnnotation.setAccessible(true);
            Map <String , Object> memeberValues_map_VarAnnotation = (Map<String , Object>) memberValue_field_VarAnnotation.get(handler_VarAnnotation);
            memeberValues_map_TaskAnnotation.put("vars", vars);
            memeberValues_map_TaskAnnotation.put("accessType", type);
            
            
            
            
            
            
//			Method annotationData = Class.class.getDeclaredMethod("annotationData");
//			annotationData.setAccessible(true);
//			Object annotationField = annotationData.invoke(Method.class);
//			Field annotations = annotationField.getClass().getDeclaredField("annotations"); 
//			annotations.setAccessible(true);
//			Map<Class<? extends Annotation>, Annotation> annotation = (Map<Class<? extends Annotation>, Annotation>) annotations.get(annotationField);
//			annotation.put(a, new_annotation_task);
//			annotation.put(b, new_annotation_var);
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException | NoSuchMethodException  e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}
	
	protected final Map<Method , Long> sched (List<Method> l, Map<Method ,Long> ord , long cycle_of_component , long time_marker) 
			throws SchedulingException{
		if (l.size() == 0) {
			return ord;
		}
		List<Method> l2 = (List<Method>) ((ArrayList<Method>) l).clone();
		for (Method i : l) {
			long start_time_of_task = ((TaskAnnotation) i.getAnnotation(TaskAnnotation.class)).startTime();
			if (start_time_of_task > time_marker)
				time_marker = start_time_of_task;
			ord.put(i, time_marker);
			time_marker += ((TaskAnnotation) i.getAnnotation(TaskAnnotation.class)).wcet();
			l2.remove(i);
			if (AbstractComponentRT.checkPossibility(ord , cycle_of_component)) {
				ord = this.sched(l2, ord , cycle_of_component , time_marker);
				return ord;
			}
			l2.add(i);
			ord.remove(i);
			time_marker -= ((TaskAnnotation) i.getAnnotation(TaskAnnotation.class)).wcet();
		}
		return ord;
	}
	
	
	// il faut comparer chaque élèment avec tout ceux qui le suivent
	protected static boolean  checkPossibility (Map<Method , Long> list , long cycle_of_component) {
		MethodComparator tc = new MethodComparator();
		for (Map.Entry<Method , Long> e1 : list.entrySet()) {
			if ((e1.getValue() + ((TaskAnnotation) e1.getKey().getAnnotation(TaskAnnotation.class)).wcet()) > cycle_of_component) {
				return false;
			}
			for (Map.Entry<Method , Long> e2 : list.entrySet()) {
				if (e1.getValue() < e2.getValue() ) {
					if (tc.compare(e1.getKey(), e2.getKey()) <=0)
						continue;
					return false;
				}
			}
		}
		return true;
	}
	
	
	protected static boolean isSemantique (Method method) {
		return method.isAnnotationPresent(Semantique.class);
	}
	
	
	protected List<Method> getAllMethodsAsList(AbstractComponentRT component) {
		Method m [] = component.getClass().getMethods();
		List<Method> list = new ArrayList<Method>();
		for (int i = 0 ; i < m.length  ; i ++) {
			if (m[i].isAnnotationPresent(TaskAnnotation.class)) {
				list.add(m[i]);
			}
		}
		return list;
	}
	
	protected static List<Method> getAllOfferedMethods (AbstractComponentRT component) {
		List<Method> methods = new ArrayList<>();
		OfferedInterfaces offredInterface = component.getClass().getAnnotation(OfferedInterfaces.class);
		if (offredInterface != null) {
		Class< ? extends OfferedI>[] offeredMethod = offredInterface.offered();
			for (Class< ? extends OfferedI> oi : offeredMethod ) {
				System.out.println(oi.getClass());
				for (Method m : oi.getDeclaredMethods()) {
					methods.add(m);
				}
			}
		}
		return methods;
	}
	
	
	// last added 
	
	
	public List<List<Method>> split_list_task (List<Method> list , int nb_of_lists) {
		List<List<Method>> lists = new ArrayList<>();
		
		if (nb_of_lists <= 1) {
			lists.add(list);
			return lists;
		}
		
		
		for (int i = 0 ; i < nb_of_lists  ; i ++) {
			lists.add(new ArrayList<Method>());
		}
		
		for (int i = 0 ; i < nb_of_lists-1  ; i ++) {
			lists.get(i).add(list.get(0));
			list.remove(0);
			Iterator<Method> it = list.iterator();
			while(it.hasNext()) {
				Method cur = it.next();
				if (must_be_with(cur , lists.get(i))) {
					lists.get(i).add(cur);
					list.remove(cur);
				}
			}
		}
		
		
		//add all remaining task in the last list
		for (Method task : list)
		{
			lists.get(lists.size()-1).add(task);
		}	
		return lists;
	}
	
	
	private boolean must_be_with(Method task , List<Method> list) {
		String [] var_accessed = ((AccessedVars) task.getClass().getAnnotation(AccessedVars.class)).vars();
		AccessType [] type_access = ((AccessedVars) task.getClass().getAnnotation(AccessedVars.class)).accessType();
		Set<String> s1 = filtre_by_accessing_type(var_accessed , type_access);
		
		String [] var_accessed_by_other;
		AccessType [] type_access_by_other;
		
		for (Method t : list) {
			var_accessed_by_other = ((AccessedVars) t.getAnnotation(AccessedVars.class)).vars();
			type_access_by_other = ((AccessedVars) task.getAnnotation(AccessedVars.class)).accessType();
			Set<String> s2 = filtre_by_accessing_type(var_accessed_by_other , type_access_by_other);
			s2.retainAll(s1);
			if (s2.size() != 0) {
				return true;
			}
		}
		return false;
	}
	
	private Set<String> filtre_by_accessing_type (String [] vars , AccessType [] type) {
		Set<String> s = new HashSet<>();
		for (int i = 0 ; i < vars.length ; i ++) {
			if (type[i] == AccessType.WRITE || type[i] == AccessType.BOTH) {
				s.add(vars[i]);
			}
		}
		return s;
	}
	
	/**
	 * inutil
	 */
	private List<List<TaskCommand>> fix_start_time (List<List<TaskCommand>> lists) {
		// ici utiliser java reflect
		return null;
	}
	
	
	public void cycle () throws Exception {
		System.out.println("cycle is launched");
		long cycle_time = ((CyclePeriod) this.getClass().getAnnotation(CyclePeriod.class)).period();
		for (Map<Method , Long> i : this.tasks_list) {
			for (Map.Entry<Method, Long> t : i.entrySet()) {
			this.executors.get(this.tasks_list.indexOf(i)).scheduleAtFixedRate(()-> {
				try {
					System.out.println("running "+t);
					t.getKey().invoke(null, null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			,t.getValue(), cycle_time, TimeUnit.MILLISECONDS);
			}
		}
	}
	
	
	@Override 
	public <T> T handleRequestSync(ComponentService<T> task) throws Exception { 
		T cf = task.call(); 
		assert cf instanceof CompletableFuture; 
		((CompletableFuture)cf).get();
		return cf; 
	}

	@Override 
	public <T> void handleRequestAsync(ComponentService<T> task) throws Exception { 
		T cf = task.call(); 
		assert cf instanceof CompletableFuture; 
	}
	
	@AccessedVars(accessType = {  }, vars = {  })
	@TaskAnnotation(timeLimit = 0, wcet = 0 , startTime = 0)
	public void executeCallTask () throws Exception {
		ICommand task = this.getNextTask();
		if(task!=null) {
			((TaskCommand)task).execute();
		}
	}
	
	
}
