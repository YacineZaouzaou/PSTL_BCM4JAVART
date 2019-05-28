package fr.upmc.pstl;

import java.lang.reflect.Method;
import java.util.Comparator;

import fr.upmc.pstl.annotations.AccessType;
import fr.upmc.pstl.annotations.AccessedVars;
import fr.upmc.pstl.annotations.TaskAnnotation;

public class MethodComparator implements Comparator<Method> {

	@Override
	public int compare(Method t1, Method t2) {
//		System.out.println(t1.getClass().getName()+" "+t2.getClass().getName());
		String [] vars1 = t1.getAnnotation(AccessedVars.class).vars();
		AccessType [] vars1Access = t1.getAnnotation(AccessedVars.class).accessType();
		String [] vars2 = t2.getAnnotation(AccessedVars.class).vars();
		AccessType [] vars2Access = t2.getAnnotation(AccessedVars.class).accessType();
		for (int i = 0 ; i < vars1.length ; i ++) {
			for (int j = 0 ; j < vars2.length ; j ++) {
				if (vars1[i].equals(vars2[j])) {
					if ((vars1Access[i] == AccessType.BOTH || vars1Access[i] == AccessType.WRITE) 
							&& vars2Access[j] == AccessType.READ ) {
						return -1;
					}
					if ((vars2Access[i] == AccessType.BOTH || vars2Access[i] == AccessType.WRITE) 
							&& vars1Access[j] == AccessType.READ ) {
						return 1;
					}
				}
			}
		}
		TaskAnnotation a1 = (TaskAnnotation) t1.getAnnotation(TaskAnnotation.class);
		TaskAnnotation a2 = (TaskAnnotation) t2.getAnnotation(TaskAnnotation.class);
		
		if (a1.timeLimit() < a2.timeLimit())
			return -1;
		if (a2.timeLimit() < a2.timeLimit())
			return 1;
		
		if (a1.wcet() < a2.wcet())
			return -1;
		if (a1.wcet() > a2.wcet())
			return 1;
		
		return 0;
	}

	
}