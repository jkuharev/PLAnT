/** PeakListTimeWarpingTool, de.mz.jk.ms.peak.reader, 19.02.2014*/
package de.mz.jk.ms.peak.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

import org.jdom.Element;

import de.mz.jk.jsix.libs.XJDOM;
import de.mz.jk.ms.align.com.IMSPeak;
import de.mz.jk.ms.align.com.IMSPeakUtils;

/**
 * <h3>{@link MassSpectrumXmlPeakListReader}</h3>
 * @author kuharev
 * @version 19.02.2014 13:53:16
 */
public class MassSpectrumXmlPeakListReader extends PeakListReader
{
	@Override public List<IMSPeak> getPeaks(File massSpectrumXmlFile) throws Exception
	{
		openFile( massSpectrumXmlFile );
		List<IMSPeak> res = new ArrayList<IMSPeak>();
		while (next())
		{
			res.add( getIMSPeak() );
		}
		return res;
	}

	private File msFile = null;
	private int peakCounter = 0;
	private BufferedReader reader = null;
	private IMSPeak lastPeak = null;
	private Map<String, Integer> cols = null;
	private InstrumentMode instrumentMode = InstrumentMode.mse;

	/** we know about some WTF-differences in MSE (Electrospray-Shotgun) and DDA (MSMS) modes */
	public static enum InstrumentMode
	{
		mse, dda, unknown;
		public static InstrumentMode guessFromString(String modeString)
		{
			modeString = modeString.toLowerCase();
			return ( modeString.contains( "shotgun" ) ) ? mse : modeString.contains( "msms" ) ? dda : unknown;
		}
	}

	/**
	 * open a mass spectrum file
	 * @param msFile file to open
	 * @throws Exception
	 */
	public void openFile(File msFile) throws Exception
	{
		this.msFile = msFile;
		peakCounter = 0;
		reader = new BufferedReader( new FileReader( msFile ) );
		// skip lines until <DATA>-Tag
		String line = "";
		String xml = "";
		while (null != ( line = reader.readLine() ))
		{
			if (line.contains( "<DATA " ))
				break; // return;
			else xml += line + "\n";
		}
		readXMLData( xml );
	}

	/**
	 * @param xml
	 * @throws Exception
	 */
	private void readXMLData(String xml) throws Exception
	{
		Element doc = XJDOM.getBadJDOMRootElement( xml );
		try
		{
			Element pp = XJDOM.getChildren( doc, "PROCESSING_PARAMETERS", false ).get( 0 );
			instrumentMode = InstrumentMode.guessFromString( XJDOM.getAttributeValue( pp, "INSTRUMENT_MODE" ) );
		}
		catch (Exception e)
		{}
		List<Element> formats = XJDOM.getChildren( doc, "FORMAT" );
		for ( Element format : formats )
		{
			if (XJDOM.getAttributeValue( format, "FRAGMENTATION_LEVEL" ).trim().equals( "0" ))
			{
				cols = getColumnIndexHashMap( format );
			}
		}
	}

	/**
	 * @param format
	 * @return
	 */
	private Map<String, Integer> getColumnIndexHashMap(Element format)
	{
		HashMap<String, Integer> res = new HashMap<String, Integer>();
		List<Element> fields = XJDOM.getChildren( format, "FIELD" );
		for ( Element field : fields )
		{
			try
			{
				res.put(
						XJDOM.getAttributeValue( field, "NAME" ),
						Integer.parseInt( XJDOM.getAttributeValue( field, "POSITION" ) ) - 1
						);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return res;
	}

	/**
	 * @return opened File 
	 */
	public File getMassSpectrumFile()
	{
		return msFile;
	}

	/**
	 * checks if next mass peak is valid and reads it<br>
	 * usage:<pre>
	 * while(reader.next())
	 * {
	 * 		IMSPeak mp = reader.getIMSPeak();
	 * 		...
	 * }
	 * </pre>
	 * @return true if next mass peak is valid, false if not
	 */
	public boolean next()
	{
		lastPeak = readNextPeak();
		return lastPeak != null;
	}

	/**
	 * @return last read mass peak
	 */
	public IMSPeak getIMSPeak()
	{
		return lastPeak;
	}

	/**
	 * @return how many valid peaks already read
	 */
	public int countValidPeaks()
	{
		return peakCounter;
	}

	private IMSPeak readNextPeak()
	{
		String line = "";
		String[] row = null;
		IMSPeak p = null;
		try
		{
			if (null == ( line = reader.readLine() ) || line.contains( "</DATA" ))
			{
				reader.close();
			}
			else
			{
				row = line.trim().split( "\\s+" );
				p = new IMSPeak();
				p.peak_id = Long.parseLong( getCol( row, "LE_ID" ) );
				p.intensity = Float.parseFloat( getCol( row, "Intensity" ) );
				p.drift = Float.parseFloat( getCol( row, "Mobility" ) );
				p.rt = Float.parseFloat( getCol( row, "RT" ) ) * 60.0f; // time in seconds
				p.charge = Float.valueOf( cols.containsKey( "Charge" ) ? getCol( row, "Charge" ) : getCol( row, "AverageCharge" ) );
				// Workaround for a very intelligent idea from Waters developers
				// to write into Mass fields not the mass but other peak properties,
				// depending on acquisition method the field Mass contains
				// a) mass + H+ for DIA data
				// b) MZ for DDA data
				switch(instrumentMode)
				{
					case dda:
						p.mz = Float.parseFloat( getCol( row, "Mass" ) );
						p.mass = IMSPeakUtils.getPeakMass( p.mz, p.charge );
						break;
					case mse:
					default:
						p.mass = Float.parseFloat( getCol( row, "Mass" ) ) - IMSPeakUtils.Hplus;
						p.mz = IMSPeakUtils.getPeakMZ( p.mass, p.charge );
				}
				peakCounter++;
				return p;
			}
		}
		catch (Exception e)
		{
			System.err.println( "processing MassSpectrum.xml file failed!" );
			System.err.println( "file: '" + msFile.getAbsolutePath() + "'" );
			System.err.println( "current row content: '" + line + "'" );
			System.err.print( "row splitting: " );
			for ( int i = 0; i < row.length; i++ )
				System.err.print( i + ": " + row[i] + "; " );
			System.err.println();
			System.err.print( "precalculated column indexes: " );
			Set<String> keys = cols.keySet();
			for ( String key : keys )
				System.err.print( cols.get( key ) + "='" + key + "'; " );
			System.err.println();
			e.printStackTrace();
		}
		return null;
	}

	private String getCol(String[] row, String colName)
	{
		return ( cols.containsKey( colName ) ) ? row[cols.get( colName )] : "0";
	}

	public InstrumentMode getInstrumentMode()
	{
		return instrumentMode;
	}

	@Override public String[] getTypicalFileExtensions()
	{
		return new String[] { "xml" };
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
				if (line.toLowerCase().contains( "<mass_spectrum" ))
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
		return "Waters MassSpectrum XML";
	}
}
