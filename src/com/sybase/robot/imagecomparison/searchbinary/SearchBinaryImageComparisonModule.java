package com.sybase.robot.imagecomparison.searchbinary;

import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

import com.sybase.robot.scripting.commands.impl.ImageUtil;
import com.tplan.robot.imagecomparison.search.SearchImageComparisonModule;
import com.tplan.robot.scripting.ScriptingContext;

public class SearchBinaryImageComparisonModule extends SearchImageComparisonModule {
	private static final String MATCH_RESULT_FILE = "c:\\image_match_result.png";
	List<int[]> tsm;
	List<int[]> tmm;
	int[] mm;
	Rectangle mmArea;
	boolean debugMode = true;

	public void setDebugMode(boolean b){
		this.debugMode = b;
	}
	
    public String getCode() {
        return "searchbinary";
    }

    public String getUniqueId() {
    	return "Sybase_Search_binary_image_comparison_module";
    }
    
    public void setBaseImage(Image img){
    	BufferedImage image;
    	if(debugMode){
    		image = ImageUtil.edgefy(img, new File("c:\\template.png"), 3);
    	}else{
    		image = ImageUtil.edgefy(img, null, 3);
    	}
    	super.setBaseImage(image);
    }
    
	public float compareToBaseImage(Image desktopImage, Rectangle area,	String methodParams, ScriptingContext repository, float passRate){
		BufferedImage desktopimg = getMatchArea(desktopImage, area);
		if(debugMode){
			desktopimg = ImageUtil.edgefy(desktopimg, new File("c:\\desktop.png"), 3);
		}else{
			desktopimg = ImageUtil.edgefy(desktopimg, null, 3);
		}
		Rectangle fullRect = new Rectangle(0,0,desktopimg.getWidth(), desktopimg.getHeight());
		Point matchPoint = compareToBaseImage(desktopimg, fullRect, passRate);
		Map variables = repository.getVariables();
		if(matchPoint!=null){
			variables.put("_SEARCH_X", matchPoint.x);
			variables.put("_SEARCH_Y", matchPoint.y);
			System.out.println(matchPoint.toString());
			drawMatchResultImage((BufferedImage) desktopImage, mmArea, matchPoint.x, matchPoint.y, r, new File(MATCH_RESULT_FILE));
			return 1;
		}else{
			variables.put("_SEARCH_X", new Integer(-1));
			variables.put("_SEARCH_Y", new Integer(-1));
			return -1;
		}
	}
	
	private BufferedImage getMatchArea(Image img, Rectangle area){
		if(area!=null){
			BufferedImage image = ImageUtil.imageToBufferedImage(img);
			return image.getSubimage(area.x, area.y, area.width, area.height);
		}else{
			return ImageUtil.imageToBufferedImage(img);
		}
	}
	
	public Point compareToBaseImage(Image desktopImage, Rectangle area, float passRate){
//		BufferedImage desktopimg = ImageUtil.edgefy(desktopImage, new File("d:\\desktop.png"), 0);
		BufferedImage desktopimg = (BufferedImage)desktopImage;
		Rectangle fullRect = new Rectangle(0, 0, desktopimg.getWidth(), desktopimg.getHeight());
		if (area == null) {
			area = fullRect;
		} else {
			area = area.intersection(fullRect);
		}
		mm = loadIntArrayFromImage(desktopimg, area);
		mmArea = area;
		Point p = compareMatrixFirst(mm, mmArea, pixels, r, passRate);
		if(p!=null && debugMode){
			drawMatchResultImage((BufferedImage) desktopimg, area, p.x, p.y, r, new File(MATCH_RESULT_FILE));
		}
		return p;
	}
	
	public Point compareMatrixFirst(int[] mainMatrix, Rectangle mr,int[] subMatrix, Rectangle sr,float passRate){
		List<int[]> tmm = chainCodeTransformMatrix(mainMatrix, mr);
		List<int[]> tsm = chainCodeTransformMatrix(subMatrix, sr);
		for(int i=0;i<=mr.height - sr.height;i++){
			int[] tmmLine = tmm.get(i);
			int[] tsmLine = tsm.get(0);
			int[] positions = getSubIndexOfArray(tmmLine, tsmLine);
			for(int position:positions){
				int[] ps = getOriginalMatrixColmIndex2(tmmLine, tsmLine, position);
				for(int x=0; x<ps.length; x++){
					int misCount = match(ps[x], i, mainMatrix, subMatrix, mr, sr, passRate);
					if(misCount >= 0){
						return new Point(ps[x], i);
					}
				}
			}
		}
		return null;
	}
	
