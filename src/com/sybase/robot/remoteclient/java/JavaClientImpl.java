package com.sybase.robot.remoteclient.java;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.KeyStroke;

import com.tplan.robot.gui.DesktopViewer;
import com.tplan.robot.plugin.DependencyMissingException;
import com.tplan.robot.plugin.PluginManager;
import com.tplan.robot.preferences.Preference;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.remoteclient.RemoteDesktopClient;
import com.tplan.robot.remoteclient.RemoteDesktopClientEvent;
import com.tplan.robot.remoteclient.RemoteDesktopClientListener;
import com.tplan.robot.remoteclient.RemoteDesktopServerEvent;
import com.tplan.robot.remoteclient.RemoteDesktopServerListener;
import com.tplan.robot.remoteclient.rfb.PasswordRequiredException;
import com.tplan.robot.remoteclient.rfb.RfbClientImpl;
import com.tplan.robot.util.Utils;

public class JavaClientImpl extends RfbClientImpl{
	
	Robot robot ;  
	Rectangle screenRectangle;
	
	private Robot robot(){
		if(robot==null){
			try {
				return new Robot();
			} catch (AWTException e) {
				throw new RuntimeException("Failed to create instance of Robot");
			}
		}else{
			return robot;
		}
	}
	
	private Rectangle screenRectangle(){
		if(screenRectangle == null){
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			screenRectangle = new Rectangle(screenSize);
		}
		return screenRectangle;
	}
	
	
	@Override
	public Thread close() throws IOException {
		return new Thread(){
			public void run(){
				System.out.println("Close java connection");
			}
		};
	}

	@Override
	public Thread connect() throws Exception, PasswordRequiredException {
//		System.out.println("connecting...");
		fireRemoteServerEvent(new RemoteDesktopServerEvent(this, RemoteDesktopServerEvent.SERVER_UPDATE_EVENT));
		return new Thread(){
			public void run(){
				System.out.println("Establish java connection");
			}
		};
	}

	@Override
	public void destroy() {
		// do nothing
		
	}

	@Override
	public String getConnectString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getDefaultPort() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getDesktopHeight() {
		return (int) screenRectangle().getHeight();
	}

	@Override
	public String getDesktopName() {
		return "localhost desktop";
	}

	@Override
	public int getDesktopWidth() {
		return (int) screenRectangle().getWidth();
	}

	@Override
	public String getHost() {
		return "localhost";
	}

	@Override
	public BufferedImage getImage() {
		return robot().createScreenCapture(screenRectangle());
	}

	@Override
	public MouseEvent getLastMouseEvent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Preference> getLoginParamsSpecification() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPassword() {
		return null;
	}

	@Override
	public int getPort() {
		return 0;
	}

	@Override
	public String getProtocol() {
		return "JAVA";
	}

	@Override
	public String getUser() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasSufficientConnectInfo() {
		return true;
	}

	@Override
	public boolean isConnected() {
		return true;
	}

	@Override
	public boolean isConnectedTo(String connectString) {
		return false;
	}

	@Override
	public boolean isConnecting() {
		return false;
	}

