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

import java.awt.image.Kernel;


public enum BlurKernel{
	LAPLACE(3, 3, new float[]{
		0.f, -1.f, 0.f,
		-1.f, 4.f, -1.f,
		0.f, -1.f, 0.f
	}, KernelNorm.NONE),
	LAPLACIAN_GRADIENT(3, 3, new float[]{
		1.f, 4.f, 1.f,
		4.f, -20.f, 4.f,
		1.f, 4.f, 1.f
	}, KernelNorm.NONE),
	//https://en.wikipedia.org/wiki/Sobel_operator
	//second to best?
	SOBEL_TENENGRAD(3, 3, new float[]{
		1.f, 0.f, -1.f,
		2.f, 0.f, -2.f,
		1.f, 0.f, -1.f
	}, KernelNorm.EUCLIDEAN),
	//https://en.wikipedia.org/wiki/Sobel_operator
	SOBEL_FIELDMANN(3, 3, new float[]{
		3.f, 0.f, -3.f,
		10.f, 0.f, -10.f,
		3.f, 0.f, -3.f
	}, KernelNorm.EUCLIDEAN),
	//https://en.wikipedia.org/wiki/Sobel_operator
	SCHARR(3, 3, new float[]{
		47.f, 0.f, -47.f,
		162.f, 0.f, -162.f,
		47.f, 0.f, -47.f
	}, KernelNorm.EUCLIDEAN);


	private final Kernel kernel;
	private Kernel kernelTransposed;
	private final KernelNorm norm;


	BlurKernel(final int width, final int height, final float[] kernel, final KernelNorm norm){
		this.kernel = new Kernel(width, height, kernel);
		this.norm = norm;
	}

	Kernel getKernel(){
		return kernel;
	}

	Kernel getKernelTransposed(){
		if(kernelTransposed == null)
			kernelTransposed = transpose(kernel);

		return kernelTransposed;
	}

	KernelNorm getNorm(){
		return norm;
	}

	private Kernel transpose(final Kernel kernel){
		final int width = kernel.getWidth();
		final int height = kernel.getHeight();
		final float[] array = kernel.getKernelData(null);
		final float[] result = new float[array.length];
		for(int i = 0; i < width; i ++)
			for(int j = 0; j < height; j ++)
				result[j * width + i] = array[i * height + j];
		return new Kernel(height, width, result);
	}

}
