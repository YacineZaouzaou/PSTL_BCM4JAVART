package fr.upmc.pstl;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
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
	
	public boolean arret = false;
	
	public AbstractComponentRT(String uri, Map<String , Object> vars, int nbThreads) {
		super(uri,nbThreads,1);
		this.vars = vars;
		this.to_execute = new ArrayList<TaskCommand>();
		this.executors = new ArrayList<>();
		for (int i = 0 ; i < super.nbThreads ; i ++) {
			this.executors.add(new ScheduledThreadPoolExecutor(super.nbSchedulableThreads));
		}
	}
	
	public Map<String,Object> getVars(){
		return this.vars;
	}
	
	public void setVar(String varName, Object newVal){
		this.vars.put(varName, newVal);
	}
	
	
	/**
	 * ajoute une tache a la liste des taches a executer
	 * @param task la tache a ajouter
	 */
	public void addCall(ICommand task) {
		this.to_execute.add((TaskCommand) task);
	}
	
	/**
	 * recupere la prochaine tâche dans la liste d'attente
	 * @return une reference vers la prochaine tache ou null si pas de taches
	 */
	protected TaskCommand getNextTask() {
		if(to_execute.size()>0) {
			TaskCommand curr = this.to_execute.get(0);
			this.to_execute.remove(0);
			return curr;
		} return null;
	}
	

	
	
	
	/**
	 * cherche un ordonnoncement sur le plus petit nombre de threads possible et lève une exception si pas d'ordonnoncement 
	 * @param r référence vers le composant à ordonnoncer
	 * @return liste de listes 
	 */
	protected final List<Map<Method , Long>> scheduler_multi_thread (AbstractComponentRT r) throws SchedulingException{
		
		
		this.tasks_list = new ArrayList<Map<Method , Long>>();
		// looking for semantic task ?!
		List<Method> tasks = getAllMethodsAsList(r);
		
		for (int i = 1 ; i <= super.nbThreads ; i ++) {
			try {
				List<List<Method>> lists = split_list_task(tasks, i);
				System.out.println("i "+i);
				for (List l : lists) {
					this.tasks_list.add(this.scheduler(r, l));	
				}
				
				
				Map<Method , Map<String , List<Method>>> errors = checkConstraints(tasks_list);
				
				if (errors.size() == 0) {
					return tasks_list;
				}
				
				
				tasks_list = redoScheduling(r, errors, tasks_list);
				if (tasks_list == null) {
					throw new SchedulingException("impossible to find a schedul");
				}
				
				return tasks_list;


			} catch (TimeException 
					| PrecedanceException 
					| CircularityException 
					| SchedulingException 
					| NoSuchMethodException 
					| SecurityException e) {
				this.tasks_list = new ArrayList<Map<Method , Long>>();
			}
		}
		throw new SchedulingException("impossible to schedul");
	
	}
	
	
	
	
	
	
	protected List<Map<Method, Long>> redoScheduling (AbstractComponentRT r , 
			Map<Method , Map<String ,
			List<Method>>> errors , 
			List<Map<Method, Long>> ordre) {


		long end_befor; 
		long start_after;
		Long tmp;
		for (Map.Entry<Method, Map<String , List<Method>>> e : errors.entrySet()) {
			end_befor = ((TaskAnnotation) e.getKey().getAnnotation(TaskAnnotation.class)).timeLimit();
			start_after = ((TaskAnnotation) e.getKey().getAnnotation(TaskAnnotation.class)).startTime();
			for (Method m : e.getValue().get("read")) {
				tmp = getStartTime(m , ordre);
				tmp += ((TaskAnnotation) m.getAnnotation (TaskAnnotation.class)).wcet();
				if (tmp > start_after)
					start_after = tmp;
			}

			for (Method m : e.getValue().get("write")) {
				end_befor = getStartTime(e.getKey(), ordre);
				end_befor =+ ((TaskAnnotation) e.getKey().getAnnotation(TaskAnnotation.class)).wcet();
				long startTime_m = getStartTime(m, ordre);
				if (end_befor < startTime_m) {
					end_befor = startTime_m;
				}else {
					long endTime_m = startTime_m + ((TaskAnnotation) m.getAnnotation(TaskAnnotation.class)).wcet();
					if (start_after > endTime_m) {
						
					}else {
						start_after = endTime_m;
					}
				}
			}
			
			
			alterAnnotationValue(e.getKey(), 
									((AccessedVars) e.getKey().getAnnotation(AccessedVars.class)).vars(), 
									((AccessedVars) e.getKey().getAnnotation(AccessedVars.class)).accessType(), 
									end_befor, 
									start_after, 
									((TaskAnnotation) e.getKey().getAnnotation(TaskAnnotation.class)).wcet());

			/**
			 * TODO
			 * change annotation on this method for startTime and timeLimit
			 */


			try {
				Map <Method , Long> list = myList(e.getKey() , ordre);
				Map <Method , Long> tasks = this.scheduler(r, new ArrayList(list.keySet()));
				ordre.remove(list);
				ordre.add(tasks);
				if (checkConstraints(ordre).size() == 0) {
					return ordre;
				}
			}catch (Exception | TimeException | PrecedanceException exception /* which exception ? */) {
				/*don't print !*/
				exception.printStackTrace();
			}
		}
		return null;
	}
	
	protected Long getStartTime (Method m , List<Map<Method , Long>> ordre) {
		for (Map<Method , Long> o : ordre) {
			if (o.containsKey(m))
				return o.get(m);
		}
		return null;
	}
	
	protected Map<Method, Long> myList( Method m , List<Map<Method, Long>> ordre){
		for (Map <Method , Long> map : ordre) {
			if (map.containsKey(m)) 
				return map;
		}
		return null;
	}
	
	
	
	
	
	
	/**
	 * Vérifie l'existence de lectures avant ecritures et d'exclusions mutuelles non respéctées entre les sous-listes
	 * @return Correspondance entre une méthode et une liste d'autres méthodes avec lesquelles elle forme une violation des contraintes  
	 */
	protected Map<Method,Map<String,List<Method>>> checkConstraints( List<Map<Method,Long>> tasks ) {

		Map<Method,Map<String,List<Method>>> rel = new HashMap<Method, Map<String,List<Method>>>();

		for(int i=0; i<tasks.size(); i++) {
			Map<Method,Long> mapi = tasks.get(i);
			for(Map.Entry<Method, Long> mi : mapi.entrySet() ) {
				Set<String> s1read =  new HashSet<String>();
				Set<String> s1write =  new HashSet<String>();
				AccessType[] typesI = mi.getKey().getAnnotation(AccessedVars.class).accessType();
				for(int a=0; a<typesI.length; a++) {
					if(typesI[a]==AccessType.READ || typesI[a]==AccessType.BOTH)
						s1read.add(mi.getKey().getAnnotation(AccessedVars.class).vars()[a]);
					if(typesI[a]==AccessType.WRITE || typesI[a]==AccessType.BOTH)
						s1write.add(mi.getKey().getAnnotation(AccessedVars.class).vars()[a]);
				}

				for(int j=0; j<tasks.size(); j++) {
					if(i!=j) {
						Map<Method,Long> mapj = tasks.get(j);
						for(Map.Entry<Method, Long> mj : mapj.entrySet() ) {
							Set<String> s2read =  new HashSet<String>();
							Set<String> s2write =  new HashSet<String>();
							AccessType[] typesJ = mj.getKey().getAnnotation(AccessedVars.class).accessType();
							for(int a=0; a<typesJ.length; a++) {
								if(typesJ[a]==AccessType.READ || typesJ[a]==AccessType.BOTH)
									s2read.add(mj.getKey().getAnnotation(AccessedVars.class).vars()[a]);
								if(typesJ[a]==AccessType.WRITE || typesJ[a]==AccessType.BOTH)
									s2write.add(mj.getKey().getAnnotation(AccessedVars.class).vars()[a]);
							}

							Set<String> intersectionRead = new HashSet<String>(s1read); intersectionRead.retainAll(s2write);
							if(! intersectionRead.isEmpty()) {
								System.out.println(Arrays.asList("intersection "+intersectionRead).get(0));
								long start1 = mi.getValue();
								long start2 = mj.getValue();
								int wcet = (int) mj.getKey().getAnnotation(TaskAnnotation.class).wcet(); 
								if(start1 < start2+wcet) { //lecture avant ecriture
									if(rel.containsKey(mi.getKey()))
										if(rel.get(mi.getKey()).containsKey("read"))
											rel.get(mi.getKey()).get("read").add(mj.getKey());
										else {
											List<Method> l = new ArrayList<Method>(); l.add(mj.getKey());
											rel.get(mi.getKey()).put("read", l);
										}
									else {
										List<Method> l = new ArrayList<Method>(); l.add(mj.getKey());
										Map<String,List<Method>> read = new HashMap<String, List<Method>>(); read.put("read", l);
										rel.put(mi.getKey(), read);
									}
								}
							}

							Set<String> intersectionWrite = new HashSet<String>(s1write); intersectionWrite.retainAll(s2write);
							if(! intersectionWrite.isEmpty()) {
								long start1 = mi.getValue();
								long start2 = mj.getValue();
								int wcet = (int) mj.getKey().getAnnotation(TaskAnnotation.class).wcet(); 
								if(start1 > start2 && start1 <wcet) { //ecriture de la même variable au même moment par deux tâches
									if(rel.containsKey(mi.getKey()))
										if(rel.get(mi.getKey()).containsKey("write"))
											rel.get(mi.getKey()).get("write").add(mj.getKey());
										else {
											List<Method> l = new ArrayList<Method>(); l.add(mj.getKey());
											rel.get(mi.getKey()).put("write", l);
										}
									else {
										List<Method> l = new ArrayList<Method>(); l.add(mj.getKey());
										Map<String,List<Method>> read = new HashMap<String, List<Method>>(); read.put("write", l);
										rel.put(mi.getKey(), read);
									}
								}
							}
						}
					}

				}
			}

		}
		return rel;
	}
	

	
	protected final Map<Method , Long>  scheduler (AbstractComponentRT r , List<Method> tasks) throws  TimeException,
																				PrecedanceException, 
																				CircularityException, 
																				SchedulingException, 
																				NoSuchMethodException, 
																				SecurityException 
	{		
			long longer_time_offered_methods = 0;
			long smallest_time_limite_offered = ((CyclePeriod) r.getClass().getAnnotation(CyclePeriod.class)).period();
			long latest_start_time_offered = 0;
			long semantique_size = 0;
			List<Method> task_semantique = new ArrayList<>();

			for (Method t : tasks) {
				if (AbstractComponentRT.isSemantique (t)){
					task_semantique.add(t);
					semantique_size += ((TaskAnnotation)t.getAnnotation(TaskAnnotation.class)).wcet();
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
				}
			}
			
			List<Method> ect_list = createExecuteCallTask( ((CyclePeriod) r.getClass().getAnnotation(CyclePeriod.class)).period() ,
															semantique_size,
															longer_time_offered_methods,
															smallest_time_limite_offered,
															latest_start_time_offered);
			task_semantique.addAll(ect_list);
			
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
					throw new TimeException("impossible to satisfy "+TaskAnnotation.class+" deadLine");	
			}
			
			
			if (totalTime > r.getClass().getAnnotation(CyclePeriod.class).period())
				throw new TimeException("period exceeded");

			
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
			
			task_semantique.sort(new MethodComparator());
			long cycle_of_component = ((CyclePeriod) r.getClass().getAnnotation(CyclePeriod.class)).period();

			Map<Method , Long> ord = new HashMap<Method, Long>();
			
			if(ect_list.size() != 0) {
				while(thereIsECT(task_semantique)) {
					ord = this.sched(task_semantique , new HashMap<Method, Long> () , cycle_of_component ,0);
					
					if (ord.size() == task_semantique.size())
						break;
					
					Iterator<Method> it = task_semantique.iterator();
					while (it.hasNext()) {
						Method m = it.next();
						if (m.getName()=="executeCallTask") {
							task_semantique.remove(m);
							break;
						}
					}
				}
				
				if (!thereIsECT(task_semantique)) {
					throw new SchedulingException("impossible to schedul on one thread ");
				}
			}else { 
				ord = this.sched(task_semantique , new HashMap<Method, Long> () , cycle_of_component ,0);
				if (ord.size() != task_semantique.size())
					throw new SchedulingException("impossible to schedul on one thread ");
			}
			return ord;
	}
	
	protected boolean thereIsECT(List<Method> tasks) {
		int cpt = 0;
		for(Method m : tasks)
			if(m.getName()=="executeCallTask")
				cpt++;
		return cpt>0;
	}
	
	
	/**
	 * crée la liste des ExecuteCallTasks
	 * @return liste des ExecuteCallTasks créés
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 */
	private List<Method> createExecuteCallTask(long periode , 
												long semantique_time , 
												long largest_wcet,
												long smallest_timeLimit,
												long largest_startTime) throws NoSuchMethodException, SecurityException {
		List<Method> offered = allOffredMethod();
		
		if (offered.size() == 0) {
			return new ArrayList<Method>();
		}

		Map<String , AccessType> variables = new HashMap<String, AccessType>();
		
	
		String [] vars;
		AccessType [] types;
		for (Method mI : offered) {
			Class [] params = {java.lang.Object[].class, java.util.concurrent.CompletableFuture.class};
			Method m = this.getClass().getMethod(mI.getName(),params);
			vars = ((AccessedVars) m.getAnnotation(AccessedVars.class)).vars();
			types = ((AccessedVars) m.getAnnotation(AccessedVars.class)).accessType();
			for (int i = 0 ; i < vars.length ; i ++) {
				if (!variables.containsKey(vars[i])) {
					variables.put(vars[i], types[i]);
				}else {
					if (!variables.get(vars[i]).equals(types[i])) 
						variables.put(vars[i], AccessType.BOTH);
				}
			
			}
		}
		
		String [] vars_accessed = new String[variables.size()];
		AccessType [] typeAccess = new AccessType[variables.size()];
		int j = 0;
		for (Map.Entry<String, AccessType> e : variables.entrySet()) {
			vars_accessed[j] = e.getKey();
			typeAccess[j++] = e.getValue();
		}
		
		List<Method> ect_list = new ArrayList<>();
		int nb_ect = 0;
		if(largest_wcet!=0)
			nb_ect = (int) ((periode - semantique_time)/largest_wcet);

		try {
			for( int i = 0 ; i < nb_ect ; i ++) {
				Method m = AbstractComponentRT.class.getMethod("executeCallTask");
				alterAnnotationValue(m , vars_accessed , typeAccess,
										smallest_timeLimit, largest_startTime, largest_wcet  );

				ect_list.add(m);
			}
		}catch( Exception e ) {
			e.printStackTrace();
			return null;
		}
		
		return ect_list;
	}
	
	
	
	/**
	 * used in createExecuteCallTask to get the value of wcet, timeLimit ...
	 * @return all offered method by the component
	 */
	private List<Method> allOffredMethod () {
		List<Method> offered = new ArrayList<Method>();
		OfferedInterfaces oi = ((OfferedInterfaces)this.getClass().getAnnotation(OfferedInterfaces.class));
		if(oi == null)
			return offered;
		Class[] offred = oi.offered();
		for (Class c : offred) {
			Method [] methods = c.getDeclaredMethods();
			for (int i = 0 ; i < methods.length ; i ++ ) {
				offered.add(methods[i]);
			}
		}
		return offered;
	}
	
	
	
	private void alterAnnotationValue (	Method m ,
										String [] vars ,
										AccessType[] type, 
										long timeLimit , 
										long startTime , 
										long wcet) 
	{
		try {

			
			Object handler_TaskAnnotation = Proxy.getInvocationHandler(m.getAnnotation(TaskAnnotation.class));
            Field memberValue_field_TaskAnnotation = handler_TaskAnnotation.getClass().getDeclaredField("memberValues");
            memberValue_field_TaskAnnotation.setAccessible(true);
            Map <String , Object> memeberValues_map_TaskAnnotation = (Map<String , Object>) memberValue_field_TaskAnnotation.get(handler_TaskAnnotation);

            
            memeberValues_map_TaskAnnotation.put("wcet", (long) wcet);
            memeberValues_map_TaskAnnotation.put("timeLimit", (long)timeLimit);
            memeberValues_map_TaskAnnotation.put("startTime", (long)startTime);
            
            
            

            Object handler_VarAnnotation = Proxy.getInvocationHandler(m.getAnnotation(AccessedVars.class));
            Field memberValue_field_VarAnnotation = handler_VarAnnotation.getClass().getDeclaredField("memberValues");
            memberValue_field_VarAnnotation.setAccessible(true);
            Map <String , Object> memeberValues_map_VarAnnotation = (Map<String , Object>) memberValue_field_VarAnnotation.get(handler_VarAnnotation);
            
            memeberValues_map_VarAnnotation.put("vars", vars);
            memeberValues_map_VarAnnotation.put("accessType", type);
            
            System.out.println("var "+vars[0]);
            
		} catch (Exception  e) {
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
			if ((e1.getValue() + ((TaskAnnotation) e1.getKey().getAnnotation(TaskAnnotation.class)).wcet()) > e1.getKey().getAnnotation(TaskAnnotation.class).timeLimit()) {
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
			if (m[i].isAnnotationPresent(TaskAnnotation.class) && m[i].getName()!="executeCallTask") {
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
				for (Method m : oi.getDeclaredMethods()) {
					methods.add(m);
				}
			}
		}
		return methods;
	}
	
	
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

		String [] var_accessed = ((AccessedVars) task.getAnnotation(AccessedVars.class)).vars();
		AccessType [] type_access = ((AccessedVars) task.getAnnotation(AccessedVars.class)).accessType();
		Set<String> s1 = filtre_by_accessing_type(var_accessed , type_access);
		
		String [] var_accessed_by_other;
		AccessType [] type_access_by_other;
		
		for (Method t : list) {
			var_accessed_by_other = ((AccessedVars) t.getAnnotation(AccessedVars.class)).vars();
			type_access_by_other = ((AccessedVars) t.getAnnotation(AccessedVars.class)).accessType();
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
	
	
	public void cycle () throws Exception {
		System.out.println("cycle is launched");
		long cycle_time = ((CyclePeriod) this.getClass().getAnnotation(CyclePeriod.class)).period();

		for (Map<Method , Long> i : this.tasks_list) {
			for (Map.Entry<Method, Long> t : i.entrySet()) {
			this.executors.get(this.tasks_list.indexOf(i)).scheduleAtFixedRate(()-> {
				try {
					t.getKey().invoke(this);
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
		ICommand task = getNextTask();
		if(task!=null) {
			((TaskCommand)task).execute();
		}
	}
	
	
}
