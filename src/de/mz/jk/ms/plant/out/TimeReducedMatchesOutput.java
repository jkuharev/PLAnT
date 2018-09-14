/** PLAnT, de.mz.jk.ms.plant.out, 13.03.2014*/
package de.mz.jk.ms.plant.out;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import de.mz.jk.jsix.math.interpolation.Interpolator;
import de.mz.jk.ms.align.com.IMSPeak;

/**
 * write csv file containing rt and ref_rt for every alignment match reduced to unique rt,
 * that means matches found at the same source rt are reduced to a single time!
 * <h3>{@link TimeReducedMatchesOutput}</h3>
 * @author kuharev
 * @version 13.03.2014 17:29:48
 */
public class TimeReducedMatchesOutput extends AlignmentOutput
{
	@Override public void doOutputAction()
	{
		List<IMSPeak> srcPeaks = run.getSrcPeaks();

		// store all source rt to a set to reduce duplicate values 
		Set<Double> uniX = new LinkedHashSet<Double>( srcPeaks.size() );
		for ( IMSPeak p : srcPeaks ) uniX.add( Double.valueOf( (double)p.rt ) );

		// get reference values for the reduced set of source time points
		List<Double> x = new ArrayList<Double>( uniX );
		List<Double> y = Interpolator.getAverageY( run.getAlignmentResults(), x );
		
		// output to csv
		writePeaksToCSV( x, y, outFile );
	}

	@Override protected Option createCliOption()
	{
		return OptionBuilder
				.withArgName( "output file path" )
				// .withLongOpt( "output-reduced-match-times" )
				.withDescription( "alignment matches reduced to unique source times" )
				.hasArg().create( "orm" );
	}
}
