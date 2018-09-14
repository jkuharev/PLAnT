/** PeakListTimeWarpingTool, de.mz.jk.ms.peak.reader, 19.02.2014*/
package de.mz.jk.ms.peak.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.ac.ebi.pride.tools.mzxml_parser.MzXMLFile;
import uk.ac.ebi.pride.tools.mzxml_parser.MzXMLFile.MzXMLScanIterator;
import uk.ac.ebi.pride.tools.mzxml_parser.mzxml.model.PrecursorMz;
import uk.ac.ebi.pride.tools.mzxml_parser.mzxml.model.Scan;
import de.mz.jk.jsix.utilities.Bencher;
import de.mz.jk.ms.align.com.IMSPeak;
import de.mz.jk.ms.align.com.IMSPeakUtils;

/**
 * <h3>{@link MzXmlPeakListReader}</h3>
 * @author kuharev
 * @version 19.02.2014 13:51:20
 */
public class MzXmlPeakListReader extends PeakListReader
{
	@Override public List<IMSPeak> getPeaks(File xmlFile) throws Exception
	{
		return PeakListReader.getPeaks( new MzXMLFile( xmlFile ) );
	}
	
	public List<IMSPeak> getPeaks2(File xmlFile) throws Exception
	{
		Bencher t = new Bencher( true );
		MzXMLFile inputParser = new MzXMLFile( xmlFile );
		int scanCount = inputParser.getMS2ScanCount();
		List<IMSPeak> peaks = new ArrayList<IMSPeak>( scanCount );
		MzXMLScanIterator si = inputParser.getMS2ScanIterator();
		while (si.hasNext())
		{
			Scan scan = si.next();
			IMSPeak peak = new IMSPeak();
			peak.peak_id = scan.getNum();
			peak.rt = scan.getRetentionTime().getSeconds();
			PrecursorMz precursor = scan.getPrecursorMz().get( 0 );
			// mz = (mass + z * H+) / z
			// mass = (mz - H+) * z
			// with H+ = 1.0078
			peak.mz = precursor.getValue();
			peak.charge = precursor.getPrecursorCharge().floatValue();
			peak.mass = IMSPeakUtils.getPeakMass( peak.mz, peak.charge );
			Map<Double, Double> fragmentPeaks = inputParser.getSpectrumById( "" + peak.peak_id ).getPeakList();
			for ( Double fMZ : fragmentPeaks.keySet() )
			{
				peak.intensity += fragmentPeaks.get( fMZ );
			}
			peaks.add( peak );
		}
		// System.out.println( "[" + scanCount + "scans, " + t.stop().getSecString() + "]" );
		return peaks;
	}

	@Override public String[] getTypicalFileExtensions()
	{
		return new String[] { "mzxml" };
	}

	@Override public boolean isCompatible(File inputFile)
	{
		BufferedReader r = null;
		try
		{
			r = new BufferedReader( new FileReader( inputFile ) );
			String line = "";
			for ( int i = 0; i < 20 && null != ( line = r.readLine() ); i++ )
			{
				if (line.trim().length() < 1) continue;
				if (line.toLowerCase().contains( "<mzxml" ))
				{
					r.close();
					return true;
				}
			}
		}
		catch (Exception e)
		{}
		finally
		{
			try
			{
				r.close();
			}
			catch (Exception ex)
			{}
		}
		return false;
	}

	@Override public String getFileTypeName()
	{
		return "mzXML";
	}
}
