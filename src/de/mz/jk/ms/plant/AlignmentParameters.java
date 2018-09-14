package de.mz.jk.ms.plant;

import java.util.LinkedHashMap;
import java.util.Map;

/** PeakListTimeWarpingTool, 18.02.2014 */
/**
 * <h3>{@link AlignmentParameters}</h3>
 * @author kuharev
 * @version 18.02.2014 13:57:28
 */
public class AlignmentParameters
{
	public static enum PathMode
	{
		LEFT, RIGHT, BOTH;
		public static PathMode fromString(String pathDesc)
		{
			String d = pathDesc.toLowerCase();
			if (d.contains( "right" )) return RIGHT;
			if (d.contains( "left" )) return LEFT;
			if (d.contains( "both" )) return BOTH;
			if (d.startsWith( "r" )) return RIGHT;
			if (d.startsWith( "l" )) return LEFT;
			return BOTH;
		}
	}

	private int maxProcessForkingDepth = 8;
	private double maxDeltaMassPPM = 10.0;
	private double maxDeltaDriftTime = 200;

	private double[] abstractionParts = { .0625, .125, .25, .5 };

	private int minPeaksForIterativeAlignment = 5000;
	private double corridorRadius = 300;
	private boolean forceFullAlignment = false;

	private PathMode pathMode = PathMode.BOTH;

	public int getMaxProcessForkingDepth()
	{
		return maxProcessForkingDepth;
	}

	public AlignmentParameters setMaxProcessForkingDepth(int maxProcessForkingDepth)
	{
		this.maxProcessForkingDepth = maxProcessForkingDepth;
		return this;
	}

	public double getMaxDeltaMassPPM()
	{
		return maxDeltaMassPPM;
	}

	public AlignmentParameters setMaxDeltaMassPPM(double maxDeltaMassPPM)
	{
		this.maxDeltaMassPPM = maxDeltaMassPPM;
		return this;
	}

	public double getMaxDeltaDriftTime()
	{
		return maxDeltaDriftTime;
	}

	public AlignmentParameters setMaxDeltaDriftTime(double maxDeltaDriftTime)
	{
		this.maxDeltaDriftTime = maxDeltaDriftTime;
		return this;
	}

	public double[] getAbstractionParts()
	{
		return abstractionParts;
	}

	public AlignmentParameters setAbstractionParts(double[] abstractionParts)
	{
		this.abstractionParts = abstractionParts;
		return this;
	}

	public int getMinPeaksForIterativeAlignment()
	{
		return minPeaksForIterativeAlignment;
	}

	public AlignmentParameters setMinPeaksForIterativeAlignment(int minPeaksForIterativeAlignment)
	{
		this.minPeaksForIterativeAlignment = minPeaksForIterativeAlignment;
		return this;
	}

	public double getCorridorRadius()
	{
		return corridorRadius;
	}

	public AlignmentParameters setCorridorRadius(double radius)
	{
		this.corridorRadius = radius;
		return this;
	}

	public boolean getForceFullAlignment()
	{
		return forceFullAlignment;
	}

	public AlignmentParameters setForceFullAlignment(boolean forceFullAlignment)
	{
		this.forceFullAlignment = forceFullAlignment;
		return this;
	}

	public PathMode getPathMode()
	{
		return pathMode;
	}

	public AlignmentParameters setPathMode(PathMode pathMode)
	{
		this.pathMode = pathMode;
		return this;
	}

	public Map<String, String> dump()
	{
		Map<String, String> res = new LinkedHashMap<String, String>();
		res.put( "pathMode", "" + pathMode );
		res.put( "forceFullAlignment", "" + forceFullAlignment );
		res.put( "pathRefinementRadius", "" + corridorRadius );
		res.put( "minPeaksForPathRefinement", "" + minPeaksForIterativeAlignment );
		res.put( "processForkingDepth", "" + maxProcessForkingDepth );
		res.put( "maxDeltaDriftTimeBIN", "" + maxDeltaDriftTime );
		res.put( "maxDeltaMassPPM", "" + maxDeltaMassPPM );
		return res;
	}
}
