package de.mz.jk.ms.plant;

import java.util.*;

import org.apache.log4j.Logger;

import de.mz.jk.jsix.math.interpolation.Interpolator;
import de.mz.jk.jsix.utilities.Bencher;
import de.mz.jk.ms.align.com.DTWPathDescription;
import de.mz.jk.ms.align.com.IMSPeak;
import de.mz.jk.ms.align.com.IMSPeakComparator;
import de.mz.jk.ms.align.com.abstraction.PeakListAbstractor;
import de.mz.jk.ms.align.com.abstraction.PeakListAbstractorInRTWindowsByHighestMass;
import de.mz.jk.ms.align.method.dtw.RTW;
import de.mz.jk.ms.align.method.dtw.linear.ParallelFastLinearRTW;
import de.mz.jk.ms.align.method.dtw.linear.ParallelLinearRTW;

/** PeakListTimeWarpingTool, , 17.02.2014*/
/**
 * <h3>{@link PeakListAligner}</h3>
 * @author kuharev
 * @version 17.02.2014 16:01:44
 */
public class PeakListAligner
{
	private static Logger log = Logger.getLogger( PeakListAligner.class );
	
	private AlignmentParameters parameters = new AlignmentParameters();
	private Map<String, Map<String, String>> statistics = new LinkedHashMap<String, Map<String, String>>();
	
	// peak lists
	private List<IMSPeak> srcPeaks = null;
	private List<IMSPeak> refPeaks = null;

	// peak abstractors
	private PeakListAbstractor srcAbstractor = null;
	private PeakListAbstractor refAbstractor = null;

	/**
	 * @param src
	 * @param ref
	 * @param params
	 */
	public PeakListAligner(List<IMSPeak> src, List<IMSPeak> ref, AlignmentParameters params)
	{
		setParameters( params );
		setSourcePeaks( src );
		setReferencePeaks( ref );
	}

	/** @return the statistics */
	public Map<String, Map<String, String>> getRunTimeStatistics()
	{
		return statistics;
	}
	
	/** @return the statistics map for a category */
	public Map<String, String> getRunTimeStatistics(String category)
	{
		if (!statistics.containsKey( category ))
			statistics.put( category, new LinkedHashMap<String, String>() );
		return statistics.get( category );
	}

	/** @return the parameters */
	public AlignmentParameters getParameters(){return parameters;}
	
	/** @param parameters the parameters to set */
	public void setParameters(AlignmentParameters parameters){this.parameters = parameters;}

	/** 
	 * set a Category-Key-Value entity to the run time statistics map. 
	 * Use category="." for global statistics
	 * */
	public void addToRunTimeStatistics(String category, String key, Object value)
	{
		Map<String, String> map = getRunTimeStatistics( category );
		map.put( key, value == null ? "" : value.toString() );
	}

	public List<Interpolator> align() throws Exception
	{
		getRunTimeStatistics( "parameters" ).putAll( parameters.dump() );
		switch (parameters.getPathMode())
		{
			case LEFT:
				return alignSinglePass( DTWPathDescription.LEFT );
			case RIGHT:
				return alignSinglePass( DTWPathDescription.RIGHT );
			case BOTH:
			default:
				return alignTwoPass();
		}
	}

	/** 
	 * subsequently align using LEFT and RIGHT outermost DTW paths
	 * @throws Exception */
	private List<Interpolator> alignTwoPass() throws Exception
	{
		List<Interpolator> interpolatedResultMapping = new ArrayList<Interpolator>( 2 );
		interpolatedResultMapping.add( align( DTWPathDescription.LEFT ).getInterpolator( true ) );
		interpolatedResultMapping.add( align( DTWPathDescription.RIGHT ).getInterpolator( true ) );
		return interpolatedResultMapping;
	}
	
	/** align using user defined DTW path mode 
	 * @throws Exception */
	private List<Interpolator> alignSinglePass(DTWPathDescription dtwPathMode) throws Exception
	{
		return Collections.singletonList( align( dtwPathMode ).getInterpolator( true ) );
	}
	
