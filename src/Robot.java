import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.AutomatedRunnable;
import com.tplan.robot.scripting.DefaultJavaTestScript;
import com.tplan.robot.scripting.JavaTestScript;

public class Robot extends DefaultJavaTestScript implements JavaTestScript {
	
	public static TestResult run(String scriptName) {
		try {
			Class scriptClass = Class.forName(scriptName);
			SuadeTestScript scriptInstance = (SuadeTestScript)scriptClass.newInstance();
			ApplicationSupport robot = new ApplicationSupport();
			String[] params = {"--nodisplay"};
			AutomatedRunnable t = robot.createAutomatedRunnable((SuadeTestScript) scriptInstance, "javatest",params, System.out, false);
//			AutomatedRunnable t = robot.createAutomatedRunnable((JavaTestScript) scriptInstance, "javatest",params, System.out, false);
			t.run();
//			new Thread(t).start();
			return scriptInstance.getResult();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("TPlan script "+scriptName+" failed becuase of "+e.getClass());
		}
	}
	
	public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException, InterruptedException{
//		System.out.println(System.getProperty("user.dir"));
		System.out.println(run("testscript.tplan.android.SampleScript").isPass());
	}

	private static String parseScriptName(String scriptName) {
		Matcher androidMatcher = Pattern.compile(Pattern.quote("_android"), Pattern.CASE_INSENSITIVE).matcher(scriptName);
		Matcher wmMatcher = Pattern.compile(Pattern.quote("_WM"), Pattern.CASE_INSENSITIVE).matcher(scriptName);
		if(androidMatcher.find()){
			String androidStr = androidMatcher.group(0);
			String result = scriptName.replace(androidStr, "").replace("testscript", "tplan"); 
			int position = result.lastIndexOf(".");
			result = result.substring(0, position)+".android."+result.substring(position+1, result.length())+".Script";
			return result; 
		}
		if(wmMatcher.find()){
			String wmStr = wmMatcher.group(0);
			String result = scriptName.replace(wmStr, "").replace("testscript", "tplan"); 
			int position = result.lastIndexOf(".");
			result = result.substring(0, position)+".WM."+result.substring(position+1, result.length())+".Script";
			return result; 
		}
		else{
			throw new RuntimeException("Unable to get T-Plan test script for "+scriptName);
		}
	}

}
