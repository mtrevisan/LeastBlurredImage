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
public final class Main{

	private static final ImageService IMAGE_SERVICE = ImageService.getInstance();

	private static final int[][] KERNEL = {
		{0, -1, 0},
		{-1, 4, -1},
		{0, -1, 0}
	};


	private Main(){}

	public static void main(final String[] args) throws IOException{
		final Map<String, BufferedImage> sources = loadImages(args.length > 0? args: new String[]{});
		System.out.println("loaded " + args.length + " images");

		String leastBlurredImageName = null;
		double maximumVariance = 0.;
		for(final Map.Entry<String, BufferedImage> element : sources.entrySet()){
			final BufferedImage image = element.getValue();
			final int[] convolutedPixels = IMAGE_SERVICE.applyLaplacian(image, KERNEL);

			final double variance = IMAGE_SERVICE.calculateVariance(convolutedPixels);

			System.out.println(element.getKey() + " -> " + ((int)(variance * 10) / 10));

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
			sources.put(imageName, IMAGE_SERVICE.grayscaledImage(image));
		}
		return sources;
	}

}
