package com.example.joshcarter.a3dphotomaker2;

import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.Arrays;

import static java.lang.Math.abs;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;

/**
 * Created by JoshCarter on 13/03/2018.
 */

abstract public class AutoAlign2 extends AppCompatActivity{

    public static Bitmap alignAllColsVer(Bitmap bitmapL, Bitmap bitmapR, int x, int y) {
        int height = bitmapL.getHeight();
        int width = bitmapL.getWidth();
        int alignSquareSize;

        if (height <= width) {
            alignSquareSize = (int) ((double) height / 2);
        } else {
            alignSquareSize = (int) ((double) width / 2);
        }

        int startVer = (int) ((double) (height - alignSquareSize) / 2) + y;
        int startHor = (int) ((double) (width - alignSquareSize) / 2) + x;

        int[][] horComponentsL = new int[3][alignSquareSize];
        int[][] horComponentsR = new int[3][alignSquareSize];
        int[][] verComponentsL = new int[3][alignSquareSize];
        int[][] verComponentsR = new int[3][alignSquareSize];
        double[] corr = new double[2 * alignSquareSize - 1];
        double[] corrVer = new double[2 * alignSquareSize - 1];

        int[] pixelColorL = new int[alignSquareSize];
        int[] pixelColorR = new int[alignSquareSize];
        int[] pixelColorLVer = new int[alignSquareSize];
        int[] pixelColorRVer = new int[alignSquareSize];

        for (int i = 0; i < alignSquareSize; i++) {

            bitmapL.getPixels(pixelColorL, 0, alignSquareSize, startHor, startVer + i, alignSquareSize, 1);
            bitmapR.getPixels(pixelColorR, 0, alignSquareSize, startHor, startVer + i, alignSquareSize, 1);
            bitmapL.getPixels(pixelColorLVer, 0, 1, startHor + i, startVer, 1, alignSquareSize);
            bitmapR.getPixels(pixelColorRVer, 0, 1, startHor + i, startVer, 1, alignSquareSize);


            for (int j = 0; j < 3; j++) {
                horComponentsL[j][i] = colSum(pixelColorL, j);
                horComponentsR[j][i] = colSum(pixelColorR, j);
                verComponentsL[j][i] = colSum(pixelColorLVer, j);
                verComponentsR[j][i] = colSum(pixelColorRVer, j);

            }
        }


        int PixelBuffer = (int) round(((double) alignSquareSize / 2));

        for (int i = -alignSquareSize + 1 + PixelBuffer; i < alignSquareSize - 1 - PixelBuffer; i++) {

            int[][] horComponentsLReduced = new int[3][alignSquareSize - abs(i)];
            int[][] horComponentsRReduced = new int[3][alignSquareSize - abs(i)];
            int[][] verComponentsLReduced = new int[3][alignSquareSize - abs(i)];
            int[][] verComponentsRReduced = new int[3][alignSquareSize - abs(i)];

            if (i < 0) {
                for (int j = 0; j < 3; j++) {
                    horComponentsLReduced[j] = Arrays.copyOfRange(horComponentsL[j], 0, i + alignSquareSize);
                    horComponentsRReduced[j] = Arrays.copyOfRange(horComponentsR[j], -i, alignSquareSize);
                    verComponentsLReduced[j] = Arrays.copyOfRange(verComponentsL[j], 0, i + alignSquareSize);
                    verComponentsRReduced[j] = Arrays.copyOfRange(verComponentsR[j], -i, alignSquareSize);
                }
            } else if (i > 0) {
                for (int j = 0; j < 3; j++) {
                    horComponentsLReduced[j] = Arrays.copyOfRange(horComponentsL[j], i, alignSquareSize);
                    horComponentsRReduced[j] = Arrays.copyOfRange(horComponentsR[j], 0, alignSquareSize - i);
                    verComponentsLReduced[j] = Arrays.copyOfRange(verComponentsL[j], i, alignSquareSize);
                    verComponentsRReduced[j] = Arrays.copyOfRange(verComponentsR[j], 0, alignSquareSize - i);
                }
            } else {
                for (int j = 0; j < 3; j++) {
                    horComponentsLReduced[j] = Arrays.copyOfRange(horComponentsL[j], 0, alignSquareSize);
                    horComponentsRReduced[j] = Arrays.copyOfRange(horComponentsR[j], 0, alignSquareSize);
                    verComponentsLReduced[j] = Arrays.copyOfRange(verComponentsL[j], 0, alignSquareSize);
                    verComponentsRReduced[j] = Arrays.copyOfRange(verComponentsR[j], 0, alignSquareSize);
                }
            }

            double[] Lmean = new double[3];
            double[] Rmean = new double[3];
            double[] LmeanVer = new double[3];
            double[] RmeanVer = new double[3];

            for (int j = 0; j < 3; j++) {
                Lmean[j] = sum(horComponentsLReduced[j]) / horComponentsLReduced[j].length;
                Rmean[j] = sum(horComponentsRReduced[j]) / horComponentsRReduced[j].length;
                LmeanVer[j] = sum(verComponentsLReduced[j]) / verComponentsLReduced[j].length;
                RmeanVer[j] = sum(verComponentsRReduced[j]) / verComponentsRReduced[j].length;
            }

            double[] numerator = new double[3];
            double[] denominatorL = new double[3];
            double[] denominatorR = new double[3];
            double[] corrPart = new double[3];
            corr[i + alignSquareSize - 1] = 0;

            double[] numeratorVer = new double[3];
            double[] denominatorLVer = new double[3];
            double[] denominatorRVer = new double[3];
            double[] corrPartVer = new double[3];
            corrVer[i + alignSquareSize - 1] = 0;

            for (int j = 0; j < 3; j++) {
                numerator[j] = numerator(horComponentsLReduced[j], Lmean[j], horComponentsRReduced[j], Rmean[j]);
                denominatorL[j] = denominator(horComponentsLReduced[j], Lmean[j]);
                denominatorR[j] = denominator(horComponentsRReduced[j], Rmean[j]);
                corrPart[j] = numerator[j] / (denominatorL[j] * denominatorR[j]);
                corr[i + alignSquareSize - 1] += corrPart[j] * corrPart[j];

                numeratorVer[j] = numerator(verComponentsLReduced[j], LmeanVer[j], verComponentsRReduced[j], RmeanVer[j]);
                denominatorLVer[j] = denominator(verComponentsLReduced[j], LmeanVer[j]);
                denominatorRVer[j] = denominator(verComponentsRReduced[j], RmeanVer[j]);
                corrPartVer[j] = numeratorVer[j] / (denominatorLVer[j] * denominatorRVer[j]);
                corrVer[i + alignSquareSize - 1] += corrPartVer[j] * corrPartVer[j];
            }


        }

        int maxCorr = maxIndex(corr) - alignSquareSize + 1;
        int maxCorrVer = maxIndex(corrVer) - alignSquareSize + 1;

        if (corr[maxCorr + alignSquareSize - 1] < 1.3 || corrVer[maxCorrVer + alignSquareSize - 1] < 1.3) {
            return null;
        }else{
            return combinePhotos(bitmapL, bitmapR, maxCorr, maxCorrVer);
        }
    }


