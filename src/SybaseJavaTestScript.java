import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tplan.robot.scripting.DefaultJavaTestScript;
import com.tplan.robot.scripting.StopRequestException;
import com.tplan.robot.scripting.commands.AbstractCommandHandler;
import com.tplan.robot.scripting.commands.impl.CompareToCommand;
import com.tplan.robot.scripting.commands.impl.WaitforCommand;

public class SybaseJavaTestScript extends DefaultJavaTestScript {
	
	public int waitForMatchEdge(String event, Rectangle area, String extent, Boolean cumulative,
            Object templates, float passRate, String interval, String method, String methodParams,
            Rectangle cmpArea, int count, String timeout, String wait) throws IllegalArgumentException, StopRequestException, IOException{
		List l = new ArrayList();
        Map m = new HashMap();
        if (event != null) {
            l.add(event);
        }
        if (count > 0) {
            l.add(AbstractCommandHandler.PARAM_COUNT);
            m.put(AbstractCommandHandler.PARAM_COUNT, count);
        }
        if (wait != null) {
            l.add(AbstractCommandHandler.PARAM_WAIT);
            m.put(AbstractCommandHandler.PARAM_WAIT, wait);
        }
        if (timeout != null) {
            l.add(WaitforCommand.PARAM_TIMEOUT);
            m.put(WaitforCommand.PARAM_TIMEOUT, timeout);
        }
        if (area != null) {
            l.add(WaitforCommand.PARAM_AREA);
            m.put(WaitforCommand.PARAM_AREA, area);
        }
        if (extent != null) {
            l.add(WaitforCommand.PARAM_EXTENT);
            m.put(WaitforCommand.PARAM_EXTENT, extent);
        }
        if (cumulative != null) {
            l.add(WaitforCommand.PARAM_CUMULATIVE);
            m.put(WaitforCommand.PARAM_CUMULATIVE, cumulative);
        }
        if (templates != null) {
            if (templates instanceof File[]) {
                List<File> lf = Arrays.asList((File[])templates);
                l.add(WaitforCommand.PARAM_TEMPLATE);
                m.put(WaitforCommand.PARAM_TEMPLATE, lf);
            } else {
                l.add(WaitforCommand.PARAM_TEMPLATE);
                m.put(WaitforCommand.PARAM_TEMPLATE, templates);
            }
        }
        if (passRate >= 0) {
            l.add(CompareToCommand.PARAM_PASSRATE);
            m.put(CompareToCommand.PARAM_PASSRATE, passRate);
        }
        if (interval != null) {
            l.add(WaitforCommand.PARAM_INTERVAL);
            m.put(WaitforCommand.PARAM_INTERVAL, interval);
        }
        if (method != null) {
            l.add(CompareToCommand.PARAM_METHOD);
            m.put(CompareToCommand.PARAM_METHOD, method);
        }
        if (methodParams != null) {
            l.add(CompareToCommand.PARAM_METHODPARAMS);
            m.put(CompareToCommand.PARAM_METHODPARAMS, methodParams);
        }
        if (cmpArea != null) {
            l.add(CompareToCommand.PARAM_CMPAREA);
            m.put(CompareToCommand.PARAM_CMPAREA, cmpArea);
        }
        return runScriptCommand("waitForEdge", l, m);
	}
	
	public int waitForMatchEdge(File templates[]) throws IOException {
        return waitForMatchEdge(WaitforCommand.EVENT_MATCH, null, null, null, templates, -1f, null, "searchbinary", null, null, -1, "5s", null);
//        return waitForMatchEdge(WaitforCommand.EVENT_MATCH, null, null, null, templates, -1f, null, "search", null, null, -1, "5s", null);
    }
	
	public int waitForMatchEdge(File templates[], String timeout) throws IOException {
		return waitForMatchEdge(WaitforCommand.EVENT_MATCH, null, null, null, templates, -1f, null, "searchbinary", null, null, -1, timeout, null);
//        return waitForMatchEdge(WaitforCommand.EVENT_MATCH, null, null, null, templates, -1f, null, "search", null, null, -1, "5s", null);
	}
	
	public int waitForMatchEdge(File templates[], float passRate) throws IOException {
		return waitForMatchEdge(WaitforCommand.EVENT_MATCH, null, null, null, templates, passRate, null, "searchbinary", null, null, -1, "5s", null);
//		return waitForMatchEdge(WaitforCommand.EVENT_MATCH, null, null, null, templates, passRate, null, "search", null, null, -1, "5s", null);
	}
	
	
	public int waitForMatchEdge(File templates[], String method, float passRate) throws IOException {
        return waitForMatchEdge(WaitforCommand.EVENT_MATCH, null, null, null, templates, passRate, null, method, null, null, -1, "5s", null);
    }

}