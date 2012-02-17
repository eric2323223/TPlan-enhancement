package com.sybase.robot.scripting.commands.impl;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public class ImageUtil {
	static public String imageTypeName(BufferedImage img) {
		switch (img.getType()) {
		case BufferedImage.TYPE_3BYTE_BGR:
			return "TYPE_3BYTE_BGR";
		case BufferedImage.TYPE_4BYTE_ABGR:
			return "TYPE_4BYTE_ABGR";
		case BufferedImage.TYPE_4BYTE_ABGR_PRE:
			return "TYPE_4BYTE_ABGR_PRE";
		case BufferedImage.TYPE_BYTE_BINARY:
			return "TYPE_BYTE_BINARY";
		case BufferedImage.TYPE_BYTE_GRAY:
			return "TYPE_BYTE_GRAY";
		case BufferedImage.TYPE_BYTE_INDEXED:
			return "TYPE_BYTE_INDEXED";
		case BufferedImage.TYPE_CUSTOM:
			return "TYPE_CUSTOM";
		case BufferedImage.TYPE_INT_ARGB:
			return "TYPE_INT_ARGB";
		case BufferedImage.TYPE_INT_ARGB_PRE:
			return "TYPE_INT_ARGB_PRE";
		case BufferedImage.TYPE_INT_BGR:
			return "TYPE_INT_BGR";
		case BufferedImage.TYPE_INT_RGB:
			return "TYPE_INT_RGB";
		case BufferedImage.TYPE_USHORT_555_RGB:
			return "TYPE_USHORT_555_RGB";
		case BufferedImage.TYPE_USHORT_565_RGB:
			return "TYPE_USHORT_565_RGB";
		case BufferedImage.TYPE_USHORT_GRAY:
			return "TYPE_USHORT_GRAY";
		}
		return "unknown image type #" + img.getType();
	}

	static public int nrChannels(BufferedImage img) {
		switch (img.getType()) {
		case BufferedImage.TYPE_3BYTE_BGR:
			return 3;
		case BufferedImage.TYPE_4BYTE_ABGR:
			return 4;
		case BufferedImage.TYPE_BYTE_GRAY:
			return 1;
		case BufferedImage.TYPE_INT_BGR:
			return 3;
		case BufferedImage.TYPE_INT_ARGB:
			return 4;
		case BufferedImage.TYPE_INT_RGB:
			return 3;
		case BufferedImage.TYPE_CUSTOM:
			return 4;
		case BufferedImage.TYPE_4BYTE_ABGR_PRE:
			return 4;
		case BufferedImage.TYPE_INT_ARGB_PRE:
			return 4;
		case BufferedImage.TYPE_USHORT_555_RGB:
			return 3;
		case BufferedImage.TYPE_USHORT_565_RGB:
			return 3;
		case BufferedImage.TYPE_USHORT_GRAY:
			return 1;
		}
		return 0;
	}

	/**
	 * 
	 * returns one row (height == 1) of byte packed image data in BGR or AGBR
	 * form
	 * 
	 * @param img
	 * @param y
	 * @param w
	 * @param array
	 * @param temp
	 *            must be either null or a array with length of w*h
	 * @return
	 */
	public static byte[] getPixelsBGR(BufferedImage img, int y, int w,
			byte[] array, int[] temp) {
		final int x = 0;
		final int h = 1;

		assert array.length == temp.length * nrChannels(img);
		assert (temp.length == w);

		int imageType = img.getType();
		Raster raster;
		switch (imageType) {
		case BufferedImage.TYPE_3BYTE_BGR:
		case BufferedImage.TYPE_4BYTE_ABGR:
		case BufferedImage.TYPE_4BYTE_ABGR_PRE:
		case BufferedImage.TYPE_BYTE_GRAY:
			raster = img.getRaster();
			// int ttype= raster.getTransferType();
			raster.getDataElements(x, y, w, h, array);
			break;
		case BufferedImage.TYPE_INT_BGR:
			raster = img.getRaster();
			raster.getDataElements(x, y, w, h, temp);
			ints2bytes(temp, array, 0, 1, 2); // bgr --> bgr
			break;
		case BufferedImage.TYPE_INT_RGB:
			raster = img.getRaster();
			raster.getDataElements(x, y, w, h, temp);
			ints2bytes(temp, array, 2, 1, 0); // rgb --> bgr
			break;
		case BufferedImage.TYPE_INT_ARGB:
		case BufferedImage.TYPE_INT_ARGB_PRE:
			raster = img.getRaster();
			raster.getDataElements(x, y, w, h, temp);
			ints2bytes(temp, array, 2, 1, 0, 3); // argb --> abgr
			break;
		case BufferedImage.TYPE_CUSTOM: // TODO: works for my icon image loader,
										// but else ???
			img.getRGB(x, y, w, h, temp, 0, w);
			ints2bytes(temp, array, 2, 1, 0, 3); // argb --> abgr
			break;
		default:
			img.getRGB(x, y, w, h, temp, 0, w);
			ints2bytes(temp, array, 2, 1, 0); // rgb --> bgr
			break;
		}

		return array;
	}

	/**
	 * converts and copies byte packed BGR or ABGR into the img buffer, the img
	 * type may vary (e.g. RGB or BGR, int or byte packed) but the number of
	 * components (w/o alpha, w alpha, gray) must match
	 * 
	 * does not unmange the image for all (A)RGN and (A)BGR and gray imaged
	 * 
	 */
	public static void setBGRPixels(byte[] bgrPixels, BufferedImage img, int x,
			int y, int w, int h) {
		int imageType = img.getType();
		WritableRaster raster = img.getRaster();
		// int ttype= raster.getTransferType();
		if (imageType == BufferedImage.TYPE_3BYTE_BGR
				|| imageType == BufferedImage.TYPE_4BYTE_ABGR
				|| imageType == BufferedImage.TYPE_4BYTE_ABGR_PRE
				|| imageType == BufferedImage.TYPE_BYTE_GRAY) {
			raster.setDataElements(x, y, w, h, bgrPixels);
		} else {
			int[] pixels;
			if (imageType == BufferedImage.TYPE_INT_BGR) {
				pixels = bytes2int(bgrPixels, 2, 1, 0); // bgr --> bgr
			} else if (imageType == BufferedImage.TYPE_INT_ARGB
					|| imageType == BufferedImage.TYPE_INT_ARGB_PRE) {
				pixels = bytes2int(bgrPixels, 3, 0, 1, 2); // abgr --> argb
			} else {
				pixels = bytes2int(bgrPixels, 0, 1, 2); // bgr --> rgb
			}
			if (w == 0 || h == 0) {
				return;
			} else if (pixels.length < w * h) {
				throw new IllegalArgumentException(
						"pixels array must have a length" + " >= w*h");
			}
			if (imageType == BufferedImage.TYPE_INT_ARGB
					|| imageType == BufferedImage.TYPE_INT_RGB
					|| imageType == BufferedImage.TYPE_INT_ARGB_PRE
					|| imageType == BufferedImage.TYPE_INT_BGR) {
				raster.setDataElements(x, y, w, h, pixels);
			} else {
				// Unmanages the image
				img.setRGB(x, y, w, h, pixels, 0, w);
			}
		}
	}

	public static void ints2bytes(int[] in, byte[] out, int index1, int index2,
			int index3) {
		for (int i = 0; i < in.length; i++) {
			int index = i * 3;
			int value = in[i];
			out[index + index1] = (byte) value;
			value = value >> 8;
			out[index + index2] = (byte) value;
			value = value >> 8;
			out[index + index3] = (byte) value;
		}
	}

	public static void ints2bytes(int[] in, byte[] out, int index1, int index2,
			int index3, int index4) {
		for (int i = 0; i < in.length; i++) {
			int index = i * 4;
			int value = in[i];
			out[index + index1] = (byte) value;
			value = value >> 8;
			out[index + index2] = (byte) value;
			value = value >> 8;
			out[index + index3] = (byte) value;
			value = value >> 8;
			out[index + index4] = (byte) value;
		}
	}

	public static int[] bytes2int(byte[] in, int index1, int index2, int index3) {
		int[] out = new int[in.length / 3];
		for (int i = 0; i < out.length; i++) {
			int index = i * 3;
			int b1 = (in[index + index1] & 0xff) << 16;
			int b2 = (in[index + index2] & 0xff) << 8;
			int b3 = in[index + index3] & 0xff;
			out[i] = b1 | b2 | b3;
		}
		return out;
	}

	public static int[] bytes2int(byte[] in, int index1, int index2,
			int index3, int index4) {
		int[] out = new int[in.length / 4];
		for (int i = 0; i < out.length; i++) {
			int index = i * 4;
			int b1 = (in[index + index1] & 0xff) << 24;
			int b2 = (in[index + index2] & 0xff) << 16;
			int b3 = (in[index + index3] & 0xff) << 8;
			int b4 = in[index + index4] & 0xff;
			out[i] = b1 | b2 | b3 | b4;
		}
		return out;
	}

	public static BufferedImage convert(BufferedImage src, int bufImgType) {
		BufferedImage img = new BufferedImage(src.getWidth(), src.getHeight(),
				bufImgType);
		Graphics2D g2d = img.createGraphics();
		g2d.drawImage(src, 0, 0, null);
		g2d.dispose();
		return img;
	}

	/**
	 * Copy jpeg meta data (exif) from source to dest and save it to out.
	 * 
	 * @param source
	 * @param dest
	 * @return result
	 * @throws IOException
	 */
	public static byte[] copyJpegMetaData(byte[] source, byte[] dest)
			throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageOutputStream out = new MemoryCacheImageOutputStream(baos);
		copyJpegMetaData(new ByteArrayInputStream(source),
				new ByteArrayInputStream(dest), out);
		return baos.toByteArray();
	}

	/**
	 * Copy jpeg meta data (exif) from source to dest and save it to out
	 * 
	 * @param source
	 * @param dest
	 * @param out
	 * @throws IOException
	 */
	public static void copyJpegMetaData(InputStream source, InputStream dest,
			ImageOutputStream out) throws IOException {
		// Read meta data from src image
		Iterator iter = ImageIO.getImageReadersByFormatName("jpeg");
		ImageReader reader = (ImageReader) iter.next();
		ImageInputStream iis = new MemoryCacheImageInputStream(source);
		reader.setInput(iis);
		IIOMetadata metadata = reader.getImageMetadata(0);
		iis.close();
		// Read dest image
		ImageInputStream outIis = new MemoryCacheImageInputStream(dest);
		reader.setInput(outIis);
		IIOImage image = reader.readAll(0, null);
		image.setMetadata(metadata);
		outIis.close();
		// write dest image
		iter = ImageIO.getImageWritersByFormatName("jpeg");
		ImageWriter writer = (ImageWriter) iter.next();
		writer.setOutput(out);
		writer.write(image);
	}
	
	public static Image bufferedImageToImage(BufferedImage bufferedImage) {
	    return Toolkit.getDefaultToolkit().createImage(bufferedImage.getSource());
	}
	
	 public static BufferedImage imageToBufferedImage(Image im) {
		 if(im instanceof BufferedImage){
			 return (BufferedImage)im;
		 }else{
		     BufferedImage bi = new BufferedImage
		        (im.getWidth(null),im.getHeight(null),BufferedImage.TYPE_BYTE_GRAY);
		     Graphics bg = bi.getGraphics();
		     bg.drawImage(im, 0, 0, null);
		     bg.dispose();
		     return bi;
		 }
	  }
	 
	 public static byte[] bufferedImageToByteArray(BufferedImage img) throws IOException{
//		ByteArrayOutputStream bas = new ByteArrayOutputStream();
//		ImageIO.write(img,"png", bas);
//		byte[] result = bas.toByteArray();
//		System.out.println(result.length);
//		return result;
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write( img, "png", baos );
		baos.flush();
		byte[] imageInByte = baos.toByteArray();
		baos.close();
		return imageInByte;
	 }
	 
	 public static Image createImageFromArray(int[] pixels, int width, int height) {
         BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
         WritableRaster raster = (WritableRaster) image.getData();
         raster.setPixels(0,0,width,height,pixels);
         return image;
     }

	 public static void writeBufferedImageToFile(BufferedImage bi, File file){
		 try {
			ImageIO.write(bi, "png", file);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }

	public static BufferedImage cropEdge(BufferedImage processImage, int i) {
		int w = processImage.getWidth();
		int h = processImage.getHeight();
		return processImage.getSubimage(i, i, w-i-i, h-i-i);
	}

	public static BufferedImage readImage(String string) throws IOException {
		// TODO Auto-generated method stub
		return ImageIO.read(new File(string));
	}

	public static int getImageWidth(File imageFile) {
		try {
			BufferedImage bi = ImageIO.read(imageFile);
			return bi.getWidth();
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}

	public static int getImageHeight(File imageFile) {
		try {
			BufferedImage bi = ImageIO.read(imageFile);
			return bi.getHeight();
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public static int[] loadImage(BufferedImage img, Rectangle rect){
		int width = rect.width;
		int height = rect.height;
		int ai[]; // = new int[width * height];

		if (img instanceof BufferedImage) {
			BufferedImage bi = (BufferedImage) img;
			ai = ((BufferedImage) img).getRGB(rect.x, rect.y, width, height,
					(int[]) null, 0, width);

		} else {
			ai = new int[width * height];
			PixelGrabber pixelgrabber = new PixelGrabber(img, rect.x, rect.y, width,
					height, ai, 0, width);
			try {
				pixelgrabber.grabPixels();
			} catch (InterruptedException interruptedexception) {
				interruptedexception.printStackTrace();
			}
		}
		return ai;
	}
	
	public static void drawRectangle(final BufferedImage img, final Point point, final Rectangle rect, File file){
		int[] image = loadImage(img, new Rectangle(0,0,img.getWidth(), img.getHeight()));
		BufferedImage bi = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_BGR);
		for(int i=0;i<image.length;i++){
//			System.out.print(i+",");
			int px = i%img.getWidth();
			int py = i/img.getWidth();
			int x = point.x;
			int y = point.y;
			if((py == y && px >= x && px < x+rect.width) ||
					(py == y+rect.height && px >= x && px < x+rect.width)||
					(px == x && py>=y && py<y+rect.height) ||
					(px == x+rect.width && py >= y && py< y+rect.height)){
				int rgb = 0xFF00FF00;
				bi.setRGB(px, py, rgb);
			}else{
//				System.out.println(px+","+py+"="+img.getRGB(px,py));
				bi.setRGB(px, py, img.getRGB(px, py));
			}
		}
		writeBufferedImageToFile(bi, file);
	}

	public static BufferedImage edgefy(Image img, File file, int crop) {
//		System.out.println("Processing file "+file.getAbsolutePath());
		CannyEdgeDetection detector = new CannyEdgeDetection();
//		BufferedImage grayImg = ImageUtil.convert(ImageUtil.imageToBufferedImage(img), BufferedImage.TYPE_BYTE_GRAY);
//		BufferedImage edgeImg = detector.processImage(grayImg);
		BufferedImage edgeImg = detector.processImage(ImageUtil.imageToBufferedImage(img));
    	edgeImg = ImageUtil.cropEdge(edgeImg, crop);
		if(file != null){
    		ImageUtil.writeBufferedImageToFile(edgeImg, file);
    	}
		return edgeImg;
	}
}
