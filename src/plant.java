import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import de.mz.jk.jsix.libs.XJava;
import de.mz.jk.jsix.utilities.CSVUtils;
import de.mz.jk.jsix.utilities.Settings;
import de.mz.jk.ms.peak.reader.*;
import de.mz.jk.ms.plant.AlignmentParameters;
import de.mz.jk.ms.plant.AlignmentRun;
import de.mz.jk.ms.plant.out.*;

/** PeakListTimeWarpingTool, , 26.02.2014*/
/**
 * PLAnT - Peak List Alignment Tool
 * 
 * TODO don't use fix list sizes for iterations but break down peak lists until threshold value
 * 
 * <h3>{@link plant}</h3>
 * @author kuharev
 * @version 26.02.2014 13:53:48
 */
public class plant
{
	private static Logger log = Logger.getLogger( plant.class );
	private final static File log_file = new File( "log4j.xml" );
	public final static String version = "2016-06-21";

	private static void init_log()
	{
		try
		{
			if (log_file.exists())
				DOMConfigurator.configure( log_file.getPath() );
			else 
				DOMConfigurator.configure( ClassLoader.getSystemResource( log_file.getName() ) );
		}
		catch (Exception e)
		{
		}
	}

	public static void main(String[] args)
	{
		init_log();
		new plant( args );
	}

	private PeakListReader[] peakReaders = new PeakListReader[]
	{
			new MzXmlPeakListReader(),
			new MassSpectrumXmlPeakListReader(),
			new MzmlPeakListReader(),
			new MgfPeakListReader()
	};


	private AlignmentOutput[] alignmentOutputters = new AlignmentOutput[]
	{
			new AllSourcePeaksMappingOutput(),
			new InterpolatedLookUpMappingOutput(),
			new TimeReducedMatchesOutput(),
			new TimeReducedSourcePeaksMappingOutput(),
			new SimplePlotOutput(),
			new StatisticsOutput()
	};

	private String iniFileComment = "Peak List Alignment Tool (build " + version + "), (c) Jørg Kuharev";
	private Settings defaultConfig = new Settings( "plant.ini", iniFileComment );
	private CSVUtils csvUtils = new CSVUtils();
	private AlignmentParameters alignmentParameters = new AlignmentParameters();
	private PlantCLI plantCLI = new PlantCLI( getOutputOptions() );

	public plant(String[] args)
	{
		try
		{
			log.info( iniFileComment );
			if (args.length < 1) throw new Exception( "empty command line!" );
			log.info( "parsing command line options: " + XJava.joinArray( args, " " ) );
			CommandLine cli = plantCLI.parseCommandLine( args );
			
			if (cli.hasOption( "i" ))
			{
				File iniFile = new File( cli.getOptionValue( "i" ) );
				if (!iniFile.exists()) throw new Exception( "specified configuration file does not exist!" );
				if (!iniFile.canRead()) throw new Exception( "specified configuration file is not readable!" );
				loadConfig( new Settings( cli.getOptionValue( "i" ), iniFileComment ) );
			}
			else
			{
				loadConfig( defaultConfig );
			}
			
			AlignmentRun run = new AlignmentRun();
				run.setAlignmentParameters( alignmentParameters );
				run.setPeakReaders( Arrays.asList( peakReaders ) );
			run.setCSV( csvUtils );
			
			// test if both input files are set
			log.info( "checking input files ..." );
			if (cli.hasOption( "s" ))
				run.setSourceFile( new File( cli.getOptionValue( "s" ) ) );
			else 
				throw new Exception( "missing source file!" );
			
			if (cli.hasOption( "r" ))
				run.setReferenceFile( new File( cli.getOptionValue( "r" ) ) );
			else 
				throw new Exception( "missing reference file!" );
			
			// check if any output option is set
			log.info( "checking output files ..." );
			List<AlignmentOutput> cliOutputters = getCliOutputters( cli );
			if (cliOutputters.size() < 1)
				throw new Exception( "missing output option!" );
			run.setAlignmentOutputters( cliOutputters );
			
			// run the time alignment process
			run.run();
		}
		catch (Exception e)
		{
			log.error( "execution stopped: " + e.getMessage() );
			log.debug( "execution stopped: ", e );
			plantCLI.showHelp();
		}
	}

	/**
	 * parse outputters from command line
	 */
	private List<AlignmentOutput> getCliOutputters(CommandLine cli) throws Exception
	{
		List<AlignmentOutput> userOutputters = new ArrayList<AlignmentOutput>();
		for ( int i = 0; i < alignmentOutputters.length; i++ )
		{
			AlignmentOutput outputter = alignmentOutputters[i];
			String optID = outputter.getCliOption().getOpt();
			if (cli.hasOption( optID ))
			{
				String optValue = cli.getOptionValue( optID );
				if (optValue != null) outputter.setOutputFile( new File( optValue ) );
				userOutputters.add( outputter );
			}
		}
		return userOutputters;
	}

	private List<Option> getOutputOptions()
	{
		List<Option> outputOptions = new ArrayList<Option>( alignmentOutputters.length );
		for ( int i = 0; i < alignmentOutputters.length; i++ )
		{
			outputOptions.add( alignmentOutputters[i].getCliOption() );
		}
		return outputOptions;
	}

	private void loadConfig(Settings cfg)
	{
		log.info( "loading alignment parameters from file: " + cfg.getConfigurationFilePath() );
		csvUtils.setDecPoint( XJava.stripQuotation( cfg.getStringValue( "csvUtils.decPoint", CSVUtils.defaultDecPoint, false ) ) )
				.setColSep( XJava.stripQuotation( cfg.getStringValue( "csvUtils.colSep", CSVUtils.defaultColSep, false ) ) )
				.setQuoteChar( XJava.stripQuotation( cfg.getStringValue( "csvUtils.quoteChar", CSVUtils.defaultQuoteChar, false ) ) );

		alignmentParameters
				.setForceFullAlignment(
						cfg.getBooleanValue( "alignment.forceFullAlignment", alignmentParameters.getForceFullAlignment(), false ) )
				.setMinPeaksForIterativeAlignment(
						cfg.getIntValue( "alignment.minPeaksForIterativeAlignment", alignmentParameters.getMinPeaksForIterativeAlignment(), false ) )
				.setCorridorRadius(
						cfg.getDoubleValue( "alignment.pathRefinementRadius", alignmentParameters.getCorridorRadius(), false ) )
				.setMaxDeltaDriftTime(
						cfg.getDoubleValue( "alignment.match.maxDeltaDriftTimeBIN", alignmentParameters.getMaxDeltaDriftTime(), false ) )
				.setMaxDeltaMassPPM(
						cfg.getDoubleValue( "alignment.match.maxDeltaMassPPM", alignmentParameters.getMaxDeltaMassPPM(), false ) )
				.setPathMode( AlignmentParameters.PathMode.fromString(
						cfg.getStringValue( "alignment.pathMode", alignmentParameters.getPathMode().toString(), false ) ) )
				.setMaxProcessForkingDepth(
						cfg.getIntValue( "alignment.processForkingDepth", alignmentParameters.getMaxProcessForkingDepth(), false ) );
	}
}
