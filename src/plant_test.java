import java.util.LinkedHashMap;
import java.util.Map;

/** PLAnT, , Jun 17, 2016*/
/**
 * <h3>{@link plant_test}</h3>
 * @author jkuharev
 * @version Jun 17, 2016 2:34:17 PM
 */
public class plant_test
{
	String dir = "/Volumes/DAT/2013-04 Human Yeast Ecoli 1P FDR/";
	String prj = dir + "root/Proj__13966189271230_9093339492956815/";
	String xml = "/MassSpectrum.xml";
	static Map<String, String> files = new LinkedHashMap<String, String>();

	static
	{
		files.put( "mse90a3", "_13966196262550_07997381742660425" );
		files.put( "mse90b3", "_13966196262700_3327164753551458" );
		files.put( "udmse90a3", "_13966195933390_08010164768094308" );
		files.put( "udmse90b3", "_13966195933540_04240616369953687" );
		files.put( "mse200a3", "_13966195272570_8243785024181673" );
		files.put( "mse200b3", "_13966195272880_3925221604965876" );
		files.put( "udmse200a3", "_13966194929990_4592695821966547" );
		files.put( "udmse200b3", "_13966194930310_45741855126229025" );
		files.put( "iniMSEfast", "plant.mse.fast.ini" );
		files.put( "iniMSEfull", "plant.mse.full.ini" );
	}

	private String f(String f)
	{
		return files.containsKey( f ) ? files.get( f ) : f;
	}

	private String _if(String run)
	{
		return '"' + prj + f( run ) + xml + '"';
	}

	private String _of(String file)
	{
		return '"' + dir + f( file ) + '"';
	}

	public plant_test(String src, String ref, String ini, String lab)
	{
		String[] args = new String[] {
				"-s", _if( src ), "-r", _if( ref ),
				"-orm", _of(src+"_to_"+ref+"."+lab+".matches.csv"), 
				"-ost", _of( src + "_to_" + ref + "." + lab + ".stat.csv" ),
				"-i", _of( ini )
		};
		plant.main( args );
	}

	public static void main(String[] args)
	{
		new plant_test( "mse90a3", "mse90b3", "iniMSEfast", "fast" );
		new plant_test( "mse90a3", "mse90b3", "iniMSEfull", "full" );
	}
}
