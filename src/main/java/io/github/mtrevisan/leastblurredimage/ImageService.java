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

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;


final class ImageService{

	private static class SingletonHelper{
		private static final ImageService INSTANCE = new ImageService();
	}


	static ImageService getInstance(){
		return SingletonHelper.INSTANCE;
	}

	private ImageService(){}

	BufferedImage readImage(final String file) throws IOException{
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
		throw new IllegalArgumentException("No reader for " + file);
	}

	BufferedImage grayscaledImage(final BufferedImage colorImage){
		final BufferedImage image = new BufferedImage(colorImage.getWidth(), colorImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		final Graphics g = image.getGraphics();
		g.drawImage(colorImage, 0, 0, null);
		g.dispose();
		return image;
	}

	int[] applyLaplacian(final BufferedImage image, final int[][] kernel){
		final int width = image.getWidth(null);
		final int height = image.getHeight(null);
		final int[] pixels = getPixels(image, width, height);
		final int kernelWidth = kernel.length;
		final int kernelHeight = kernel[0].length;

		//histogram equalization
		final int imageType = image.getType();
		final int[] equalizedPixels = histogramEqualized(pixels, imageType);

		//apply the convolution
		final int[] destinationPixels = new int[pixels.length - ((width + height) << 1)];
		//NOTE: ignore first and last rows to avoid going out of range
		for(int i = 1; i < width - 1; i ++)
			for(int j = 1; j < height - 1; j ++){
				double value = 0.;
				for(int u = -(kernelWidth - 1) >> 1; u <= (kernelWidth - 1) >> 1; u ++){
					for(int v = -(kernelHeight - 1) >> 1; v <= (kernelHeight - 1) >> 1; v ++)
						value += equalizedPixels[(i + u) * width + (j + v)] * kernel[u + ((kernelWidth - 1) >> 1)][v + ((kernelHeight - 1) >> 1)];
				}

				destinationPixels[(i - 1) * width + (j - 1)] = (int)value;
			}
		return destinationPixels;
	}

	private int[] histogramEqualized(final int[] pixels, final int imageType){
		final int length = pixels.length;
		final int[] equalizedPixels = new int[length];
		final int[] histogram = getHistogram(pixels, imageType);
		final double tmp = 255. / length;
		for(int i = 0; i < length; i ++){
			int sum = 0;
			for(int k = 0; k < pixels[i]; k ++)
				sum += histogram[k];

			equalizedPixels[i] = (int)(sum * tmp);
		}
		return equalizedPixels;
	}

	/**
	 * Calculates and returns the histogram for the image.
	 * The histogram is represented by an int array of 256 elements. Each element gives the number
	 * of pixels in the image of the value equal to the index of the element.
	 */
	private int[] getHistogram(final int[] pixels, final int imageType){
		final int[] histogram = (imageType == 10? new int[256]: new int[65536]);
		final int length = pixels.length;
		for(int i = 0; i < length; i ++)
			histogram[pixels[i]] ++;
		return histogram;
	}

	private int[] getPixels(final BufferedImage image, final int width, final int height){
		final int[] pixels = new int[width * height];
		image.getRaster()
			.getPixels(0, 0, width, height, pixels);
		return pixels;
	}

	double calculateVariance(final int[] pixels){
		//take the variance (i.e. standard deviation squared) of the response
		final double mean = calculateMean(pixels);

		double variance = 0.;
		final int length = pixels.length;
		for(int i = 0; i < length; i ++){
			final double tmp = pixels[i] - mean;
			variance += tmp * tmp;
		}
		variance /= length - 1.;
		return variance;
	}

	private double calculateMean(final int[] pixels){
		double mean = 0.;
		final int length = pixels.length;
		for(int i = 0; i < length; i ++)
			mean += pixels[i];
		mean /= length;
		return mean;
	}

}
