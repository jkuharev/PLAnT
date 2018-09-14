/** PLAnT, de.mz.jk.ms.peak.reader, 07.03.2014*/
package de.mz.jk.ms.peak.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

import uk.ac.ebi.pride.tools.mgf_parser.MgfFile;
import de.mz.jk.ms.align.com.IMSPeak;

/**
 * <h3>{@link MgfPeakListReader}</h3>
 * @author kuharev
 * @version 07.03.2014 10:46:33
 */
public class MgfPeakListReader extends PeakListReader
{
	@Override public List<IMSPeak> getPeaks(File file) throws Exception
	{
		return PeakListReader.getPeaks( new MgfFile( file ) );
	}

	@Override public boolean isCompatible(File inputFile)
	{
		// check if input file is probably an MGF file
		// by finding BEGIN IONS in first 256 lines
		BufferedReader r = null;
		try
		{
			r = new BufferedReader( new FileReader( inputFile ) );
			String line = "";
			for ( int i = 0; i < 256 && null != ( line = r.readLine() ); i++ )
			{
				line = line.trim().toLowerCase();
				if (line.length() < 1) continue;
				if (line.equals( "begin ions" ))
				{
					r.close();
					return true;
				}
			}
		}
		catch (Exception e){}
		finally{ try{r.close();}catch (Exception ex){} }
		return false;
	}

	@Override public String[] getTypicalFileExtensions()
	{
		return new String[] { "mgf" };
	}

	@Override public String getFileTypeName()
	{
		return "Mascot Generic Format";
	}
}
