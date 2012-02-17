import java.awt.Point;
import java.io.File;


public class Main extends SuadeTestScript{

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Robot.run("Main");

	}

	@Override
	@TplanTest(
			comparisonMethod = "searchbinary", 
			waitFor = "5s", 
			matchRate = 95f,
//			matchArea="desktop")
//			matchArea="5554:avd22")
			matchArea="Rectangle:0,0,1,1"
			)
	public void doTest() throws Exception {
//		connect("java://localhost");
		connect("10.35.180.238","test");
//		waitForMatch(new File[]{new File("c:\\MyCatch.png")}, "search","3s");
		waitForMatch(new File[]{new File("c:\\home.png")}, 98f, "searchbinary","3s");
		System.out.println(getContext().getExitCode());
//		System.out.println(getVariableAsInt("_SEARCH_X"));
	  	Point point = new Point(getVariableAsInt("_SEARCH_X")+10, getVariableAsInt("_SEARCH_Y")+10);
//	  	System.out.println(point.toString());
	  	mouseClick(point);
//	  	mouseMove(new Point(800,600));
		
//		mouseClick(new Point(100, 75));
//	  	type("xX1");
		disconnect();
		
	}

}
