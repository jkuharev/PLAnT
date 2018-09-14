/** PLAnT, de.mz.jk.ms.plant.out, 14.03.2014*/
package de.mz.jk.ms.plant.out;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.log4j.Logger;

import de.mz.jk.jsix.math.interpolation.Interpolator;
import de.mz.jk.jsix.plot.pt.XYPlotter;
import de.mz.jk.ms.align.com.IMSPeak;

/**
 * reference vs source time 2d line plot by 1000 points
 * between first and last observed source time
 * <h3>{@link SimplePlotOutput}</h3>
 * @author kuharev
 * @version 14.03.2014 11:25:24
 */
public class SimplePlotOutput extends AlignmentOutput
{
	private static Logger log = Logger.getLogger( SimplePlotOutput.class );

	private int nPoints = 1000;

	String plotTitle = "retention time alignment";

	@Override public void doOutputAction()
	{

		List<IMSPeak> srcPeaks = run.getSrcPeaks();
		float minRT = srcPeaks.get( 0 ).rt;
		float maxRT = srcPeaks.get( srcPeaks.size() - 1 ).rt;
		float rtStep = ( maxRT - minRT ) / nPoints;
		final List<Double> x = new ArrayList<Double>( nPoints );
		for ( double rt = minRT; rt <= maxRT; rt += rtStep )
			x.add( rt );
		
		// show plot window in a separate thread to unblock main thread
		new Thread(){
			@Override public void run()
			{
				List<Double> y = Interpolator.getAverageY( run.getAlignmentResults(), x );
				XYPlotter p = new XYPlotter( 800, 600 );
				p.setPlotTitle( plotTitle );
				p.setPointStyle( XYPlotter.PointStyle.points );
				p.setXAxisLabel( "src: " + run.getSourceFile().getName() );
				p.setYAxisLabel( "ref: " + run.getReferenceFile().getName() );
				p.plotXY( x, y, null, true );
			}
		}.start();
	}


	@Override protected Option createCliOption()
	{
		return OptionBuilder
				.withLongOpt( "plot" )
				.withDescription( "plot calculated alignment" )
				.isRequired( false ).create( "p" );
	}

	@Override public boolean allowAsynchronousExecution()
	{
		return true;
	}
}
