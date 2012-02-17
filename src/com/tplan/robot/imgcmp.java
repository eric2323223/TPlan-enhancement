/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.tplan.robot;

/**
 * Generated on Thu Feb 04 20:07:34 CET 2010
 * T-Plan Robot v2.0.3Beta (Build No. 2.0.3Beta-20100128.1)
 * Default Java Converter version 2.0.0
 */

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.AutomatedRunnable;
import com.tplan.robot.scripting.DefaultJavaTestScript;
import com.tplan.robot.scripting.JavaTestScript;
import com.tplan.robot.imagecomparison.*;
import java.awt.*;
import javax.imageio.*;
import java.io.File;
import java.io.IOException;

public class imgcmp extends DefaultJavaTestScript implements JavaTestScript {

   public void test() {
      try {
         ImageComparisonModule m = ImageComparisonModuleFactory.getInstance().getModule("search");
         connect("localhost:1", "welcome");
         compareTo(new Image[] { ImageIO.read(new File("/home/robert/tr.png")) }, m, 100.0f, null);
         System.out.println("result="+getContext().getExitCode());
      } catch (IOException ex) {
         ex.printStackTrace();
      }
   }

   public static void main(String args[]) {
      imgcmp test = new imgcmp();
      ApplicationSupport robot = new ApplicationSupport();
      AutomatedRunnable t = robot.createAutomatedRunnable(test, "javatest", args, System.out, false);
      new Thread(t).start();
   }
}