	private RTW align(DTWPathDescription dtwPathMode) throws Exception
	{
		RTW rtw = null;
		Bencher alignmentBencher = new Bencher( true );
		addToRunTimeStatistics( dtwPathMode.toString(), "src peaks", "" + srcPeaks.size() );
		addToRunTimeStatistics( dtwPathMode.toString(), "ref peaks", "" + refPeaks.size() );

		// full alignment
		if (parameters.getForceFullAlignment() || Math.max( srcPeaks.size(), refPeaks.size() ) < parameters.getMinPeaksForIterativeAlignment())
		{
			log.info( "running full " + dtwPathMode + " alignment ..." );
			addToRunTimeStatistics( dtwPathMode.toString(), "mode", "full" );
			rtw = alignFull( srcPeaks, refPeaks, dtwPathMode );
		}
		// iterative alignment
		else
		{
			log.info( "running iterative " + dtwPathMode + " alignment ..." );
			addToRunTimeStatistics( dtwPathMode.toString(), "mode", "fast" );
			// run iterative path refining alignment
			double corFac = Math.abs( parameters.getCorridorRadius() );
			boolean dynamicRadius = corFac < .5 && corFac > 0;
			int radius = (int)( corFac >= 1 ? corFac : 1 );
			List<Interpolator> prealignmentResult = null;
			
			// path refining prealignments
			int nMin = parameters.getMinPeaksForIterativeAlignment();
			int nMax = Math.max( refPeaks.size(), srcPeaks.size() );
			
			// double the number of aligned peaks for the path refinement until all peaks can be aligned
			for ( int n = nMin, prealignmentCount = 0; n < nMax; n *= 2, prealignmentCount++ )
			{
				if (dynamicRadius) radius = (int)( n * corFac );
				String prealignmentID = dtwPathMode + " #" + prealignmentCount;

				List<IMSPeak> refSubPeaks = refAbstractor.getSortedSubPeaks( n, new IMSPeakComparator.byRtAsc() );
				List<IMSPeak> keySubPeaks = srcAbstractor.getSortedSubPeaks( n, new IMSPeakComparator.byRtAsc() );

				int nSrcSubPeaks = keySubPeaks.size();
				int nRefSubPeaks = refSubPeaks.size();
				
				log.debug( "prealigning by " + nSrcSubPeaks + " x " + nRefSubPeaks + " peaks ..." );
				addToRunTimeStatistics( prealignmentID, "src peaks", nSrcSubPeaks );
				addToRunTimeStatistics( prealignmentID, "ref peaks", nRefSubPeaks );
				
				Bencher prealignmentBencher = new Bencher().start();
				if (n == nMin)
				{
					addToRunTimeStatistics( prealignmentID, "method", "full" );
					rtw = alignFull( keySubPeaks, refSubPeaks, dtwPathMode );
				}
				else
				{
					addToRunTimeStatistics( prealignmentID, "method", "fast" );
					addToRunTimeStatistics( prealignmentID, "radius", nRefSubPeaks );
					rtw = alignFast( keySubPeaks, refSubPeaks, prealignmentResult, dtwPathMode, radius );
				}
				prealignmentBencher.stop();

				Interpolator iterationResult = rtw.getInterpolator( true );
				prealignmentResult = Collections.singletonList( iterationResult );
				
				addToRunTimeStatistics( prealignmentID, "matches", iterationResult.getOriginalSize() );
				addToRunTimeStatistics( prealignmentID, "duration", prealignmentBencher.getSec() );
				
				log.debug( "prealignment duration: " + prealignmentBencher.getSecString() );
				log.debug( "prealignment resulted in " + iterationResult.getOriginalSize() + " matches ..." );
			}
			
			// align all available peaks
			rtw = alignFast( srcPeaks, refPeaks, prealignmentResult, dtwPathMode, radius );
		}

		int nMatches = rtw.getInterpolator( true ).getOriginalSize();
		double duration = alignmentBencher.stop().getSec();
		addToRunTimeStatistics( dtwPathMode.toString(), "matches", nMatches );
		addToRunTimeStatistics( dtwPathMode.toString(), "duration", duration );
		
		log.info( "alignment result: " + nMatches + " matches, " + duration + "s!" );
		return rtw;
	}