    public static Bitmap alignAllCols(Bitmap bitmapL, Bitmap bitmapR){
        int height = bitmapL.getHeight();
        int width = bitmapL.getWidth();
        int alignSquareSize;

        if (height<=width){
            alignSquareSize = (int)((double)height/2);
        } else {
            alignSquareSize = (int)((double)width/2);
        }

        int startVer = (int)((double)(height-alignSquareSize)/2);
        int startHor = (int)((double)(width-alignSquareSize)/2);

        int[][] horComponentsL = new int[3][alignSquareSize];
        int[][] horComponentsR = new int[3][alignSquareSize];
        double[] corr = new double[2*alignSquareSize-1];

        int[] pixelColorL= new int[bitmapL.getWidth()];
        int[] pixelColorR= new int[bitmapR.getWidth()];
        for (int i=0; i<alignSquareSize; i++){

            bitmapL.getPixels(pixelColorL,0,alignSquareSize,startHor,startVer+i,alignSquareSize,1);
            bitmapR.getPixels(pixelColorR,0,alignSquareSize,startHor,startVer+i,alignSquareSize,1);

            for (int j=0; j<3;j++){
                horComponentsL[j][i]= colSum(pixelColorL,j);
                horComponentsR[j][i]= colSum(pixelColorR,j);
            }
        }

        int PixelBuffer = (int)round(((double)alignSquareSize/2));

        for (int i=-alignSquareSize+1+PixelBuffer; i<alignSquareSize-1-PixelBuffer; i++){

            int[][] horComponentsLReduced = new int[3][alignSquareSize-abs(i)];
            int[][] horComponentsRReduced = new int[3][alignSquareSize-abs(i)];

            if (i<0){
                for (int j=0; j<3; j++) {
                    horComponentsLReduced[j] = Arrays.copyOfRange(horComponentsL[j], 0, i + alignSquareSize);
                    horComponentsRReduced[j] = Arrays.copyOfRange(horComponentsR[j], -i, alignSquareSize);
                }
            } else if (i>0){
                for (int j=0; j<3; j++) {
                    horComponentsLReduced[j] = Arrays.copyOfRange(horComponentsL[j], i, alignSquareSize);
                    horComponentsRReduced[j] = Arrays.copyOfRange(horComponentsR[j], 0, alignSquareSize-i);
                }
            }else{
                for (int j=0; j<3; j++) {
                    horComponentsLReduced[j] = Arrays.copyOfRange(horComponentsL[j], 0, alignSquareSize);
                    horComponentsRReduced[j] = Arrays.copyOfRange(horComponentsR[j], 0, alignSquareSize);
                }
            }

            double[] Lmean = new double[3];
            double[] Rmean = new double[3];
            for (int j=0; j<3; j++) {
                Lmean[j] = sum(horComponentsLReduced[j]) / horComponentsLReduced[j].length;
                Rmean[j] = sum(horComponentsRReduced[j]) / horComponentsRReduced[j].length;
            }

            double[] numerator = new double[3];
            double[] denominatorL = new double[3];
            double[] denominatorR = new double[3];
            double[] corrPart = new double[3];
            corr[i+alignSquareSize-1]=0;

            for(int j=0; j<3;j++){
                numerator[j] = numerator(horComponentsLReduced[j],Lmean[j],horComponentsRReduced[j],Rmean[j]);
                denominatorL[j] = denominator(horComponentsLReduced[j],Lmean[j]);
                denominatorR[j] = denominator(horComponentsRReduced[j],Rmean[j]);
                corrPart[j] = numerator[j]/(denominatorL[j]*denominatorR[j]);
                corr[i+alignSquareSize-1]+=corrPart[j]*corrPart[j];
            }

        }

        int maxCorr = maxIndex(corr)-alignSquareSize+1;

        if (corr[maxCorr + alignSquareSize - 1] < 1.5) {
            return null;
        }else{
            return combinePhotos(bitmapL,bitmapR,maxCorr,0);
        }

    }




