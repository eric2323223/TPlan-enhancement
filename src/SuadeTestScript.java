import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sybase.robot.scripting.commands.impl.ImageUtil;

public abstract class SuadeTestScript extends SybaseJavaTestScript {
	private final static String REPORT_FILE = "report.html";
	private int status = 0;
	private int result = -1;
	private int defaultDelayMS = 1000;
	
	private String password;
	
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
			waitForMatch(new File[] { new File(getResourcePath(imageFile)) }, -1F,
					"3s", "search", null, null, "10s", null);
			if(exitCode()==0){
				return true;
			}else{
				return false;
			}
		} catch (IOException e) {
			return false;
		}
	}
	
	public boolean isExistEdge(String imageFile){
		try {
			waitForMatchEdge(getResourceFiles(imageFile), 90F);
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
			waitForMatch(new File[] { new File(getResourcePath(fileName)) }, -1F,"3s", "search", null, null, "10s", null);
			if(exitCode()!=0){
				break;
			}else{
				Thread.sleep(1000);
			}
		}
	}
	
	public abstract void doTest() throws Exception;

	public void clickOn(String imageFileName) throws IOException, ActionFailedException{
//		String path = getResourcePath(imageFileName);
		File[] files = getResourceFiles(imageFileName);
		waitForMatchEdge(files, 90F);
//		System.out.println(exitCode());
		int width = ImageUtil.getImageWidth(files[0])/2;
		int height = ImageUtil.getImageHeight(files[0])/2;
		if (getContext().getExitCode() == 0) {
			Point point = new Point(getVariableAsInt("_SEARCH_X") + width,	getVariableAsInt("_SEARCH_Y") + height);
//			Point point = new Point(getVariableAsInt("_SEARCH_X") + ImageUtil.getImageWidth(new File(getResourcePath(imageFileName)))/2,
//					getVariableAsInt("_SEARCH_Y") + ImageUtil.getImageHeight(new File(getResourcePath(imageFileName)))/2);
//			System.out.println(point.toString());
			mouseClick(point);
		} 
		result = exitCode();
		if(result>0){
			throw new ActionFailedException("Filed to click on "+imageFileName);
		}
//		System.out.println("clickOn "+imageFileName+": "+result);
	}
	
	public void waitForMatch(String fileName, float accuracyRate)throws IOException{
		waitForMatch(getResourceFiles(fileName), accuracyRate,"3s", "search", null, null, "10s" , null);
//		waitForMatch(new File[] { new File(getResourcePath(fileName)) }, accuracyRate,"3s", "search", null, null, "10s" , null);
	}
	
	public void waitForMatch(String fileName, int timeout) throws IOException, ActionFailedException{
		long start = new Date().getTime();
		long end = 0;
		do{
			String time = new Integer(timeout).toString()+"s";
			waitForMatch(getResourceFiles(fileName), -1F,"3s", "search", null, null, time , null);
//			waitForMatch(new File[] { new File(getResourcePath(fileName)) }, -1F,"3s", "search", null, null, time , null);
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
	
	public void waitForMatchEdge(String fileName, int timeout) throws IOException, ActionFailedException{
		long start = new Date().getTime();
		long end = 0;
		do{
			waitForMatchEdge(getResourceFiles(fileName), 90F);
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

	public void verifyLoginName(String fileName) throws Exception {
		waitForMatch(new File[] { new File(getResourcePath(fileName)) }, -1F, "3s",
				"search", null, null, "10s", null);
		result = exitCode();
		System.out.println("verifyLoginName: "+result);
	}
	
//	public abstract void homeScreen() throws IOException;

	public File reportFile() {
		return new File(getResourceFolder()+"/"+REPORT_FILE);
	}
	
}