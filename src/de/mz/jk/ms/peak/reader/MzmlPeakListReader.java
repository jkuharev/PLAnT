/** PeakListTimeWarpingTool, de.mz.jk.ms.peak.reader, 19.02.2014*/
package de.mz.jk.ms.peak.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import uk.ac.ebi.jmzml.model.mzml.*;
import uk.ac.ebi.jmzml.xml.io.MzMLObjectIterator;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshaller;
import de.mz.jk.jsix.utilities.Bencher;
import de.mz.jk.ms.align.com.IMSPeak;
import de.mz.jk.ms.align.com.IMSPeakUtils;

/**
 * <h3>{@link MzmlPeakListReader}</h3>
 * @author kuharev
 * @version 19.02.2014 13:51:20
 */
public class MzmlPeakListReader extends PeakListReader
{
	private static Logger log = Logger.getLogger( MzmlPeakListReader.class );

	/*
		<spectrum index="29" id="index=45265" defaultArrayLength="52">
		  <cvParam cvRef="MS" accession="MS:1000511" name="ms level" value="2"/>
		  <cvParam cvRef="MS" accession="MS:1000580" name="MSn spectrum" value=""/>
		  <cvParam cvRef="MS" accession="MS:1000128" name="profile spectrum" value=""/>
		  <cvParam cvRef="MS" accession="MS:1000528" name="lowest observed m/z" value="86.0821"/>
		  <cvParam cvRef="MS" accession="MS:1000527" name="highest observed m/z" value="908.3557"/>
		  <cvParam cvRef="MS" accession="MS:1000285" name="total ion current" value="-1.0"/>
		  <cvParam cvRef="MS" accession="MS:1000796" name="spectrum title" value="T140210_04.45265.45265.3 File:&quot;&quot;, NativeID:&quot;index=45265&quot;"/>
		  <scanList count="1">
		    <cvParam cvRef="MS" accession="MS:1000795" name="no combination" value=""/>
		    <scan instrumentConfigurationRef="IC1">
		      <cvParam cvRef="MS" accession="MS:1000498" name="full scan" value=""/>
		      <cvParam cvRef="MS" accession="MS:1000016" name="scan start time" value="678.0" unitCvRef="UO" unitAccession="UO:0000010" unitName="second"/>
		    </scan>
		  </scanList>
		  <precursorList count="1">
		    <precursor>
		      <selectedIonList count="1">
		        <selectedIon>
		          <cvParam cvRef="MS" accession="MS:1000041" name="charge state" value="3"/>
		          <cvParam cvRef="MS" accession="MS:1000744" name="selected ion m/z" value="633.9232" unitCvRef="MS" unitAccession="MS:1000040" unitName="m/z"/>
		        </selectedIon>
		      </selectedIonList>
		      <activation>
		        <cvParam cvRef="MS" accession="MS:1000133" name="collision-induced dissociation" value=""/>
		      </activation>
		    </precursor>
		  </precursorList>
		  ...
		</spectrum> 
	*/
	public List<IMSPeak> getPeaks(File mzmlFile) throws Exception
	{
		Bencher t = new Bencher( true );
		MzMLUnmarshaller unmarshaller = new MzMLUnmarshaller( mzmlFile );
		MzMLObjectIterator<Spectrum> spectrumIterator = unmarshaller.unmarshalCollectionFromXpath( "/run/spectrumList/spectrum", Spectrum.class );
		List<IMSPeak> peaks = new ArrayList<IMSPeak>(  );
		while (spectrumIterator.hasNext())
		{
			Spectrum spectrum = spectrumIterator.next();
			try
			{
				float rt = getRtFromSpectrum( spectrum );
				List<Precursor> precursors = spectrum.getPrecursorList().getPrecursor();
				for ( Precursor precursor : precursors )
				{
					for ( ParamGroup ion : precursor.getSelectedIonList().getSelectedIon() )
					{
						IMSPeak p = new IMSPeak();
						p.rt = rt;
						for ( CVParam par : ion.getCvParam() )
						{
							try{
								String parId = par.getAccession();
								// MS:1000041 charge state
								if (parId.equals( "MS:1000041" ))
								{
										p.charge = Float.parseFloat( par.getValue() );
								}
								// MS:1000744 selected ion m/z
								else if (parId.equals( "MS:1000744" ))
								{
										p.mz = Float.parseFloat( par.getValue() );
								}
								// MS:1000042 peak intensity
								else if (parId.equals( "MS:1000042" ))
								{
										p.intensity = Float.parseFloat( par.getValue() );
								}
							} catch (Exception e) {}
						}
						// store peaks having at least m/z and charge info
						if (p.mz > 0 && p.charge > 0)
						{
							p.mass = IMSPeakUtils.getPeakMass( p.mz, p.charge );
							peaks.add( p );
						}
						else
						{
							log.debug( "parent ion information for spectrum " + spectrum.getId() + " not found!" );
						}
					}
				}
			}
			catch (Exception e)
			{
				log.debug( e.getMessage(), e );
			}
		}
		return peaks;
	}

	private float getRtFromSpectrum(Spectrum spectrum) throws Exception
	{
		ScanList scanList = spectrum.getScanList();
		if (scanList.getCount().intValue() > 0)
		{
			List<Scan> scans = scanList.getScan();
			if (scans.size() > 1)
				log.info( "spectrum " + spectrum.getId() + " has " + scans.size() + " scans!" );

			for ( CVParam par : scans.get( 0 ).getCvParam() )
			{
				if (par.getAccession().equals( "MS:1000016" ))
				{
					if (par.getUnitAccession().equals( "UO:0000031" ) || par.getUnitName().contains( "minute" ))
						return Float.parseFloat( par.getValue() ) * 60.0f;
					else
						return Float.parseFloat( par.getValue() );
				}
			}
		}
		throw new Exception( "scan start time for spectrum " + spectrum.getId() + " not found." );
	}

	@Override public String[] getTypicalFileExtensions()
	{
		return new String[] { "mzml" };
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
				if (line.toLowerCase().contains( "<mzml" ))
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
		return "mzML";
	}
}
