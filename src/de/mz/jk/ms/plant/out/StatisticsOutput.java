/** PLAnT, de.mz.jk.ms.plant.out, Jun 17, 2016*/
package de.mz.jk.ms.plant.out;

import java.io.PrintStream;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.log4j.Logger;

import de.mz.jk.jsix.utilities.CSVUtils;

/**
 * <h3>{@link StatisticsOutput}</h3>
 * @author jkuharev
 * @version Jun 17, 2016 12:59:54 PM
 */
public class StatisticsOutput extends AlignmentOutput
{
	private static Logger log = Logger.getLogger( StatisticsOutput.class );

	@Override protected void doOutputAction() throws Exception
	{
		PrintStream out = null;
		try
		{
			log.info( "writing output to file: " + outFile );
			out = new PrintStream( outFile );
			CSVUtils csv = run.getCSV();
			csv.printTxtCell( out, "category" ).printColSep( out ).printTxtCell( out, "key" ).printColSep( out ).printTxtCell( out, "value" ).endLine( out );
			Map<String, Map<String, String>> stats = getAlignmentRun().getPeakListAligner().getRunTimeStatistics();
			Set<String> cats = stats.keySet();
			for ( String cat : cats )
			{
				Map<String, String> _stats = stats.get( cat );
				Set<String> labs = _stats.keySet();
				for ( String lab : labs )
				{
					csv.printTxtCell( out, cat ).printColSep( out ).printTxtCell( out, lab ).printColSep( out ).printTxtCell( out, _stats.get( lab ) ).endLine( out );
				}
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

	@Override protected Option createCliOption()
	{
		return OptionBuilder
				.withArgName( "output file path" )
				// .withLongOpt( "statistics" )
				.withDescription( "runtime statistics" )
				.hasArg().create( "ost" );
	}
}
