package fr.upmc.pstl.exemples.basic;

import fr.sorbonne_u.components.cvm.utils.DCVM_Launcher;

public class DCVMRT_Launcher {

	public static void	main(String[] args)
	{
		assert	args != null && args.length >= 1 ;

		try {
			DCVM_Launcher launcher = new DCVM_Launcher(args[0]) ;
			launcher.launch() ;
		} catch (Exception e) {
			throw new RuntimeException(e) ;
		}
	}
}
