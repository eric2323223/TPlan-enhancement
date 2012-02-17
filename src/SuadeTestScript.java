import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sybase.robot.scripting.commands.impl.ImageUtil;

public abstract class SuadeTestScript extends SybaseJavaTestScript {
	private final static String REPORT_FILE = "report.html";
	private int status = 0;
	private int result = 0; //only set result when error occurrs
	private int defaultDelayMS = 1000;
	
	private String password;
	private String comparisonMethod;
	private String waitFor;
	private float matchRate;
	private String matchArea;
	private Rectangle windowRectangle = null;
	
	public void setPassword(String pass){
		password = pass;
	}
	
	public String getPassword(){
		return this.password;
	}

	public String getScriptName() {
		return this.getClass().getName();
	}

	private String baseDir() {
		String str = getClass().getClassLoader().getResource("").toString();
		str = str.replace("file:", "");
		str = str.replace("%20", " ");
		if(str.startsWith("/")){
			str = str.substring(1, str.length());
		}
		if(str.endsWith("/")){
			str = str.substring(0, str.length()-1);
		}
		
//		System.out.println(str);
//		str = str.substring(0, str.indexOf("/bin/"));
		return str;
//		return "C:/Documents and Settings/eric/IBM/rationalsdp/workspace1/DeviceTest";
	}

	public String getResourceFolder() {
		return baseDir() + "/"+getScriptName().substring(0, getScriptName().lastIndexOf(".")).replace(".", "/");
		
	}

	public void test() {
//		setupReport();
		status = 1;
		try{
			getMetaData();
			doTest();
		}catch(ActionFailedException e){
			System.out.println(e.getMessage());
		}catch(Exception e){
			e.printStackTrace();
		}
		status = 2;
	}
	
	public boolean isExist(String imageFile){
		try {
			waitForMatch(getResourceFiles(imageFile), this.matchRate, "1s", this.comparisonMethod, null, this.windowRectangle, this.waitFor, null);
			if(exitCode()==0){
				return true;
			}else{
				return false;
			}
		} catch (IOException e) {
			return false;
		}
	}

	protected TestResult getResult(){
//		if(status == 0){
//			throw new RuntimeException("Script is not started yet!");
//		}
		while(status == 1 || status == 0){
			safeWait(200);
		}
		return new TestResult(result);
	}
	
