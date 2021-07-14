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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;


//https://www.pyimagesearch.com/2015/09/07/blur-detection-with-opencv/
//https://www.researchgate.net/publication/3887632_Diatom_autofocusing_in_brightfield_microscopy_A_comparative_study
//https://stackoverflow.com/questions/7765810/is-there-a-way-to-detect-if-an-image-is-blurry
public final class Main{

	private static final EventListener eventListener = EventLogger.getInstance();

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
		final Map<String, BufferedImage> sources = loadImages(args.length > 0? args: new String[]{});
		System.out.println("loaded " + args.length + " images");

		String leastBlurredImageName = null;
		double minimumVariance = Double.MAX_VALUE;
		for(final Map.Entry<String, BufferedImage> element : sources.entrySet()){
			final BufferedImage destination = applyLaplacian(element.getValue());

			//take the variance (i.e. standard deviation squared) of the response
			final double variance = -Core.norm(destination);

			System.out.println(element.getKey() + " / " + -variance);

			if(variance < minimumVariance){
				minimumVariance = variance;
				leastBlurredImageName = element.getKey();
			}
		}
		System.out.println("least blurred is " + leastBlurredImageName);
	}

	private static Map<String, BufferedImage> loadImages(final String[] imageNames){
		final Map<String, BufferedImage> sources = new HashMap<>(imageNames.length);
		for(final String imageName : imageNames)
			sources.put(imageName, Imgcodecs.imread(imageName, Imgcodecs.IMREAD_GRAYSCALE));

		//check if images are loaded fine
		for(final Map.Entry<String, BufferedImage> element : sources.entrySet())
			if(element.getValue().empty()){
				eventListener.failedLoadingImage(element.getKey());
				System.exit(-1);
			}

		return sources;
	}

	public static BufferedImage readImage(final String file) throws IOException{
		final File f = new File(file);
		if(!f.exists())
			throw new IllegalArgumentException("File `" + file + "` does not exists.");

		try(final ImageInputStream input = ImageIO.createImageInputStream(f)){
			final Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
			if(readers.hasNext()){
				final ImageReader reader = readers.next();
				try{
					reader.setInput(input);
					return reader.read(0);
				}
				finally{
					reader.dispose();
				}
			}
		}
	}

	private static Mat applyLaplacian(final Mat source){
		//histogram equalization
		final Mat equalizedSource = new Mat(source.rows(), source.cols(), source.type());
		Imgproc.equalizeHist(source, equalizedSource);

		final Mat destination = new Mat(source.rows(), source.cols(), source.type());
		Imgproc.filter2D(equalizedSource, destination, -1, KERNEL);

		return destination;
	}

}
