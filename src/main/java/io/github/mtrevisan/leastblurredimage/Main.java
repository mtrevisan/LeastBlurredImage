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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


//https://www.pyimagesearch.com/2015/09/07/blur-detection-with-opencv/
//https://www.researchgate.net/publication/3887632_Diatom_autofocusing_in_brightfield_microscopy_A_comparative_study
//https://stackoverflow.com/questions/7765810/is-there-a-way-to-detect-if-an-image-is-blurry
//https://www.programmersought.com/article/6056166465/
public final class Main{

	private static final ImageService IMAGE_SERVICE = ImageService.getInstance();

	//Laplace
	private static final int[][] KERNEL_LAPLACE = {
		{0, -1, 0},
		{-1, 4, -1},
		{0, -1, 0}
	};

	//Laplacian gradient
	private static final int[][] KERNEL_LAPLACIAN_GRADIENT = {
		{1, 4, 1},
		{4, -20, 4},
		{1, 4, 1}
	};

	//Sobel/Tenengrad
	//https://en.wikipedia.org/wiki/Sobel_operator
	private static final int[][] KERNEL_SOBEL_HORIZONTAL = {
		{1, 0, -1},
		{2, 0, -2},
		{1, 0, -1}
	};
	private static final int[][] KERNEL_SOBEL_VERTICAL = {
		{1, 2, 1},
		{0, 0, 0},
		{-1, -2, -1}
	};

	//Sobel-Fieldmann
	//https://en.wikipedia.org/wiki/Sobel_operator
	private static final int[][] KERNEL_SOBEL_FIELDMANN_HORIZONTAL = {
		{3, 0, -3},
		{10, 0, -10},
		{3, 0, -3}
	};
	private static final int[][] KERNEL_SOBEL_FIELDMANN_VERTICAL = {
		{3, 10, 3},
		{0, 0, 0},
		{-3, -10, -3}
	};

	//Scharr
	//https://en.wikipedia.org/wiki/Sobel_operator
	private static final int[][] KERNEL_SCHARR_HORIZONTAL = {
		{47, 0, -47},
		{162, 0, -162},
		{47, 0, -47}
	};
	private static final int[][] KERNEL_SCHARR_VERTICAL = {
		{47, 162, 47},
		{0, 0, 0},
		{-47, -162, -47}
	};

	//Gradient (mean norm) / Brenner (euclidean norm)
	private static final int[][] KERNEL_GRADIENT_HORIZONTAL = {
		{-1},
		{1}
	};
	private static final int[][] KERNEL_GRADIENT_VERTICAL = {
		{-1, 1}
	};


	private Main(){}

	public static void main(final String[] args) throws IOException{
		System.out.println("loading " + args.length + " images");
		final Map<String, BufferedImage> sources = loadImages(args.length > 0? args: new String[]{});

		String leastBlurredImageName = null;
		double maximumVariance = 0.;
		for(final Map.Entry<String, BufferedImage> element : sources.entrySet()){
			final BufferedImage image = element.getValue();
			final int width = image.getWidth(null);
			final int height = image.getHeight(null);
			final int[] pixels = IMAGE_SERVICE.getPixels(image, width, height);
			final int imageType = image.getType();
//			final int[] convolutedPixels = IMAGE_SERVICE.convolute(pixels, width, height, imageType, KERNEL_LAPLACE);
//			final int[] convolutedPixels = IMAGE_SERVICE.convolute(pixels, width, height, imageType, KERNEL_LAPLACIAN_GRADIENT);
//			final int[] convolutedPixels = IMAGE_SERVICE.convoluteEuclidean(pixels, width, height, imageType, KERNEL_SOBEL_HORIZONTAL,
//				KERNEL_SOBEL_VERTICAL);
//			final int[] convolutedPixels = IMAGE_SERVICE.convoluteEuclidean(pixels, width, height, imageType,
//				KERNEL_SOBEL_FIELDMANN_HORIZONTAL, KERNEL_SOBEL_FIELDMANN_VERTICAL);
//			final int[] convolutedPixels = IMAGE_SERVICE.convoluteEuclidean(pixels, width, height, imageType, KERNEL_SCHARR_HORIZONTAL,
//				KERNEL_SCHARR_VERTICAL);
//			final int[] convolutedPixels = IMAGE_SERVICE.convoluteMean(pixels, width, height, imageType, KERNEL_GRADIENT_HORIZONTAL,
//				KERNEL_GRADIENT_VERTICAL);
			//best?
			final int[] convolutedPixels = IMAGE_SERVICE.convoluteEuclidean(pixels, width, height, imageType, KERNEL_GRADIENT_HORIZONTAL,
				KERNEL_GRADIENT_VERTICAL);

			final double variance = IMAGE_SERVICE.calculateVariance(convolutedPixels);

			System.out.println(element.getKey() + " -> " + variance);

			if(variance > maximumVariance){
				maximumVariance = variance;
				leastBlurredImageName = element.getKey();
			}
		}
		System.out.println("least blurred is " + leastBlurredImageName);
	}

	private static Map<String, BufferedImage> loadImages(final String[] imageNames) throws IOException{
		final Map<String, BufferedImage> sources = new HashMap<>(imageNames.length);
		for(final String imageName : imageNames){
			final BufferedImage image = IMAGE_SERVICE.readImage(imageName);

			//put grayscaled image into the map
			final int width = image.getWidth(null);
			final int height = image.getHeight(null);
			sources.put(imageName, IMAGE_SERVICE.grayscaledImage(image, width, height));

			System.out.print(".");
		}
		System.out.println();
		return sources;
	}

}