	@Override
	public boolean isConsoleMode() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isLocalDisplay() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeClientListener(RemoteDesktopClientListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeServerListener(RemoteDesktopServerListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendClientCutText(String text) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setConsoleMode(boolean consoleMode) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLoginParams(Map<String, Object> params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void checkDependencies(PluginManager manager)
			throws DependencyMissingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getCode() {
		return "JAVA";
	}

	@Override
	public Date getDate() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDisplayName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class getImplementedInterface() {
		return RemoteDesktopClient.class;
	}

	@Override
	public int[] getLowestSupportedVersion() {
		return Utils.getVersion();
	}

	@Override
	public String getMessageAfterInstall() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMessageBeforeInstall() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSupportContact() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getUniqueId() {
		return "Sybase_JAVA_Client";
	}

	@Override
	public String getVendorHomePage() {
		return "http://www.sybase.com";
	}

	@Override
	public String getVendorName() {
		return "Sybase";
	}

	@Override
	public int[] getVersion() {
		return Utils.getVersion();
	}

	@Override
	public boolean requiresRestart() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public synchronized void addServerListener(RemoteDesktopServerListener listener) {
//		System.out.println(listener.toString());
		if(listener instanceof DesktopViewer){
			//block DesktopViewer registration
		}else{
			super.addServerListener(listener);
		}
	}

//	public static void main(String[] args) throws AWTException {
//		Robot rob = new Robot();
//		rob.keyPress("");
//	}
	
    public void sendPointerEvent(MouseEvent evt) throws IOException {
        System.out.println("sendPointerEvent");
    }
    
    public void sendPointerEvent(MouseEvent evt, boolean sendModifiers) throws IOException {
//    	System.out.println(evt.toString());
//    	System.out.print("Button="+evt.getButton());
//    	System.out.println(" Type="+evt.getID());
//    	System.out.println("click_type="+evt.MOUSE_CLICKED);
    	int eventType = evt.getID();
//    	System.out.println(evt.getID());
    	switch(eventType){
	    	case MouseEvent.MOUSE_MOVED:
//	    		System.out.println(evt.getX()+","+evt.getY());
	    		robot().mouseMove(evt.getX(), evt.getY());
	    		break;
	    	case MouseEvent.MOUSE_PRESSED:
	    		System.out.println(evt.getButton());
	    		if(evt.getButton()==MouseEvent.BUTTON1){
	    			robot().mousePress(InputEvent.BUTTON1_MASK);
	    		}
	    		if(evt.getButton()==MouseEvent.BUTTON2){
	    			robot().mousePress(InputEvent.BUTTON2_MASK);
	    		}
	    		if(evt.getButton()==MouseEvent.BUTTON3){
	    			robot().mousePress(InputEvent.BUTTON3_MASK);
	    		}
	    		break;
	    	case MouseEvent.MOUSE_RELEASED:
	    		if(evt.getButton()==MouseEvent.BUTTON1){
	    			robot().mouseRelease(InputEvent.BUTTON1_MASK);
	    		}
	    		if(evt.getButton()==MouseEvent.BUTTON2){
	    			robot().mouseRelease(InputEvent.BUTTON2_MASK);
	    		}
	    		if(evt.getButton()==MouseEvent.BUTTON3){
	    			robot().mouseRelease(InputEvent.BUTTON3_MASK);
	    		}
	    		break;
	    	default:
	    		break;	
    	}
    }

	@Override
	public void sendFramebufferUpdateRequest(Rectangle rect, boolean incremental)
			throws IOException {
		System.out.println("sendFramebufferUpdateRequest");
		
	}

	@Override
	public void sendKeyEvent(KeyEvent evt) throws IOException {
		int type = evt.getID();
		int offset = 32;
		int code = (int)evt.getKeyChar();
		char c = evt.getKeyChar();
		if(type == KeyEvent.KEY_PRESSED){
//			System.out.println(evt.getKeyChar()+":"+(int)evt.getKeyChar());
			if(evt.isShiftDown()){
				robot().keyPress(KeyEvent.VK_SHIFT);
			}
			if(isLowerCaseChar(c)){
				robot().keyPress(code-offset);
			}else{
				robot().keyPress(code);
			}
		}
		if(type == KeyEvent.KEY_RELEASED){
			if(isLowerCaseChar(c)){
				robot().keyRelease(code-offset);
			}else{
				robot().keyRelease(code);
			}
			if(evt.isShiftDown()){
				robot().keyRelease(KeyEvent.VK_SHIFT);
			}
		}
	}
	
	private boolean isUpperCaseChar(char c){
		int i = (int)c;
		if(i>=65 && i<=90){
			return true;
		}else{
			return false;
		}
	}
	
	private boolean isLowerCaseChar(char c){
		int i = (int)c;
		return (i>=97 && i<=122) ? true: false;
	}

}
