/** PLAnT, de.mz.jk.ms.plant.out, 13.03.2014*/
package de.mz.jk.ms.plant.out;

import java.io.File;
import java.io.PrintStream;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.log4j.Logger;

import de.mz.jk.jsix.utilities.CSVUtils;
import de.mz.jk.ms.plant.AlignmentRun;

/**
 * <h3>{@link AlignmentOutput}</h3>
 * @author kuharev
 * @version 13.03.2014 17:23:01
 */
public abstract class AlignmentOutput implements Runnable
{
	private static Logger log = Logger.getLogger( AlignmentOutput.class );

	protected AlignmentRun run = null;
	
	protected String srcColTitle = "source time";
	protected String refColTitle = "reference time";
	
	protected File outFile = null;

	protected Option cliOption = null;

	public void setCliOption(Option cliOption)
	{
		this.cliOption = cliOption;
	}

	public Option getCliOption()
	{
		if(cliOption==null) setCliOption( createCliOption() );
		return cliOption;
	}
	
	abstract protected Option createCliOption();

	public AlignmentOutput setAlignmentRun(AlignmentRun run)
	{
		this.run = run;
		return this;
	}

	public AlignmentRun getAlignmentRun()
	{
		return run;
	}

	/**
	 * @param run
	 * @param async if true the outputter plugin is executed in a separate thread 
	 */
	public void run(AlignmentRun run)
	{
		setAlignmentRun( run );
		if (allowAsynchronousExecution())
		{
			this.log.debug( this.getClass().getName() + ": asynchronously running in separate thread." );
			new Thread( this ).start();
		}
		else
		{
			this.log.debug( this.getClass().getName() + ": synchronously running in main thread." );
			run();
		}
	}

	@Override public void run()
	{
		try
		{
			doOutputAction();
		}
		catch (Exception e)
		{
			this.log.debug( this.getClass().getName() + " failed to generate output.", e );
		}
	}

	/**
	 * particluar output action
	 * @throws Exception
	 */
	abstract protected void doOutputAction() throws Exception;

	public File getOutputFile()
	{
		return outFile;
	}

	public AlignmentOutput setOutputFile(File outFile) throws Exception
	{
		if (!outFile.exists()) outFile.createNewFile();
		if (!outFile.canWrite()) throw new Exception( "can not write to file " + outFile );
		this.outFile = outFile;
		return this;
	}

	protected void writePeaksToCSV(List<Double> src, List<Double> ref, File outFile)
	{
		PrintStream out = null;
		try
		{
			log.info( "writing output to file: " + outFile );
			out = new PrintStream( outFile );
			CSVUtils csv = run.getCSV();
			csv.printTxtCell( out, srcColTitle ).printColSep( out ).printTxtCell( out, refColTitle ).endLine( out );
			int n = Math.min( src.size(), ref.size() );
			for ( int i = 0; i < n; i++ )
			{
				csv.printNumCell( out, src.get( i ) ).printColSep( out ).printNumCell( out, ref.get( i ) ).endLine( out );
			}
			out.flush();
		}
		catch (Exception e)
		{
			log.debug( "failed to write output file: " + outFile, e );
		}
		finally
		{
			try
			{
				out.close();
			}
			catch (Exception ex)
			{}
		}
	}
	
	/**
	 * @return true if the implemented plugin allows asynchronous execution in a separate thread
	 */
	public boolean allowAsynchronousExecution()
	{
		return false;
	}
}
