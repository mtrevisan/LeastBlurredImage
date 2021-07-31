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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;


//https://www.pyimagesearch.com/2015/09/07/blur-detection-with-opencv/
//https://www.researchgate.net/publication/3887632_Diatom_autofocusing_in_brightfield_microscopy_A_comparative_study
//https://stackoverflow.com/questions/7765810/is-there-a-way-to-detect-if-an-image-is-blurry
//https://www.programmersought.com/article/6056166465/
public final class Main{

	private static final int ERROR_GENERIC = 1;
	private static final int ERROR_INPUT_NOT_FOLDER = 2;
	private static final int ERROR_INVALID_KERNEL = 3;
	private static final int ERROR_CANNOT_MOVE = 4;

	private static final String PARAM_FOLDER = "folder";
	private static final String PARAM_KERNEL = "kernel";
	private static final String PARAM_OUTPUT = "output";

	private static final BlurKernel BLUR_KERNEL_DEFAULT = BlurKernel.SOBEL_FIELDMANN;


	private static final ImageService IMAGE_SERVICE = ImageService.getInstance();


	private Main(){}

	public static void main(final String[] args){
		final Options options = new Options();
		defineOptions(options);

		final CommandLineParser cmdLineParser = new DefaultParser();
		try{
			final CommandLine cmdLine = cmdLineParser.parse(options, args);
			final File inputFolder = extractParamFolder(cmdLine);
			final BlurKernel blurKernel = extractParamKernel(cmdLine);
			final File outputFolder = extractParamOutput(cmdLine);

			process(inputFolder, blurKernel, outputFolder);
		}
		catch(final ParseException e){
			System.out.println(e.getMessage());

			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("LeastBlurredImage [options]", options);

			System.exit(ERROR_GENERIC);
		}
	}

	private static void defineOptions(final Options options) throws IllegalArgumentException{
		Option opt = new Option("f", PARAM_FOLDER, true, "folder from which to read the images");
		opt.setRequired(true);
		options.addOption(opt);

		opt = new Option("k", PARAM_KERNEL, true, "kernel type, one of " + Arrays.asList(BlurKernel.values()));
		options.addOption(opt);

		opt = new Option("o", PARAM_OUTPUT, true, "output folder, where the best image is moved into");
		options.addOption(opt);
	}

	private static File extractParamFolder(final CommandLine cmdLine){
		final File folder = new File(cmdLine.getOptionValue(PARAM_FOLDER));
		if(!folder.isDirectory()){
			System.out.println("input `" + PARAM_FOLDER + "` is not a directory");

			System.exit(ERROR_INPUT_NOT_FOLDER);
		}
		return folder;
	}

	private static BlurKernel extractParamKernel(final CommandLine cmdLine){
		BlurKernel blurKernel = BLUR_KERNEL_DEFAULT;
		if(cmdLine.hasOption(PARAM_KERNEL)){
			try{
				blurKernel = BlurKernel.valueOf(cmdLine.getOptionValue(PARAM_KERNEL));
			}
			catch(final Exception ignored){
				System.out.println("invalid kernel type, should be one of " + Arrays.asList(BlurKernel.values()));

				System.exit(ERROR_INVALID_KERNEL);
			}
		}
		return blurKernel;
	}

	private static File extractParamOutput(final CommandLine cmdLine){
		File output = null;
		if(cmdLine.hasOption(PARAM_OUTPUT))
			output = new File(cmdLine.getOptionValue(PARAM_OUTPUT));
		return output;
	}

	private static void process(final File inputFolder, final BlurKernel blurKernel, final File outputFolder){
		final File[] files = inputFolder.listFiles();
		if(files == null || files.length == 0){
			System.out.println("no images to load");

			System.exit(0);
		}

		System.out.println("kernel is " + blurKernel);
		System.out.println("loading images...");

		File leastBlurredImage = null;
		double maximumVariance = 0.;

		for(final File file : files){
			//skip folders
			if(file.isDirectory() || !file.exists())
				continue;

			BufferedImage image = IMAGE_SERVICE.readImage(file);
			if(image == null)
				continue;

			System.out.print("loaded ");
			System.out.print(file.getName());

			image = IMAGE_SERVICE.grayscaled(image);

			image = IMAGE_SERVICE.equalizeHistogram(image);

			image = IMAGE_SERVICE.convolve(image, blurKernel);

			final double variance = IMAGE_SERVICE.variance(image);

			if(variance > maximumVariance){
				maximumVariance = variance;
				leastBlurredImage = file;
			}

			System.out.print("\t-> ");
			System.out.format(Locale.ENGLISH, "%.1f%n", variance);
		}

		System.out.println("least blurred is " + leastBlurredImage.getName());

		moveImage(leastBlurredImage, outputFolder);
	}

	/**
	 * Move file into output folder.
	 *
	 * @param image	Image to be moved.
	 * @param outputFolder	Recipient folder or file.
	 */
	private static void moveImage(final File image, final File outputFolder){
		if(outputFolder != null){
			try{
				Path output = outputFolder.toPath();
				if(outputFolder.isDirectory())
					output = output.resolve(image.getName());
				Files.move(image.toPath(), output, StandardCopyOption.REPLACE_EXISTING);
			}
			catch(final IOException e){
				System.out.println(e.getMessage());

				System.exit(ERROR_CANNOT_MOVE);
			}
		}
	}

}