    private void safeWait(long time) {
        long endTime = System.currentTimeMillis() + time;
        while (System.currentTimeMillis() < endTime) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
            }
        }
    }
	
	public int exitCode(){
//		Object ec = getContext().getVariable("_EXIT_CODE");
//		if(ec instanceof Integer){
//			return ((Integer)ec).intValue();
//		}else{
//			throw new RuntimeException("Unable to handle exit code of type: "+ec.getClass());
//		}
		
		return getContext().getExitCode();
	}

	private void setupReport() {
		try{
			getContext().setVariable("_REPORT_DIR", getResourceFolder());
			report(new File(REPORT_FILE), null, getScriptName(), "file");
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	public void waitForDispear(String fileName) throws IOException, InterruptedException{
		while(true){
			waitForMatch(getResourceFiles(fileName), this.matchRate,"1s", this.comparisonMethod, null, this.windowRectangle, this.waitFor, null);
			if(exitCode()!=0){
				break;
			}else{
				Thread.sleep(1000);
			}
		}
	}
	
	public abstract void doTest() throws Exception;

	public void clickOn(String imageFileName) throws IOException, ActionFailedException{
		File[] files = getResourceFiles(imageFileName);
		waitForMatch(files, this.matchRate, "1s", this.comparisonMethod, null, this.windowRectangle, this.waitFor, null);
		System.out.println(this.windowRectangle);
		int width = ImageUtil.getImageWidth(files[0])/2;
		int height = ImageUtil.getImageHeight(files[0])/2;
		if (getContext().getExitCode() == 0) {
			Point point = new Point(getVariableAsInt("_SEARCH_X") + width,	getVariableAsInt("_SEARCH_Y") + height);
//			Point point = new Point(getVariableAsInt("_SEARCH_X") + ImageUtil.getImageWidth(new File(getResourcePath(imageFileName)))/2,
//					getVariableAsInt("_SEARCH_Y") + ImageUtil.getImageHeight(new File(getResourcePath(imageFileName)))/2);
//			System.out.println(point.toString());
			if(this.windowRectangle!=null){
				Point p = new Point(point.x+this.windowRectangle.x, point.y+this.windowRectangle.y);
				mouseClick(p);
			}else{
				mouseClick(point);
			}
		} 
		result = exitCode();
		if(result>0){
			throw new ActionFailedException("Filed to click on "+imageFileName);
		}
//		System.out.println("clickOn "+imageFileName+": "+result);
	}
	
	public void waitForMatch(String fileName, int timeout) throws IOException, ActionFailedException{
		long start = new Date().getTime();
		long end = 0;
		do{
			waitForMatch(getResourceFiles(fileName), this.matchRate, "1s", this.comparisonMethod, null, this.windowRectangle, this.waitFor, null);
			if(exitCode()!=0){
				try {
					Thread.sleep(defaultDelayMS);
					System.out.println("~~~~~~");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}else{
				break;
			}
		}while(end - start < timeout*defaultDelayMS);
		result = exitCode();
		if(result>0){
			throw new ActionFailedException("Failed to match "+fileName);
		}
	}

	private String getResourcePath(String file) {
		if(new File(getResourceFolder() + "/" + file).exists()){
			return getResourceFolder() + "/" + file;
		}else if(new File(getFeatureCommonResourcePath(getScriptName())+"/"+file).exists()){
			return getFeatureCommonResourcePath(getScriptName())+"/"+file;
		}else if(new File(getWorkflowCommonResourcePath()+"/"+file).exists()){
			return getWorkflowCommonResourcePath()+"/"+file;
		}else if(new File(getGlobalCommonResourcePath()+"/"+file).exists()){
			return getGlobalCommonResourcePath()+"/"+file;
		}else{
			throw new RuntimeException("Cannot find resource file: "+file);
		}
	}
	
	public File[] getResourceFiles(String name) {
//		String fileName = name+".png";
		if(hasFileInDir(getResourceFolder(), name)){
			return getAllFiles(getResourceFolder() , name);
		}else if(hasFileInDir(getFeatureCommonResourcePath(getScriptName()),name)){
			return getAllFiles(getFeatureCommonResourcePath(getScriptName()),name);
		}else if(hasFileInDir(getWorkflowCommonResourcePath(),name)){
			return getAllFiles(getWorkflowCommonResourcePath(), name);
		}else if(hasFileInDir(getGlobalCommonResourcePath(), name)){
			return getAllFiles(getGlobalCommonResourcePath(), name);
		}else{
			throw new RuntimeException("Cannot find resource file: "+name);
		}
	}
	
	private boolean hasFileInDir(String folder, String fileName){
		File dir = new File(folder);
		if(dir.exists()){
			File[] allFiles = dir.listFiles();
			List<File> files = new ArrayList<File>();
			for(File file:allFiles){
				Pattern p = Pattern.compile(fileName+"(_\\d)?.png");
				Matcher m = p.matcher(file.getName());
				if(m.matches()){
					return true;
				}
			}
			return false;
		}else{
			return false;
		}
	}
	
	private File[] getAllFiles(String folder, String fileName){
		if(new File(folder+"\\"+fileName+".png").exists()){
			return new File[]{new File(folder+"\\"+fileName+".png")};
		}else{
			File dir = new File(folder);
			File[] allFiles = dir.listFiles();
			List<File> files = new ArrayList<File>();
			for(File file:allFiles){
				System.out.println(file.getAbsolutePath());
				Pattern p = Pattern.compile(fileName+"(_\\d)?.png");
				Matcher m = p.matcher(file.getName());
				if(m.matches()){
					files.add(file);
				}
			}
			return (File[])files.toArray(new File[0]);
		}
	}
	
	private String getFeatureCommonResourcePath(String file){
		Matcher m  = Pattern.compile("tplan.Workflow.([^.]+)").matcher(file); 
		if(m.find()){
			String feature = m.group(0);
			return baseDir()+"/tplan/Workflow/"+feature+"/common";
		}else{
			throw new RuntimeException("Cannot find common resource folder for script "+file);
		}
	}

	private String getWorkflowCommonResourcePath() {
		return baseDir()+"/tplan/Workflow/common";
	}
	
	private String getGlobalCommonResourcePath(){
		return baseDir()+"/tplan/common";
	}

//	public void verifyLoginName(String fileName) throws Exception {
//		waitForMatch(new File[] { new File(getResourcePath(fileName)) }, 95F, "3s",
//				"search", null, null, "10s", null);
//		result = exitCode();
//		System.out.println("verifyLoginName: "+result);
//	}
	
//	public abstract void homeScreen() throws IOException;

	public File reportFile() {
		return new File(getResourceFolder()+"/"+REPORT_FILE);
	}
	
	private void getMetaData() throws ActionFailedException{
		try {
			Method m = getClass().getMethod("doTest");
			TplanTest anno = m.getAnnotation(TplanTest.class);
			this.comparisonMethod = anno.comparisonMethod();
			this.waitFor = anno.waitFor();
			this.matchRate = anno.matchRate();
			this.matchArea = anno.matchArea();
			getWindowRectangle(this.matchArea);
		} catch (Exception e) {
			throw new ActionFailedException("Failed to retrieve metadata of doTest method");
		} 
	}

	private void getWindowRectangle(String str) {
		if(str.equalsIgnoreCase("desktop")){
			this.windowRectangle = null;
		}else if(str.toLowerCase().startsWith("rectangle:")){
			String rectangleStr = str.split(":")[1].trim();
			String parts[] = rectangleStr.split(",");
			int x = new Integer(parts[0]).intValue();
			int y = new Integer(parts[1]).intValue();
			int width = new Integer(parts[2]).intValue();
			int height = new Integer(parts[3]).intValue();
			this.windowRectangle = new Rectangle(x, y, width, height);
		}else{
//				this.windowRectangle = WindowSpy.getWindowRectangle(str);
		}
	}
	
}