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

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import java.util.HashMap;
import java.util.Map;


//https://www.pyimagesearch.com/2015/09/07/blur-detection-with-opencv/
//https://www.researchgate.net/publication/3887632_Diatom_autofocusing_in_brightfield_microscopy_A_comparative_study
//https://stackoverflow.com/questions/7765810/is-there-a-way-to-detect-if-an-image-is-blurry
public final class Main{

	private static final EventListener eventListener = new EventLogger();


	private Main(){}

	public static void main(final String[] args){
		//load the native library
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		final String[] imageNames = (args.length > 0? args: new String[]{});

		//load images
		final Map<String, Mat> sources = new HashMap<>();
		for(final String imageName : imageNames)
			sources.put(imageName, Imgcodecs.imread(imageName, Imgcodecs.IMREAD_COLOR));

		//check if images are loaded fine
		for(final Map.Entry<String, Mat> element : sources.entrySet())
			if(element.getValue().empty()){
				eventListener.failedLoadingImage(element.getKey());
				System.exit(-1);
			}

		for(final Mat source : sources.values()){
			//reduce noise by blurring with a Gaussian filter (kernel size = 3)
			Imgproc.GaussianBlur(source, source, new Size(3, 3), 0, 0, Core.BORDER_DEFAULT);

			//convert the image to grayscale
			final Mat sourceGrayscaled = new Mat();
			Imgproc.cvtColor(source, sourceGrayscaled, Imgproc.COLOR_RGB2GRAY);

			//apply laplacian
			final Mat destination = new Mat();
			Imgproc.Laplacian(sourceGrayscaled, destination, CvType.CV_16S, 3, 1, 0, Core.BORDER_DEFAULT);

			//converting back to CV_8U
			final Mat destinationAbsolute = new Mat();
			Core.convertScaleAbs(destination, destinationAbsolute);
		}
		//TODO
	}

}
