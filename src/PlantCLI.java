
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.*;

/** PeakListTimeWarpingTool, , 26.02.2014*/
/**
 * <h3>{@link PlantCommandLineParser}</h3>
 * @author kuharev
 * @version 26.02.2014 14:02:57
 */
public class PlantCLI
{
	private CommandLine commandLine = null;
	private boolean helpAlreadyDisplayed = false;

	private Option[] defaultOptions = {
			Option.builder( "s" )
					.argName( "source file path" )
					.longOpt( "source" )
					.desc( "source peak list input file" )
					.hasArg()
					.build(),

			Option.builder( "r" )
					.argName( "reference file path" )
					.longOpt( "reference" )
					.desc( "source peak list input file" )
					.hasArg()
					.build(),

			Option.builder( "h" )
					.longOpt( "help" )
					.desc( "show usage information" )
					.build(),
					
			Option.builder( "i" )
					.argName( "configuration file path" )
					.hasArg()
					.longOpt( "ini" )
					.desc( "use speciefied configuration file" )
					.build()
	};

	private Map<String, Option> optionsMap = new HashMap<String, Option>();
	public Options definedOptions = new Options();

	public void initOptions(List<Option> additionalOptions)
	{
		// add default options
		for ( Option o : defaultOptions )
		{
			optionsMap.put( o.getOpt(), o );
			definedOptions.addOption( o );
		}
		// add additional options
		if(additionalOptions!=null) for ( Option o : additionalOptions )
		{
			optionsMap.put( o.getOpt(), o );
			definedOptions.addOption( o );
		}
	}

	public void showHelp()
	{
		if (helpAlreadyDisplayed) return;
		HelpFormatter formatter = new HelpFormatter();
		System.out.flush();
		synchronized (System.out)
		{
			System.out.println( "----------------------------------------------------------------------" );
			formatter.printHelp( "java -jar plant.jar", definedOptions );
			System.out.println( "----------------------------------------------------------------------" );
		}
		System.out.flush();
		helpAlreadyDisplayed = true;
	}

	public PlantCLI(List<Option> additionalOptions)
	{
		initOptions( additionalOptions );
	}

	public CommandLine getCommandLine()
	{
		return commandLine;
	}

	public CommandLine parseCommandLine(String[] args) throws Exception
	{
		commandLine = new DefaultParser().parse( definedOptions, args );
		for ( Option o : commandLine.getOptions() )
		{
			optionsMap.put( o.getOpt(), o );
		}
		if (commandLine.hasOption( "h" )) showHelp();
		return commandLine;
	}

	public Map<String, Option> getOptionsMap()
	{
		return optionsMap;
	}

	public void dumpOptionsMap()
	{
		for ( String key : optionsMap.keySet() )
		{
			Option opt = optionsMap.get( key );
			System.out.println( "name: " + opt.getOpt() + "; value:" + opt.getValue() );
		}
	}

	public void dumpCommandLine()
	{
		for ( Option opt : commandLine.getOptions() )
		{
			System.out.println( "name: " + opt.getOpt() + "; value:" + opt.getValue() );
		}
	}
}
