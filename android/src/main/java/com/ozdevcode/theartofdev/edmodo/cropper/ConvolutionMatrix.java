package com.ozdevcode.theartofdev.edmodo.cropper;

import android.graphics.Bitmap;
import android.graphics.Color;

import android.graphics.Matrix; //Added by chandrajyoti


/*
public class ConvolutionMatrix
{
    public static final int SIZE = 3;

    public double[][] Matrix;
    public double Factor = 1;
    public double Offset = 1;

    public ConvolutionMatrix(int size) {
        Matrix = new double[size][size];
    }

    public void setAll(double value) {
        for (int x = 0; x < SIZE; ++x) {
            for (int y = 0; y < SIZE; ++y) {
                Matrix[x][y] = value;
            }
        }
    }

    public void applyConfig(double[][] config) {
        for(int x = 0; x < SIZE; ++x) {
            for(int y = 0; y < SIZE; ++y) {
                Matrix[x][y] = config[x][y];
            }
        }
    }

    public static Bitmap computeConvolution3x3(Bitmap src, ConvolutionMatrix matrix) {
        int width = src.getWidth();
        int height = src.getHeight();
        Bitmap result = Bitmap.createBitmap(width, height, src.getConfig());

        int A, R, G, B;
        int sumR, sumG, sumB;
        int[][] pixels = new int[SIZE][SIZE];

        for(int y = 0; y < height - 2; ++y) {
            for(int x = 0; x < width - 2; ++x) {

                // get pixel matrix
                for(int i = 0; i < SIZE; ++i) {
                    for(int j = 0; j < SIZE; ++j) {
                        pixels[i][j] = src.getPixel(x + i, y + j);
                    }
                }

                // get alpha of center pixel
                A = Color.alpha(pixels[1][1]);

                // init color sum
                sumR = sumG = sumB = 0;

                // get sum of RGB on matrix
                for(int i = 0; i < SIZE; ++i) {
                    for(int j = 0; j < SIZE; ++j) {
                        sumR += (Color.red(pixels[i][j]) * matrix.Matrix[i][j]);
                        sumG += (Color.green(pixels[i][j]) * matrix.Matrix[i][j]);
                        sumB += (Color.blue(pixels[i][j]) * matrix.Matrix[i][j]);
                    }
                }

                // get final Red
                R = (int)(sumR / matrix.Factor + matrix.Offset);
                if(R < 0) { R = 0; }
                else if(R > 255) { R = 255; }

                // get final Green
                G = (int)(sumG / matrix.Factor + matrix.Offset);
                if(G < 0) { G = 0; }
                else if(G > 255) { G = 255; }

                // get final Blue
                B = (int)(sumB / matrix.Factor + matrix.Offset);
                if(B < 0) { B = 0; }
                else if(B > 255) { B = 255; }

                // apply new pixel
                result.setPixel(x + 1, y + 1, Color.argb(A, R, G, B));
            }
        }

        // final image
        return result;
    }
}
*/

public class ConvolutionMatrix{
		private static final int MATRIX_SIZE = 3;

		private static int cap(int color) {
				if (color > 255)
						return 255;
				else if (color < 0)
						return 0;
				else
						return color;
		}

		public static Bitmap convolute(Bitmap bmp, float [] mxv, int factor, int offset) {
				// get matrix values
				// float [] mxv = new float[MATRIX_SIZE * MATRIX_SIZE];
				// mat.getValues(mxv);
				// cache source pixels
				int width = bmp.getWidth();
				int height = bmp.getHeight();
				int [] scrPxs = new int[width * height];
				bmp.getPixels(scrPxs, 0, width, 0, 0, width, height);

				// clone source pixels in an array
				// here we will store results
				int [] rtPxs = scrPxs.clone();

				int r, g, b;
				int rSum, gSum, bSum;
				int idx;	// current pixel index
				int pix;	// current pixel
				float mv;	// current matrix value

				for(int x = 0, w = width - MATRIX_SIZE + 1; x < w; ++x) {
						for(int y = 0, h = height - MATRIX_SIZE + 1; y < h; ++y) {
								idx = (x + 1) + (y + 1) * width;
								rSum = gSum = bSum = 0;
								for(int mx = 0; mx < MATRIX_SIZE; ++mx) {
										for(int my = 0; my < MATRIX_SIZE; ++my) {
												pix = scrPxs[(x + mx) + (y + my) * width];
												mv = mxv[mx + my * MATRIX_SIZE];

												rSum += (Color.red(pix) * mv);
												gSum += (Color.green(pix) * mv);
												bSum += (Color.blue(pix) * mv);
										}
								}

								r = cap((int)(rSum / factor + offset));
								g = cap((int)(gSum / factor + offset));
								b = cap((int)(bSum / factor + offset));
								// store computed pixel
								rtPxs[idx] = Color.argb(Color.alpha(scrPxs[idx]), r, g, b);
						}
				}

				// return bitmap with transformed pixels
				return Bitmap.createBitmap(rtPxs, width, height, bmp.getConfig());
		}
}