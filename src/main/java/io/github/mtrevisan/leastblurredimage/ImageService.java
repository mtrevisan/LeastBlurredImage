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
import java.awt.image.ConvolveOp;
import java.awt.image.DataBufferByte;
import java.awt.image.Kernel;
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

	BufferedImage readImage(final File file){
		try(final ImageInputStream input = ImageIO.createImageInputStream(file)){
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
		catch(final IOException ignored){}
		return null;
	}

	BufferedImage grayscaled(final BufferedImage coloredImage){
		final int width = coloredImage.getWidth(null);
		final int height = coloredImage.getHeight(null);

		final BufferedImage grayscaledImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		final Graphics g = grayscaledImage.getGraphics();
		g.drawImage(coloredImage, 0, 0, null);
		g.dispose();
		return grayscaledImage;
	}

	/**
	 * NOTE: Assume the image is grayscaled.
	 */
	BufferedImage equalizeHistogram(final BufferedImage image){
		final int width = image.getWidth(null);
		final int height = image.getHeight(null);

		final int[] pixels = getPixels(image, width, height);
		final double[] histogram = getHistogram(pixels);

		final int length = pixels.length;
		calculateCumulativeDistributionFunction(histogram, length);

		for(int i = 0; i < length; i ++)
			pixels[i] = (int)histogram[pixels[i]];

		return createImage(pixels, width, height);
	}

	BufferedImage convolve(final BufferedImage image, final BlurKernel blurKernel){
		if(blurKernel.getNorm() == KernelNorm.NONE){
			final ConvolveOp operation = new ConvolveOp(blurKernel.getKernel());
			return operation.filter(image, null);
		}

		//aggregate convolutions:
		final Kernel kernelHorizontal = blurKernel.getKernel();
		ConvolveOp operation = new ConvolveOp(kernelHorizontal);
		final BufferedImage convolutionHorizontal = operation.filter(image, null);
		final int width = convolutionHorizontal.getWidth(null);
		final int height = convolutionHorizontal.getHeight(null);
		final int[] convolutedPixelsHorizontal = getPixels(convolutionHorizontal, width, height);

		final Kernel kernelVertical = blurKernel.getKernelTransposed();
		operation = new ConvolveOp(kernelVertical);
		final BufferedImage convolutionVertical = operation.filter(image, null);
		final int[] convolutedPixelsVertical = getPixels(convolutionVertical, width, height);

		final KernelNorm norm = blurKernel.getNorm();
		final int[] convolutedPixels = new int[convolutedPixelsHorizontal.length];
		final int length = convolutedPixels.length;
		for(int i = 0; i < length; i ++)
			convolutedPixels[i] = norm.compose(convolutedPixelsHorizontal[i], convolutedPixelsVertical[i]);
		return createImage(convolutedPixels, width, height);
	}

	private BufferedImage createImage(final int[] pixels, final int width, final int height){
		final BufferedImage equalizedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		final byte array[] = new byte[pixels.length];
		for(int i = 0; i < pixels.length; i ++)
			array[i] = (byte)pixels[i];
		final byte[] data = ((DataBufferByte)equalizedImage.getRaster().getDataBuffer()).getData();
		System.arraycopy(array, 0, data, 0, pixels.length);
		return equalizedImage;
	}

	private void calculateCumulativeDistributionFunction(final double[] histogram, final int pixelCount){
		final int histogramLength = histogram.length;
		for(int i = 1; i < histogramLength; i ++)
			histogram[i] = histogram[i - 1] + histogram[i];

		final double tmp = (histogramLength - 1.) / pixelCount;
		for(int i = 0; i < histogramLength; i ++)
			histogram[i] *= tmp;
	}

	/**
	 * Calculates and returns the histogram for the image.
	 * The histogram is represented by an int array of 256 elements. Each element gives the number
	 * of pixels in the image of the value equal to the index of the element.
	 */
	private double[] getHistogram(final int[] pixels){
		final double[] histogram = new double[1 << 8];
		final int length = pixels.length;
		for(int i = 0; i < length; i ++)
			histogram[pixels[i]] ++;
		return histogram;
	}

	double variance(final BufferedImage image){
		final int width = image.getWidth(null);
		final int height = image.getHeight(null);
		final int[] pixels = getPixels(image, width, height);

		final double mean = calculateMean(pixels);

		double variance = 0.;
		final int length = pixels.length;
		for(int i = 0; i < length; i ++){
			final double tmp = pixels[i] - mean;
			variance += tmp * tmp;
		}
		return variance / (length - 1);
	}

	private double calculateMean(final int[] pixels){
		double mean = 0.;
		final int length = pixels.length;
		for(int i = 0; i < length; i ++)
			mean += pixels[i];
		return mean / length;
	}

	private int[] getPixels(final BufferedImage image, final int width, final int height){
		final int[] pixels = new int[width * height];
		image.getRaster()
			.getPixels(0, 0, width, height, pixels);
		return pixels;
	}

}
