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


public enum Kernel{
	LAPLACE(new int[][]{
		{0, -1, 0},
		{-1, 4, -1},
		{0, -1, 0}
	}, KernelNorm.NONE),
	LAPLACIAN_GRADIENT(new int[][]{
		{1, 4, 1},
		{4, -20, 4},
		{1, 4, 1}
	}, KernelNorm.NONE),
	//https://en.wikipedia.org/wiki/Sobel_operator
	//second to best?
	SOBEL_TENENGRAD(new int[][]{
		{1, 0, -1},
		{2, 0, -2},
		{1, 0, -1}
	}, KernelNorm.EUCLIDEAN),
	//https://en.wikipedia.org/wiki/Sobel_operator
	SOBEL_FIELDMANN(new int[][]{
		{3, 0, -3},
		{10, 0, -10},
		{3, 0, -3}
	}, KernelNorm.EUCLIDEAN),
	//https://en.wikipedia.org/wiki/Sobel_operator
	SCHARR(new int[][]{
		{47, 0, -47},
		{162, 0, -162},
		{47, 0, -47}
	}, KernelNorm.EUCLIDEAN),
	GRADIENT(new int[][]{
		{-1},
		{1}
	}, KernelNorm.MEAN),
	//best?
	BRENNER(new int[][]{
		{-1},
		{1}
	}, KernelNorm.EUCLIDEAN);


	int [][] kernel;
	int [][] kernelVertical;
	KernelNorm norm;


	Kernel(final int[][] kernel, final KernelNorm norm){
		this.kernel = kernel;
		this.norm = norm;
		if(norm != KernelNorm.NONE)
			kernelVertical = transpose(kernel);
	}

	private int[][] transpose(final int[][] array){
		final int width = array.length;
		final int height = array[0].length;
		final int[][] result = new int[height][width];
		for(int i = 0; i < height; i ++)
			for(int j = i + 1; j < width; j ++)
				result[i][j] = array[j][i];
		return result;
	}

}