	public Point compareMatrixBest(int[] mainMatrix, Rectangle mr,int[] subMatrix, Rectangle sr,float passRate){
//		List<Point> points = new ArrayList<Point>();
		Point point = null;
		int minMissCount = 999999999;
		List<int[]> tmm = chainCodeTransformMatrix(mainMatrix, mr);
		List<int[]> tsm = chainCodeTransformMatrix(subMatrix, sr);
		for(int i=0;i<=mr.height - sr.height;i++){
			int[] tmmLine = tmm.get(i);
			int[] tsmLine = tsm.get(0);
			int[] positions = getSubIndexOfArray(tmmLine, tsmLine);
			for(int position:positions){
				int[] ps = getOriginalMatrixColmIndex2(tmmLine, tsmLine, position);
				for(int x=0; x<ps.length; x++){
					int misCount = match(ps[x], i, mainMatrix, subMatrix, mr, sr, passRate);
					if(misCount >= 0 && misCount<minMissCount){
						if(misCount == 0){
							return new Point(ps[x], i);
						}else{
							minMissCount = misCount;
							point = new Point(ps[x], i);
						}
					}
				}
			}
		}
		return point;
	}

	private void drawMatchResultImage(BufferedImage img, Rectangle imageArea, int x, int y, Rectangle matchArea, File file) {
		int[] image = loadIntArrayFromImage(img, imageArea);
		BufferedImage bi = new BufferedImage(imageArea.width, imageArea.height, BufferedImage.TYPE_INT_BGR);
		for(int i=0;i<image.length;i++){
//			System.out.print(i+",");
			int px = i%imageArea.width;
			int py = i/imageArea.width;
			if((py == y && px >= x && px < x+matchArea.width) ||
					(py == y+matchArea.height && px >= x && px < x+matchArea.width)||
					(px == x && py>=y && py<y+matchArea.height) ||
					(px == x+matchArea.width && py >= y && py< y+matchArea.height)){
				int rgb = 0xFF00FF00;
				bi.setRGB(px, py, rgb);
			}else{
				bi.setRGB(px, py, img.getRGB(px, py));
			}
		}
		ImageUtil.writeBufferedImageToFile(bi, file);
		
	}

	private int match(int mmRow, int mmColumn, final int[] mm, final int[] sm,Rectangle mr, Rectangle sr, float passRate)
	{
		int totalMismatch = (int)(sr.width * sr.height * (1-passRate));
		int mismatchCount = 0;
		for(int x=0; x<sr.width; x++){
			for(int y=0;y<sr.height;y++){
				if(mismatchCount > totalMismatch){
					return -1;
				} else if(mmColumn + y >= mr.height){
					return -1;
				}
				else{
					int mmValue = mm[(mmColumn+y)*mr.width + mmRow+x];
//					System.out.print("mm["+(int)(mmRow+x)+","+(int)(mmColumn+y)+"]="+mmValue);
					int smValue = sm[y*sr.width+x];
//					System.out.println(" sm["+x+","+y+"]="+mmValue);
					
					if((mmValue!=WHITE && mmValue!=BLACK) || (smValue!=WHITE && smValue != BLACK)){
						System.out.println("invalid color for comparison");
					}
					
					if(mmValue!=smValue){
						mismatchCount++;
					}
				}
			}
		}
		return mismatchCount;
	}

	private int[] loadIntArrayFromImage(Image img, Rectangle r) {
		int width = r.width;
		int height = r.height;
		int ai[]; // = new int[width * height];

		if (img instanceof BufferedImage) {
			BufferedImage bi = (BufferedImage) img;
//			System.out.println(r.toString());
			ai = ((BufferedImage) img).getRGB(r.x, r.y, width, height,
					(int[]) null, 0, width);

		} else {
			ai = new int[width * height];
			PixelGrabber pixelgrabber = new PixelGrabber(img, r.x, r.y, width,
					height, ai, 0, width);
			try {
				pixelgrabber.grabPixels();
			} catch (InterruptedException interruptedexception) {
				interruptedexception.printStackTrace();
			}
		}
		return ai;
	}

	public int getOriginalMatrixColmIndex(final int[] tmml, final int[] tsml,	int index) {
		int result = 0;
		for (int i = 0; i < index; i++) {
			result = result + tmml[i];
		}
		result = result + tmml[index] - tsml[0];
		return result;
	}
	
	public int[] getOriginalMatrixColmIndex2(final int[] tmml, final int[] tsml, int index) {
		if(tsml.length == 1){
			int[] positions = new int[tmml[index] - tsml[0] + 1];
			for(int i = 0 ;i<positions.length;i++){
				int offset = 0;
				for(int j=0;j<index;j++){
					offset = offset+tmml[j];
				}
				positions[i] = offset + i;
			}
			return positions;
		}else{
			int result = 0;
			for (int i = 0; i < index; i++) {
				result = result + tmml[i];
			}
			result = result + tmml[index] - tsml[0];
			return new int[]{result};
		}
	}

