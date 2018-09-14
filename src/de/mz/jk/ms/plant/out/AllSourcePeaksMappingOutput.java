/** PLAnT, de.mz.jk.ms.plant.out, 13.03.2014*/
package de.mz.jk.ms.plant.out;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import de.mz.jk.jsix.math.interpolation.Interpolator;
import de.mz.jk.ms.align.com.IMSPeak;

/**
 * write csv file containing rt and ref_rt for every peak in srcPeaks
 * <h3>{@link AllSourcePeaksMappingOutput}</h3>
 * @author kuharev
 * @version 13.03.2014 17:29:48
 */
public class AllSourcePeaksMappingOutput extends AlignmentOutput
{
	@Override public void doOutputAction()
	{
		// get rt of all source peaks
		List<IMSPeak> srcPeaks = run.getSrcPeaks();
		List<Double> x = new ArrayList<Double>( srcPeaks.size() );
		for ( IMSPeak p : srcPeaks ) x.add( Double.valueOf( (double)p.rt ) );
		
		// get interpolated reference rt 
		List<Double> y = Interpolator.getAverageY( run.getAlignmentResults(), x );
		
		// output to csv
		writePeaksToCSV( x, y, outFile );
	}

	@Override protected Option createCliOption()
	{
		return OptionBuilder
				.withArgName( "output file path" )
				// .withLongOpt( "output-every-source-peak-mapping" )
				.withDescription( "source to reference mapping for each source peak" )
				.hasArg().create( "oall" );
	}
}
