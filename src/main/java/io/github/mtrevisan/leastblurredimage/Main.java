/**
 * Copyright (c) 2020-2021 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.leastblurredimage;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;


//https://www.pyimagesearch.com/2015/09/07/blur-detection-with-opencv/
//https://www.researchgate.net/publication/3887632_Diatom_autofocusing_in_brightfield_microscopy_A_comparative_study
//https://stackoverflow.com/questions/7765810/is-there-a-way-to-detect-if-an-image-is-blurry
public final class Main{

	private static final EventListener eventListener = EventLogger.getInstance();

	static{
		try{
			final String osName = System.getProperty("os.name");
			InputStream in = null;
			File fileOut = null;
			if(osName.startsWith("Windows")){
				final int bitness = Integer.parseInt(System.getProperty("sun.arch.data.model"));
				if(bitness == 64){
					in = Main.class.getResourceAsStream("/opencv/x64/opencv_java452.dll");
					fileOut = File.createTempFile("lib", ".dll");
				}
				else{
					in = Main.class.getResourceAsStream("/opencv/x86/opencv_java452.dll");
					fileOut = File.createTempFile("lib", ".dll");
				}
			}
			else if(osName.equals("Mac OS X")){
				in = Main.class.getResourceAsStream("/opencv/mac/libopencv_java452.dylib");
				fileOut = File.createTempFile("lib", ".dylib");
			}

			final byte[] buffer = new byte[in.available()];
			in.read(buffer);

			final OutputStream os = new FileOutputStream(fileOut);
			os.write(buffer);
			in.close();
			os.close();
			System.load(fileOut.toString());


			//load the native library
//			System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		}
		catch(final Exception e){
			eventListener.cannotLoadLibrary(e);
		}
	}

	private static final Mat KERNEL = new Mat(9, 9, CvType.CV_32F){
		{
			put(0, 0, 0);
			put(0, 1, -1);
			put(0, 2, 0);

			put(1, 0, -1);
			put(1, 1, 4);
			put(1, 2, -1);

			put(2, 0, 0);
			put(2, 1, -1);
			put(2, 2, 0);
		}
	};


	private Main(){}

	public static void main(final String[] args){
		final Map<String, Mat> sources = loadImages(args.length > 0? args: new String[]{});

		String leastBlurredImageName = null;
		double minimumVariance = Double.MAX_VALUE;
		for(final Map.Entry<String, Mat> element : sources.entrySet()){
			final Mat destination = applyLaplacian(element.getValue());

			//take the variance (i.e. standard deviation squared) of the response
			final double variance = -Core.norm(destination);

			if(variance < minimumVariance){
				minimumVariance = variance;
				leastBlurredImageName = element.getKey();
			}
		}
		System.out.println(leastBlurredImageName);
	}

	private static Map<String, Mat> loadImages(final String[] imageNames){
		final Map<String, Mat> sources = new HashMap<>(imageNames.length);
		for(final String imageName : imageNames)
			sources.put(imageName, Imgcodecs.imread(imageName, Imgcodecs.IMREAD_GRAYSCALE));

		//check if images are loaded fine
		for(final Map.Entry<String, Mat> element : sources.entrySet())
			if(element.getValue().empty()){
				eventListener.failedLoadingImage(element.getKey());
				System.exit(-1);
			}

		return sources;
	}

	private static Mat applyLaplacian(final Mat source){
		final Mat destination = new Mat(source.rows(), source.cols(), source.type());
		Imgproc.filter2D(source, destination, -1, KERNEL);
		return destination;
	}

}
