/** PLAnT, de.mz.jk.ms.plant.out, 13.03.2014*/
package de.mz.jk.ms.plant.out;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.log4j.Logger;

import de.mz.jk.jsix.math.interpolation.Interpolator;
import de.mz.jk.ms.align.com.IMSPeak;

/**
 * interpolated source to reference time mapping as a look-up table
 * for every second between first and last observed source time
 * <h3>{@link InterpolatedLookUpMappingOutput}</h3>
 * @author kuharev
 * @version 13.03.2014 17:29:48
 */
public class InterpolatedLookUpMappingOutput extends AlignmentOutput
{
	private static Logger log = Logger.getLogger( InterpolatedLookUpMappingOutput.class );

	private final float rtStep = 1f;

	@Override public void doOutputAction()
	{
		// get source rt range
		List<IMSPeak> srcPeaks = run.getSrcPeaks();
		float minRT = (int)srcPeaks.get( 0 ).rt;
		float maxRT = srcPeaks.get( srcPeaks.size() - 1 ).rt;

		// calculate number of steps
		int nPoints = (int)( ( maxRT - minRT ) / rtStep + 1 );

		// generate source rt in the source range using the calculated step 
		List<Double> x = new ArrayList<Double>( nPoints );
		for ( double rt = minRT; rt <= maxRT; rt += rtStep ) x.add( rt );
		
		// get interpolated reference rt
		List<Double> y = Interpolator.getAverageY( run.getAlignmentResults(), x );
		
		// output to csv
		writePeaksToCSV( x, y, outFile );
	}

	@Override protected Option createCliOption()
	{
		return OptionBuilder
				.withArgName( "output file path" )
				// .withLongOpt( "output-interpolated-mapping" )
				.withDescription( "source to reference look up table" )
				.hasArg().create( "o" );
	}
}