    public static Bitmap combinePhotos(Bitmap bitmapL, Bitmap bitmapR, int maxCorr, int maxCorrVer){

        int[] pixelColorL= new int[bitmapL.getWidth()-abs(maxCorrVer)];
        int[] pixelColorR= new int[bitmapR.getWidth()-abs(maxCorrVer)];
        Bitmap picRC = bitmapR.copy(bitmapL.getConfig(),true);

        if(maxCorr>0) {
            if(maxCorrVer>0){
                for (int i = 0; i < bitmapL.getHeight() - maxCorr; i++) {
                    bitmapL.getPixels(pixelColorL, 0, bitmapL.getWidth(), maxCorrVer, i + maxCorr, bitmapL.getWidth()-maxCorrVer, 1);
                    bitmapR.getPixels(pixelColorR, 0, bitmapR.getWidth(), 0, i, bitmapR.getWidth()-maxCorrVer, 1);

                    for (int j = 0; j < bitmapL.getWidth()-maxCorrVer; j++) {
                        pixelColorL[j] = pixelColorL[j] & 0xFFFF0000 | pixelColorR[j] & 0x0000FFFF;
                    }
                    picRC.setPixels(pixelColorL, 0, bitmapL.getWidth(), 0, i, bitmapL.getWidth()-maxCorrVer, 1);
                }
            }else if(maxCorrVer<0){
                for (int i = 0; i < bitmapL.getHeight() - maxCorr; i++) {
                    bitmapL.getPixels(pixelColorL, 0, bitmapL.getWidth(), 0, i + maxCorr, bitmapL.getWidth()+maxCorrVer, 1);
                    bitmapR.getPixels(pixelColorR, 0, bitmapR.getWidth(), -maxCorrVer, i, bitmapR.getWidth()+maxCorrVer, 1);

                    for (int j = 0; j < bitmapL.getWidth()+maxCorrVer; j++) {
                        pixelColorL[j] = pixelColorL[j] & 0xFFFF0000 | pixelColorR[j] & 0x0000FFFF;
                    }
                    picRC.setPixels(pixelColorL, 0, bitmapL.getWidth(), -maxCorrVer, i, bitmapL.getWidth()+maxCorrVer, 1);
                }
            }else{
                for (int i = 0; i < bitmapL.getHeight() - maxCorr; i++) {
                    bitmapL.getPixels(pixelColorL, 0, bitmapL.getWidth(), 0, i + maxCorr, bitmapL.getWidth(), 1);
                    bitmapR.getPixels(pixelColorR, 0, bitmapR.getWidth(), 0, i, bitmapR.getWidth(), 1);

                    for (int j = 0; j < bitmapL.getWidth(); j++) {
                        pixelColorL[j] = pixelColorL[j] & 0xFFFF0000 | pixelColorR[j] & 0x0000FFFF;
                    }
                    picRC.setPixels(pixelColorL, 0, bitmapL.getWidth(), 0, i, bitmapL.getWidth(), 1);
                }
            }
        }else if(maxCorr<0){
            if(maxCorrVer>0){
                for (int i = 0; i < bitmapL.getHeight() + maxCorr; i++) {
                    bitmapL.getPixels(pixelColorL, 0, bitmapL.getWidth(), maxCorrVer, i, bitmapL.getWidth()-maxCorrVer, 1);
                    bitmapR.getPixels(pixelColorR, 0, bitmapR.getWidth(), 0, i-maxCorr, bitmapR.getWidth()-maxCorrVer, 1);

                    for (int j = 0; j < bitmapL.getWidth()-maxCorrVer; j++) {
                        pixelColorL[j] = pixelColorL[j] & 0xFFFF0000 | pixelColorR[j] & 0x0000FFFF;
                    }
                    picRC.setPixels(pixelColorL, 0, bitmapL.getWidth(), 0, i-maxCorr, bitmapL.getWidth()-maxCorrVer, 1);
                }

            }else if(maxCorrVer<0){
                for (int i = 0; i < bitmapL.getHeight() + maxCorr; i++) {
                    bitmapL.getPixels(pixelColorL, 0, bitmapL.getWidth(), 0, i, bitmapL.getWidth()+maxCorrVer, 1);
                    bitmapR.getPixels(pixelColorR, 0, bitmapR.getWidth(), -maxCorrVer, i-maxCorr, bitmapR.getWidth()+maxCorrVer, 1);

                    for (int j = 0; j < bitmapL.getWidth()+maxCorrVer; j++) {
                        pixelColorL[j] = pixelColorL[j] & 0xFFFF0000 | pixelColorR[j] & 0x0000FFFF;
                    }
                    picRC.setPixels(pixelColorL, 0, bitmapL.getWidth(), -maxCorrVer, i-maxCorr, bitmapL.getWidth()+maxCorrVer, 1);
                }
            }else {
                for (int i = 0; i < bitmapL.getHeight() + maxCorr; i++) {
                    bitmapL.getPixels(pixelColorL, 0, bitmapL.getWidth(), 0, i, bitmapL.getWidth(), 1);
                    bitmapR.getPixels(pixelColorR, 0, bitmapR.getWidth(), 0, i - maxCorr, bitmapR.getWidth(), 1);

                    for (int j = 0; j < bitmapL.getWidth(); j++) {
                        pixelColorL[j] = pixelColorL[j] & 0xFFFF0000 | pixelColorR[j] & 0x0000FFFF;
                    }
                    picRC.setPixels(pixelColorL, 0, bitmapL.getWidth(), 0, i - maxCorr, bitmapL.getWidth(), 1);
                }
            }
        }else {
            if (maxCorrVer > 0) {
                for (int i = 0; i < bitmapL.getHeight(); i++) {
                    bitmapL.getPixels(pixelColorL, 0, bitmapL.getWidth(), maxCorrVer, i, bitmapL.getWidth() - maxCorrVer, 1);
                    bitmapR.getPixels(pixelColorR, 0, bitmapR.getWidth(), 0, i, bitmapR.getWidth() - maxCorrVer, 1);

                    for (int j = 0; j < bitmapL.getWidth() - maxCorrVer; j++) {
                        pixelColorL[j] = pixelColorL[j] & 0xFFFF0000 | pixelColorR[j] & 0x0000FFFF;
                    }
                    picRC.setPixels(pixelColorL, 0, bitmapL.getWidth(), 0, i, bitmapL.getWidth() - maxCorrVer, 1);
                }
            } else if (maxCorrVer < 0) {
                for (int i = 0; i < bitmapL.getHeight(); i++) {
                    bitmapL.getPixels(pixelColorL, 0, bitmapL.getWidth(), 0, i, bitmapL.getWidth() + maxCorrVer, 1);
                    bitmapR.getPixels(pixelColorR, 0, bitmapR.getWidth(), -maxCorrVer, i, bitmapR.getWidth() + maxCorrVer, 1);

                    for (int j = 0; j < bitmapL.getWidth() + maxCorrVer; j++) {
                        pixelColorL[j] = pixelColorL[j] & 0xFFFF0000 | pixelColorR[j] & 0x0000FFFF;
                    }
                    picRC.setPixels(pixelColorL, 0, bitmapL.getWidth(), -maxCorrVer, i, bitmapL.getWidth() + maxCorrVer, 1);
                }
            } else {
                for (int i = 0; i < bitmapL.getHeight(); i++) {
                    bitmapL.getPixels(pixelColorL, 0, bitmapL.getWidth(), 0, i, bitmapL.getWidth(), 1);
                    bitmapR.getPixels(pixelColorR, 0, bitmapR.getWidth(), 0, i, bitmapR.getWidth(), 1);

                    for (int j = 0; j < bitmapL.getWidth(); j++) {
                        pixelColorL[j] = pixelColorL[j] & 0xFFFF0000 | pixelColorR[j] & 0x0000FFFF;
                    }
                    picRC.setPixels(pixelColorL, 0, bitmapL.getWidth(), 0, i, bitmapL.getWidth(), 1);
                }
            }

        }


        return picRC;
    }


