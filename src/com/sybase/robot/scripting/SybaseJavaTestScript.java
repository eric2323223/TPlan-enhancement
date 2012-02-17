package com.sybase.robot.scripting;

import java.awt.Image;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
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
	
	@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @interface Param {

        // Name identifies name of the command parameter as specified in the scripting language spec
        String name();

        // Default value is used when the parameter is optional and no value is provided.
        // Empty string is considered as undefined default value.
        // If the value is defined, an attempt will be made to convert it to the
        // desired parameter type. For example, for a parameter of "float" type
        // the value will be parsed through Float.parseFloat(String).
        String defaultValue() default "";

        // Template may contain snippets of Java code. It allows to specify how to
        // convert a value passed from the scripting language into desired Java type.
        // Any occurence of @value@ will be replaced with the parameter value.
        // A good example are mouse buttons: the scripting language recognizes "left",
        // "middle" and "right" but the corresponding Java method accepts only an
        // integer button identifier specified in the MouseEvent class. See the
        // mouseEvent() method for the code snippet example.
        String template() default "";
    };

    @Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @interface Command {
        // Name identifies which command the method covers.

        String name();
    };
    
    private final String DEFAULT_MOUSE_COORDS = "";
    private final String MODIFIER_CONVERSION_SNIPPET = "@modifiers@";//"getContext().getParser().parseModifiers(\"@value@\")";
    private final String MOUSE_BUTTON_CONVERSION_SNIPPET = "@mouseButton@"; //"getContext().getParser().parseMouseButton(\"@value@\")";
    private final String FILE_LIST_SNIPPET = "@fileList@";
    private final String OUTPUT_FILE_STREAM_SNIPPET = "new java.io.FileOutputStream(\"@value@\")";
    private final String KEY_LOCATION_CONVERSION_SNIPPET = "@keyLocation@";
	
    public int waitForGrayScale(String event, Rectangle area, String extent, Boolean cumulative,
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
    	return runScriptCommand("waitForGrayScale", l, m);
    }
    
    @Command(name = "waitfor match")
    public int waitForGrayScaleMatch(@Param(name = "template", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "passrate") float passRate,
            @Param(name = "interval") String interval,
            @Param(name = "method") String method,
            @Param(name = "methodparams") String methodParams,
            @Param(name = "cmparea") Rectangle cmpArea,
            @Param(name = "timeout") String timeout,
            @Param(name = "wait") String wait) throws IOException {
        return waitForGrayScale(WaitforCommand.EVENT_MATCH, null, null, null, templates, passRate, interval, method, methodParams, cmpArea, -1, timeout, wait);
    }
    
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

    // ====== WAITFOR MATCH
    public int waitForMatchEdgeMatch(Image templates[],
            @Param(name = "passrate") float passRate,
            @Param(name = "interval") String interval,
            @Param(name = "method") String method,
            @Param(name = "methodparams") String methodParams,
            @Param(name = "cmparea") Rectangle cmpArea,
            @Param(name = "timeout") String timeout,
            @Param(name = "wait") String wait) throws IOException {
        return waitForMatchEdge(WaitforCommand.EVENT_MATCH, null, null, null, templates, passRate, interval, method, methodParams, cmpArea, -1, timeout, wait);
    }

    @Command(name = "waitfor match")
    public int waitForMatchEdgeMatch(@Param(name = "template", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "passrate") float passRate,
            @Param(name = "interval") String interval,
            @Param(name = "method") String method,
            @Param(name = "methodparams") String methodParams,
            @Param(name = "cmparea") Rectangle cmpArea,
            @Param(name = "timeout") String timeout,
            @Param(name = "wait") String wait) throws IOException {
        return waitForMatchEdge(WaitforCommand.EVENT_MATCH, null, null, null, templates, passRate, interval, method, methodParams, cmpArea, -1, timeout, wait);
    }

    @Command(name = "waitfor match")
    public int waitForMatchEdgeMatch(@Param(name = "template", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "passrate") float passRate,
            @Param(name = "method") String method,
            @Param(name = "cmparea") Rectangle cmpArea,
            @Param(name = "timeout") String timeout,
            @Param(name = "wait") String wait) throws IOException {
        return waitForMatchEdge(WaitforCommand.EVENT_MATCH, null, null, null, templates, passRate, null, method, null, cmpArea, -1, timeout, wait);
    }

    @Command(name = "waitfor match")
    public int waitForMatchEdgeMatch(@Param(name = "template", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "passrate") float passRate,
            @Param(name = "method") String method,
            @Param(name = "timeout") String timeout) throws IOException {
        return waitForMatchEdge(WaitforCommand.EVENT_MATCH, null, null, null, templates, passRate, null, method, null, null, -1, timeout, null);
    }

    @Command(name = "waitfor match")
    public int waitForMatchEdgeMatch(@Param(name = "template", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "method") String method,
            @Param(name = "timeout") String timeout) throws IOException {
        return waitForMatchEdge(WaitforCommand.EVENT_MATCH, null, null, null, templates, -1f, null, method, null, null, -1, timeout, null);
    }

    @Command(name = "waitfor match")
    public int waitForMatchEdgeMatch(@Param(name = "template", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "timeout") String timeout) throws IOException {
        return waitForMatchEdge(WaitforCommand.EVENT_MATCH, null, null, null, templates, -1f, null, null, null, null, -1, timeout, null);
    }

    @Command(name = "waitfor match")
    public int waitForMatchEdgeMatch(@Param(name = "template", template = FILE_LIST_SNIPPET) File templates[]) throws IOException {
        return waitForMatchEdge(WaitforCommand.EVENT_MATCH, null, null, null, templates, -1f, null, null, null, null, -1, null, null);
    }

    // ====== WAITFOR MISMATCH
    public int waitForMatchEdgeMismatch(Image templates[],
            @Param(name = "passrate") float passRate,
            @Param(name = "interval") String interval,
            @Param(name = "method") String method,
            @Param(name = "methodparams") String methodParams,
            @Param(name = "cmparea") Rectangle cmpArea,
            @Param(name = "timeout") String timeout,
            @Param(name = "wait") String wait) throws IOException {
        return waitForMatchEdge(WaitforCommand.EVENT_MISMATCH, null, null, null, templates, passRate, interval, method, methodParams, cmpArea, -1, timeout, wait);
    }

    @Command(name = "waitfor mismatch")
    public int waitForMatchEdgeMismatch(@Param(name = "template", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "passrate") float passRate,
            @Param(name = "interval") String interval,
            @Param(name = "method") String method,
            @Param(name = "methodparams") String methodParams,
            @Param(name = "cmparea") Rectangle cmpArea,
            @Param(name = "timeout") String timeout,
            @Param(name = "wait") String wait) throws IOException {
        return waitForMatchEdge(WaitforCommand.EVENT_MISMATCH, null, null, null, templates, passRate, interval, method, methodParams, cmpArea, -1, timeout, wait);
    }

    @Command(name = "waitfor mismatch")
    public int waitForMatchEdgeMismatch(@Param(name = "template", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "passrate") float passRate,
            @Param(name = "method") String method,
            @Param(name = "cmparea") Rectangle cmpArea,
            @Param(name = "timeout") String timeout,
            @Param(name = "wait") String wait) throws IOException {
        return waitForMatchEdge(WaitforCommand.EVENT_MISMATCH, null, null, null, templates, passRate, null, method, null, cmpArea, -1, timeout, wait);
    }

    @Command(name = "waitfor mismatch")
    public int waitForMatchEdgeMismatch(@Param(name = "template", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "passrate") float passRate,
            @Param(name = "method") String method,
            @Param(name = "timeout") String timeout) throws IOException {
        return waitForMatchEdge(WaitforCommand.EVENT_MISMATCH, null, null, null, templates, passRate, null, method, null, null, -1, timeout, null);
    }

    @Command(name = "waitfor mismatch")
    public int waitForMatchEdgeMismatch(@Param(name = "template", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "method") String method,
            @Param(name = "timeout") String timeout) throws IOException {
        return waitForMatchEdge(WaitforCommand.EVENT_MISMATCH, null, null, null, templates, -1f, null, method, null, null, -1, timeout, null);
    }

    @Command(name = "waitfor mismatch")
    public int waitForMatchEdgeMismatch(@Param(name = "template", template = FILE_LIST_SNIPPET) File templates[],
            @Param(name = "timeout") String timeout) throws IOException {
        return waitForMatchEdge(WaitforCommand.EVENT_MISMATCH, null, null, null, templates, -1f, null, null, null, null, -1, timeout, null);
    }

    @Command(name = "waitfor mismatch")
    public int waitForMatchEdgeMismatch(@Param(name = "template", template = FILE_LIST_SNIPPET) File templates[]) throws IOException {
        return waitForMatchEdge(WaitforCommand.EVENT_MISMATCH, null, null, null, templates, -1f, null, null, null, null, -1, null, null);
    }

}
