/** PLAnT, de.mz.jk.ms.plant, 13.03.2014*/
package de.mz.jk.ms.plant;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;

import de.mz.jk.jsix.math.interpolation.Interpolator;
import de.mz.jk.jsix.utilities.Bencher;
import de.mz.jk.jsix.utilities.CSVUtils;
import de.mz.jk.ms.align.com.IMSPeak;
import de.mz.jk.ms.peak.reader.PeakListReader;
import de.mz.jk.ms.plant.out.AlignmentOutput;

/**
 * <h3>{@link AlignmentRun}</h3>
 * @author kuharev
 * @version 13.03.2014 14:15:25
 */
public class AlignmentRun implements Runnable
{
	private static Logger log = Logger.getLogger( AlignmentRun.class );

	private File srcFile = null;
	private File refFile = null;

	private List<IMSPeak> srcPeaks = null;
	private List<IMSPeak> refPeaks = null;

	private CSVUtils csv = new CSVUtils();

	private AlignmentParameters alignmentParameters = new AlignmentParameters();
	private PeakListAligner peakListAligner = null;
	private List<PeakListReader> peakReaders = null;
	private List<Interpolator> alignmentResults = null;
	private List<AlignmentOutput> alignmentOutputters = null;

	private PeakListReader lastUsedReader = null;
	private PeakListReader srcFileReader = null;
	private PeakListReader refFileReader = null;

	@Override public void run()
	{
		try
		{
			// read peaks
			log.info( "reading input files ..." );
			srcPeaks = readPeaks( srcFile );
			srcFileReader = lastUsedReader;
			refPeaks = readPeaks( refFile );
			refFileReader = lastUsedReader;

			// run alignment and collect result
			log.info( "aligning peaks ..." );
			peakListAligner = new PeakListAligner( srcPeaks, refPeaks, alignmentParameters );

			peakListAligner.addToRunTimeStatistics( ".", "src file", srcFile.getName() );
			peakListAligner.addToRunTimeStatistics( ".", "src file reader", srcFileReader.getFileTypeName() );
			peakListAligner.addToRunTimeStatistics( ".", "src peaks", srcPeaks.size() );
			peakListAligner.addToRunTimeStatistics( ".", "ref file", refFile.getName() );
			peakListAligner.addToRunTimeStatistics( ".", "ref file reader", refFileReader.getFileTypeName() );
			peakListAligner.addToRunTimeStatistics( ".", "ref peaks", refPeaks.size() );

			Bencher alignmentBencher = new Bencher().start();
			alignmentResults = peakListAligner.align();
			peakListAligner.addToRunTimeStatistics( ".", "duration", alignmentBencher.stop().getSec() );

			// run output
			log.info( "generating output ..." );
			for ( AlignmentOutput o : alignmentOutputters )
			{
				o.run( this );
			}

			log.info( "all done!" );
		} 
		catch (Exception e)
		{
			log.debug( "failed to process.", e );
		}
	}
	
	/**
	 * @return the srcPeaks
	 */
	public List<IMSPeak> getSrcPeaks()
	{
		return srcPeaks;
	}

	/**
	 * @return the refPeaks
	 */
	public List<IMSPeak> getRefPeaks()
	{
		return refPeaks;
	}

	/**
	 * @param file
	 * @return
	 */
	private List<IMSPeak> readPeaks(File file) throws Exception
	{
		for ( PeakListReader r : peakReaders )
		{
			if (r.isCompatible( file )) 
			{
				log.info( "reading " + r.getFileTypeName() + " input file: " + file + " ... " );
				Bencher t = new Bencher( true );
				List<IMSPeak> peaks = r.getPeaks( file );
				log.info( "read " + peaks.size() + " peaks in " + t.stop().getSecString() + "." );
				lastUsedReader = r;
				return peaks;
			}
		}
		throw new Exception( "unsupported type of input file: " + file );
	}

	/**
	 * @return the peakListAligner
	 */
	public PeakListAligner getPeakListAligner()
	{
		return peakListAligner;
	}

	/**
	 * @return the alignmentResults
	 */
	public List<Interpolator> getAlignmentResults()
	{
		return alignmentResults;
	}

	/**
	 * @param alignmentParameters the alignmentParameters to set
	 * @return 
	 */
	public AlignmentRun setAlignmentParameters(AlignmentParameters alignmentParameters)
	{
		this.alignmentParameters = alignmentParameters;
		return this;
	}

	/**
	 * @return the alignmentParameters
	 */
	public AlignmentParameters getAlignmentParameters()
	{
		return alignmentParameters;
	}

	public AlignmentRun setPeakReaders(List<PeakListReader> readers)
	{
		this.peakReaders = readers;
		return this;
	}

	public File getSourceFile()
	{
		return srcFile;
	}

	public AlignmentRun setSourceFile(File srcFile) throws Exception
	{
		if (!srcFile.canRead()) throw new Exception( "can not read the source file " + srcFile + "!" );
		this.srcFile = srcFile;
		return this;
	}

	public File getReferenceFile()
	{
		return refFile;
	}

	public AlignmentRun setReferenceFile(File refFile) throws Exception
	{
		if (!refFile.canRead()) throw new Exception( "can not read the reference file " + refFile + "!" );
		this.refFile = refFile;
		return this;
	}

	public CSVUtils getCSV()
	{
		return csv;
	}

	public AlignmentRun setCSV(CSVUtils csv)
	{
		this.csv = csv;
		return this;
	}

	public List<AlignmentOutput> getAlignmentOutputters()
	{
		return alignmentOutputters;
	}

	public AlignmentRun setAlignmentOutputters(List<AlignmentOutput> alignmentOutputters)
	{
		this.alignmentOutputters = alignmentOutputters;
		return this;
	}
}