    private static double sum(int[] vec){
        double sum = 0;
        for (int aVec : vec) {
            sum += aVec;
        }
        return sum;
    }

    private static double numerator(int[] pixelColorL, double Lmean, int[] pixelColorR, double Rmean){
        double numerator=0;
        for(int i=0;i<pixelColorL.length;i++){
            numerator += (pixelColorL[i]-Lmean)*(pixelColorR[i]-Rmean);
        }
        return numerator;
    }

    private static double denominator(int[] pixelColour, double mean){
        double denominator=0;
        for (int aPixelColour : pixelColour) {
            denominator += (aPixelColour - mean) * (aPixelColour - mean);
        }
        denominator = sqrt(denominator);
        return denominator;
    }

    private static int maxIndex(double[] corr){
        int maxIndex=0;
        for(int i=0;i<corr.length;i++){
            if(corr[i]>corr[maxIndex]){
                maxIndex=i;
            }
        }
        return maxIndex;
    }

    private static int colSum(int[] pixelColors, int j){
        int ColSum = 0;
        switch (j){
            case 0:
                for(int i=1;i<pixelColors.length;i++){
                    ColSum += pixelColors[i] & 0x00FF0000;
                }
                break;
            case 1:
                for(int i=1;i<pixelColors.length;i++){
                    ColSum += pixelColors[i] & 0x0000FF00;
                }
                break;
            case 2:
                for(int i=1;i<pixelColors.length;i++){
                    ColSum += pixelColors[i] & 0x000000FF;
                }
                break;
        }
        return ColSum;
    }

    private static int colSumRed(int[] pixelCol){
        int sum = 0;
        for(int i=0;i<pixelCol.length;i++){
            sum += pixelCol[i] & 0x00FF0000;
        }
        return sum;
    }
}