	public int[] getSubIndexOfArray(final int[] mml, final int[] sml) {
		List<Integer> positions = new ArrayList<Integer>();
		int startM = 0;
		boolean found = true;
		while (startM <= mml.length) {
			if (startM + sml.length > mml.length) {
				break;
			} else {
				for (int i = 0; i < sml.length; i++) {
					if (mml[startM + i] < sml[i]) {
						found = false;
						break;
					}
				}
//				if (found == false) {
//					startM = startM + 2;
//					found = true;
//					continue;
//				} else {
//					positions.add(startM);
//					startM++;
//					continue;
//				}
				if(found){
					positions.add(startM);
				}
				startM = startM + 2;
				found = true;
			}
		}
		return ArrayUtils.toPrimitive(positions.toArray(new Integer[positions.size()]));
//		return positions;
	}

	public List<int[]> chainCodeTransformMatrix(int[] pixels, Rectangle r) {
		List<int[]> matrix = new ArrayList<int[]>();
		int width = r.width;
		int height = r.height;
		for (int i = 0; i < height; i++) {
			int[] list = chainCodeTransformLine(ArrayUtils.subarray(pixels, i* width, (i+1) * width));
//			int[] list = chainCodeTransformLine(ArrayUtils.subarray(pixels, i* height + width, i * (height + 1)));
//			System.out.println(list.length);
			matrix.add(list);
		}
		return matrix;
	}

	private final int WHITE = -1;
	private final int BLACK = -16777216;
	private final int NACOLOR = 250;
	
	public int[] chainCodeTransformLine(int[] line) {
		List<Integer> list = new ArrayList<Integer>();
		int previous = NACOLOR;
		int sameCount = 0;
		boolean is0 = true;
		for (int i = 0; i < line.length; i++) {
//			System.out.println(line[i]);
			if (previous != line[i]) {
				if (i == 0) {
					if (line[i] == BLACK) {
						list.add(0);
						// System.out.print(0);
					}
				} else {
					list.add(sameCount);
					// System.out.print(sameCount);
				}
				sameCount = 1;
				is0 = !is0;
			} else {
				sameCount++;
			}
			previous = line[i];
		}
		list.add(sameCount);
		// System.out.println(sameCount);
		return ArrayUtils.toPrimitive(list.toArray(new Integer[list.size()]));
		// return list;
	}

	public static void main(String[] args) throws IOException {
		SearchBinaryImageComparisonModule module = new SearchBinaryImageComparisonModule();
//		BufferedImage template = ImageUtil.readImage("c:\\template.png");
//		BufferedImage desktop = ImageUtil.readImage("c:\\desktop.png");
//		module.setBaseImage(template);
//		module.compareToBase(desktop, new Rectangle(0,0, desktop.getWidth(), desktop.getHeight()), 0.93F);

		
//		System.out.println(module.getSubIndexOfArray(new int[] { 1,2}, new int[] { 0, 2 }).toString());
//		int[] positions = module.getSubIndexOfArray(new int[] { 1,2}, new int[] { 0, 2 });
//		System.out.println(module.getOriginalMatrixColmIndex(new int[] { 1,2}, new int[] { 0, 2 }, positions[0]));
	
		
//		int[] mm = new int[]{0,0,0,0,0, 0,0,1,0,0, 0,1,1,1,0, 0,0,1,0,0, 0,0,0,0,0};
//		Rectangle mr = new Rectangle(0,0,5,5);
//		int[] sm = new int[]{0,1,0, 1,1,1, 0,1,0};
//		Rectangle sr = new Rectangle(0,0,3,3);
		
//		int[] mm = new int[]{0,0,0,0, 0,0,0,0, 0,0,0,1, 0,0,1,1};
//		Rectangle mr = new Rectangle(0,0,4,4);
//		int[] sm = new int[]{0,0, 0,1, 1,1};
//		Rectangle sr = new Rectangle(0,0,2,3);
//		Point point = module.compareMatrixFirst(mm, mr, sm, sr, 1F);
//		System.out.println(point.toString());
		
//		BufferedImage template = ImageUtil.readImage("d:\\screenshots\\x2.png");
//		BufferedImage desktop = ImageUtil.readImage("d:\\screenshots\\x.png");
//		BufferedImage desktop = ImageUtil.readImage("c:\\desktop_android.png");
//		BufferedImage template = ImageUtil.readImage("c:\\home.png");
//		BufferedImage desktop = ImageUtil.readImage("c:\\eclipse.png");
//		BufferedImage template = ImageUtil.readImage("c:\\mycatch4.png");
		
		
		BufferedImage desktop = ImageUtil.readImage("d:\\desktop.png");
		BufferedImage template = ImageUtil.readImage("d:\\template.png");
		
		module.setBaseImage(template);
		long start = System.currentTimeMillis();
		Point p = module.compareToBaseImage(desktop, null, 0.98f);
		long end = System.currentTimeMillis();
		System.out.println((end - start));
		System.out.println(p.toString());
	}
}