	/** this version will use predefined abstraction parts */
	private RTW align2(DTWPathDescription dtwPathMode) throws Exception
	{
		RTW rtw = null;
		Bencher t = new Bencher( true );
		// full alignment
		if (parameters.getForceFullAlignment() || Math.min( srcPeaks.size(), refPeaks.size() ) < parameters.getMinPeaksForIterativeAlignment())
		{
			log.info( "running full " + dtwPathMode + " alignment ..." );
			rtw = alignFull( srcPeaks, refPeaks, dtwPathMode );
		}
		// iterative alignment
		else
		{
			log.info( "running iterative " + dtwPathMode + " alignment ..." );
			// run iterative path refining alignment
			double corFac = Math.abs( parameters.getCorridorRadius() );
			boolean dynamicRadius = corFac < .5 && corFac > 0;
			int radius = (int)( corFac >= 1 ? corFac : 1 );
			List<Interpolator> prealignmentResult = null;
			
			// path refining prealignments
			double[] parts = parameters.getAbstractionParts();
			// increase the number of peaks to align by predefined parts
			for ( int i = 0; i < parts.length; i++ )
			{
				float part = (float)parts[i];
				List<IMSPeak> refSubPeaks = refAbstractor.getSortedSubPeaks( part, new IMSPeakComparator.byRtAsc() );
				List<IMSPeak> keySubPeaks = srcAbstractor.getSortedSubPeaks( part, new IMSPeakComparator.byRtAsc() );
				log.debug( "prealigning by " + keySubPeaks.size() + " x " + refSubPeaks.size() + " peaks ..." );
				
				if (dynamicRadius) 
					radius = (int)( part * Math.max( refSubPeaks.size() * corFac, keySubPeaks.size() * corFac ) );
				
				rtw = (i==0) 
						? alignFull( keySubPeaks, refSubPeaks, dtwPathMode )
						: alignFast( keySubPeaks, refSubPeaks, prealignmentResult, dtwPathMode, radius );
				
				Interpolator iterationResult = rtw.getInterpolator( true );
				prealignmentResult = Collections.singletonList( iterationResult );
				log.debug( "prealignment resulted in " + iterationResult.getOriginalSize() + " matches ..." );
			}
			// align all available peaks
			rtw = alignFast( srcPeaks, refPeaks, prealignmentResult, dtwPathMode, radius );
		}
		log.info( "alignment result: " + rtw.getInterpolator( true ).getOriginalSize() + " matches, " + t.stop().getSecString() + "!" );
		return rtw;
	}

	private RTW alignFull(List<IMSPeak> runPeaks, List<IMSPeak> refPeaks, DTWPathDescription pathMode)
	{
		ParallelLinearRTW rtw = new ParallelLinearRTW( runPeaks, refPeaks );
		rtw.setMaxDepth( parameters.getMaxProcessForkingDepth() );
		rtw.setMaxDeltaMassPPM( parameters.getMaxDeltaMassPPM() );
		rtw.setMaxDeltaDriftTime( parameters.getMaxDeltaDriftTime() );
		rtw.setPathMode( pathMode );
		rtw.run();
		return rtw;
	}

	private RTW alignFast(List<IMSPeak> runPeaks, List<IMSPeak> refPeaks, List<Interpolator> pre, DTWPathDescription pathMode, int radius)
	{
		ParallelFastLinearRTW rtw = new ParallelFastLinearRTW( runPeaks, refPeaks );
		rtw.setMaxDepth( parameters.getMaxProcessForkingDepth() );
		rtw.setMaxDeltaMassPPM( parameters.getMaxDeltaMassPPM() );
		rtw.setMaxDeltaDriftTime( parameters.getMaxDeltaDriftTime() );
		rtw.setPathMode( pathMode );
		rtw.setRadius( radius );
		for ( Interpolator f : pre )
		{
			rtw.addCorridorFunction( f );
		}
		rtw.run();
		return rtw;
	}

	/**
	 * @param refPeaks the refPeaks to set
	 */
	public PeakListAligner setReferencePeaks(List<IMSPeak> referencePeaks)
	{
		this.refPeaks = referencePeaks;
		refAbstractor = new PeakListAbstractorInRTWindowsByHighestMass( referencePeaks );
		return this;
	}

	/**
	 * @param srcPeaks the srcPeaks to set
	 * @return 
	 */
	public PeakListAligner setSourcePeaks(List<IMSPeak> mappablePeaks)
	{
		this.srcPeaks = mappablePeaks;
		srcAbstractor = new PeakListAbstractorInRTWindowsByHighestMass( mappablePeaks );
		return this;
	}
}
