/** PeakListTimeWarpingTool, de.mz.jk.ms.peak.reader, 19.02.2014*/
package de.mz.jk.ms.peak.reader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import uk.ac.ebi.pride.tools.jmzreader.JMzReader;
import uk.ac.ebi.pride.tools.jmzreader.model.Spectrum;
import uk.ac.ebi.pride.tools.jmzreader.model.impl.CvParam;
import de.mz.jk.ms.align.com.IMSPeak;
import de.mz.jk.ms.align.com.IMSPeakComparator;
import de.mz.jk.ms.align.com.IMSPeakUtils;

/**
 * peak list reader interface
 * <h3>{@link PeakListReader}</h3>
 * @author kuharev
 * @version 19.02.2014 13:42:52
 */
public abstract class PeakListReader
{
	private static Logger log = Logger.getLogger( PeakListReader.class );

	/** 
	 * get peaks from a file
	 * @param file
	 * @return list of peaks
	 * @throws Exception
	 */
	public abstract List<IMSPeak> getPeaks(File file) throws Exception;

	/**
	 * get peaks from a file
	 * @param filePath
	 * @return list of peaks
	 * @throws Exception
	 */
	public List<IMSPeak> getPeaks(String filePath) throws Exception
	{
		return getPeaks( new File( filePath ) );
	}
	
	/**
	 * get peaks in ascending order by rt
	 * @param filePath
	 * @param orderByRT
	 * @return list of peaks
	 * @throws Exception
	 */
	public List<IMSPeak> getPeaks(String filePath, boolean orderByRT) throws Exception
	{
		List<IMSPeak> peaks = getPeaks( new File( filePath ) );
		if (orderByRT) Collections.sort( peaks, new IMSPeakComparator.byRtAsc() );
		return peaks;
	}

	/** @return typical file extension of input file */
	public abstract String[] getTypicalFileExtensions();

	/** @return the name of file format to read */
	public abstract String getFileTypeName();

	/** test if the reader can handle given file 
	 * @param inputFile
	 * @return true if input file is compatible with the reader
	 */
	public abstract boolean isCompatible(File inputFile);

	/**
	 * test if the reader can handle given file 
	 * @param inputFilePath
	 * @return
	 */
	public boolean isCompatible(String inputFilePath)
	{
		return isCompatible( new File( inputFilePath ) );
	}

	/**
	 * read peaks using JMzReader interface
	 * @param inputParser a JMzReader interface implementation 
	 * @return list of IMSPeaks
	 * @throws Exception
	 */
	public static List<IMSPeak> getPeaks(JMzReader inputParser) throws Exception
	{
		List<IMSPeak> peaks = new ArrayList<IMSPeak>( inputParser.getSpectraCount() );
		Iterator<Spectrum> spi = inputParser.getSpectrumIterator();
		while (spi.hasNext())
		{
			try
			{
				Spectrum s = spi.next();
				IMSPeak peak = new IMSPeak();
				try{peak.peak_id = Long.parseLong( s.getId() );}catch (Exception e){}
				peak.mz = s.getPrecursorMZ().floatValue();
				peak.charge = s.getPrecursorCharge();
				peak.mass = IMSPeakUtils.getPeakMass( peak.mz, peak.charge );
				try{peak.intensity = s.getPrecursorIntensity().floatValue();}catch (Exception e){}
				for ( CvParam par : s.getAdditional().getCvParams() )
				{
					// MS:1000894 retention time 
					// A measure of the interval relative to the beginning of a mass spectrometric run
					// when a peptide will exit the chromatographic column.
					if (par.getAccession().equals( "MS:1000894" ))
					{
						String val = par.getValue();
						float rt = Float.parseFloat( val.replaceAll( "[A-Z]", "" ) );
						// ensure retention time in seconds
						peak.rt = ( val.endsWith( "H" ) ) ? rt * 3600f : ( val.endsWith( "M" ) ) ? rt * 60f : rt;
					}
					// MS:1001966	product ion mobility	
					// The mobility of an MS2 product ion, as measured by ion mobility mass spectrometry.	
					// MS:1001967	product ion drift time
					// The ion drift time of an MS2 product ion.
					if (par.getAccession().equals( "MS:1001966" ))
					{
						peak.drift = Float.parseFloat( par.getValue() );
					}
				}
				peaks.add( peak );
			}
			catch (Exception e)
			{
				log.debug( e.getMessage(), e );
			}
		}
		return peaks;
	}
}
