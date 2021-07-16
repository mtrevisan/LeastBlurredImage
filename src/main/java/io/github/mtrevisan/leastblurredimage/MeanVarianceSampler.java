package io.github.mtrevisan.leastblurredimage;


/**
 * Utility to incrementally calculate mean, variance and standard deviation of a sample.
 * <p>
 * The implementation is based on Welford’s Algorithm given in Knuth Vol 2, p 232.
 * </p>
 * <p>
 * This class is <i>NOT</i> thread safe.
 * </p>
 */
class MeanVarianceSampler{

	private long count;
	private double mean;
	private double s;


	/**
	 * Resets this sampler to its initial state.
	 */
	public void reset(){
		count = 0;
		mean = 0.;
		s = 0.;
	}

	/**
	 * Adds the value {@code x} to the sample. The sample count is incremented by one by this operation,
	 *
	 * @param x the value to add
	 */
	public void add(final double x){
		count ++;
		final double delta = x - mean;
		mean += delta / count;
		s += delta * (x - mean);
	}

	/**
	 * Returns the variance of the sample using the {@code (n)} method. Returns NaN if count is 0.
	 * <p>
	 * The method is based on calculated values and returns almost immediately (involves a simple division).
	 * </p>
	 *
	 * @return the biased variance of the sample
	 */
	public double getVariance(){
		return s / count;
	}

	/**
	 * Returns the variance of the sample using the {@code (n-1)} method. Returns 0 if the sample count is zero, and Inf
	 * or NaN if count is 1.
	 * <p>
	 * The method is based on calculated values and returns almost immediately (involves a simple division).
	 * </p>
	 *
	 * @return the variance of the sample (bias corrected)
	 */
	public double getVarianceUnbiased(){
		return (count > 0? s / (count - 1): 0.);
	}

	/**
	 * Returns the standard deviation of the sample using the {@code (n)} method.
	 *
	 * @return the biased standard deviation of the sample
	 */
	public double getStdDev(){
		return Math.sqrt(getVariance());
	}

	/**
	 * Returns the standard deviation of the sample using the {@code (n-1)} method.
	 *
	 * @return the standard deviation of the sample (bias corrected)
	 */
	public double getStdDevUnbiased(){
		return Math.sqrt(getVarianceUnbiased());
	}

	/**
	 * Returns the number of values in the sample.
	 *
	 * @return the number of values in the sample
	 */
	public long getCount(){
		return count;
	}

}
