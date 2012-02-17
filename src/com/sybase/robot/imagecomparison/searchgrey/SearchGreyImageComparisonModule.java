package com.sybase.robot.imagecomparison.searchgrey;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import com.sybase.robot.scripting.commands.impl.ImageUtil;
import com.tplan.robot.imagecomparison.search.SearchImageComparisonModule;
import com.tplan.robot.scripting.ScriptingContext;

public class SearchGreyImageComparisonModule extends SearchImageComparisonModule{
	
	public void setBaseImage(Image img){
		BufferedImage bi = ImageUtil.convert(ImageUtil.imageToBufferedImage(img), BufferedImage.TYPE_BYTE_GRAY);
		super.setBaseImage(bi);
		
	}

	@Override
	public float compareToBaseImage(Image desktopImage, Rectangle area,
			String methodParams, ScriptingContext repository, float passRate) {
		BufferedImage bi = ImageUtil.convert(ImageUtil.imageToBufferedImage(desktopImage), BufferedImage.TYPE_BYTE_GRAY);
		return super.compareToBaseImage(bi, area, methodParams, repository,
				passRate);
	}
	
	public String getCode(){
//		System.out.println("plugin activated");
		return "searchgrey";
	}

}
