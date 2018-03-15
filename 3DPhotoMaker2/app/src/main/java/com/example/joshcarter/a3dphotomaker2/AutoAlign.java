package com.example.joshcarter.a3dphotomaker2;

/**
 * Created by JoshCarter on 14/03/2018.
 */

import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.Arrays;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

/**
 * Created by JoshCarter on 13/03/2018.
 */

abstract public class AutoAlign extends AppCompatActivity{

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

        int PixelBuffer = 100;

        for (int i=-alignSquareSize+1+PixelBuffer; i<alignSquareSize-1-PixelBuffer; i++){

            //Log.d("horComSiz",Integer.toString(horComponentsL[2].length));
            int[][] horComponentsLReduced = new int[3][alignSquareSize-abs(i)];
            int[][] horComponentsRReduced = new int[3][alignSquareSize-abs(i)];

            if (i<0){
                for (int j=0; j<3; j++) {
                    //Log.d("horComRedSize",Integer.toString(horComponentsLReduced[j].length));
                    //Log.d("horComRedSize2",Integer.toString(Arrays.copyOfRange(horComponentsL[j], 0, i + alignSquareSize).length));
                    //Log.d("test1",Integer.toString(i + alignSquareSize));
                    //Log.d("test2",Integer.toString(-i));
                    //Log.d("test2",Integer.toString(alignSquareSize));
                    //Log.d("horComRedSize3",Integer.toString(Arrays.copyOfRange(horComponentsR[j], -i, alignSquareSize).length));
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
                //Log.d("horComLength",Integer.toString(horComponentsLReduced[j].length));
                //Log.d("horCom",horComponentsLReduced[j].toString());
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


            Log.d(Integer.toString(i),Double.toString(corr[i+alignSquareSize-1]));


        }

        int maxCorr = maxIndex(corr)-alignSquareSize+1;
        Log.d("maxCorr",Integer.toString(maxCorr));

        return combinePhotos(bitmapL,bitmapR,maxCorr);
    }


    public static Bitmap alignRed(Bitmap bitmapL, Bitmap bitmapR){
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

        //Bitmap bitmapLSmall = Bitmap.createBitmap(bitmapL,startHor,startVer,alignSquareSize,alignSquareSize);
        //Bitmap bitmapRSmall = Bitmap.createBitmap(bitmapR,startHor,startVer,alignSquareSize,alignSquareSize);

        int[] horComponentsL = new int[alignSquareSize];
        int[] horComponentsR = new int[alignSquareSize];
        double[] corr = new double[2*alignSquareSize-1];

        int[] pixelColorL= new int[bitmapL.getWidth()];
        int[] pixelColorR= new int[bitmapR.getWidth()];
        Log.d("test","1");
        for (int i=0; i<alignSquareSize; i++){

            bitmapL.getPixels(pixelColorL,0,alignSquareSize,startHor,startVer+i,alignSquareSize,1);
            bitmapR.getPixels(pixelColorR,0,alignSquareSize,startHor,startVer+i,alignSquareSize,1);

            horComponentsL[i]= (int)colSumRed(pixelColorL);
            horComponentsR[i]= (int)colSumRed(pixelColorR);
        }

        int PixelBuffer = 100;
        Log.d("test","2");
        for (int i=-alignSquareSize+1+PixelBuffer; i<alignSquareSize-1-PixelBuffer; i++){

            int[] horComponentsLReduced = new int[alignSquareSize-abs(i)];
            int[] horComponentsRReduced = new int[alignSquareSize-abs(i)];

            if (i<0){
                horComponentsLReduced = Arrays.copyOfRange(horComponentsL,0,i+alignSquareSize);
                horComponentsRReduced = Arrays.copyOfRange(horComponentsR,-i,alignSquareSize);
            } else if (i>0){
                horComponentsLReduced = Arrays.copyOfRange(horComponentsL,i,alignSquareSize);
                horComponentsRReduced = Arrays.copyOfRange(horComponentsR,0,alignSquareSize-i);
            }else{
                horComponentsLReduced = Arrays.copyOfRange(horComponentsL,0,alignSquareSize);
                horComponentsRReduced = Arrays.copyOfRange(horComponentsR,0,alignSquareSize);
            }

            double Lmean = sum(horComponentsLReduced)/horComponentsLReduced.length;
            double Rmean = sum(horComponentsRReduced)/horComponentsRReduced.length;

            double numerator = numerator(horComponentsLReduced,Lmean,horComponentsRReduced,Rmean);
            double denominatorL = denominator(horComponentsLReduced,Lmean);
            double denominatorR = denominator(horComponentsRReduced,Rmean);

            corr[i+alignSquareSize-1]=numerator/(denominatorL*denominatorR);
        }
        Log.d("test","3");

        int maxCorr = maxIndex(corr)-alignSquareSize+1;
        Log.d("maxCorr",Integer.toString(maxCorr));

        return combinePhotos(bitmapL,bitmapR,maxCorr);
    }






    public static Bitmap combinePhotos(Bitmap bitmapL, Bitmap bitmapR, int maxCorr){

        int[] pixelColorL= new int[bitmapL.getWidth()];
        int[] pixelColorR= new int[bitmapR.getWidth()];
        //Bitmap picLC = bitmapL.copy(bitmapL.getConfig(),true);
        Bitmap picRC = bitmapR.copy(bitmapL.getConfig(),true);
        Log.d("test","4");

        if(maxCorr>0) {
            for (int i = 0; i < bitmapL.getHeight() - maxCorr; i++) {
                bitmapL.getPixels(pixelColorL, 0, bitmapL.getWidth(), 0, i + maxCorr, bitmapL.getWidth(), 1);
                bitmapR.getPixels(pixelColorR, 0, bitmapR.getWidth(), 0, i, bitmapR.getWidth(), 1);

                for (int j = 0; j < bitmapL.getWidth(); j++) {
                    pixelColorL[j] = pixelColorL[j] & 0xFFFF0000 | pixelColorR[j] & 0x0000FFFF;
                }
                //picLC.setPixels(pixelColorL, 0, bitmapL.getWidth(), 0, i+maxCorr, bitmapL.getWidth(), 1);
                picRC.setPixels(pixelColorL, 0, bitmapL.getWidth(), 0, i, bitmapL.getWidth(), 1);
            }
        }else if (maxCorr<0){
            for (int i = 0; i < bitmapL.getHeight() + maxCorr; i++) {
                bitmapL.getPixels(pixelColorL, 0, bitmapL.getWidth(), 0, i, bitmapL.getWidth(), 1);
                bitmapR.getPixels(pixelColorR, 0, bitmapR.getWidth(), 0, i-maxCorr, bitmapR.getWidth(), 1);

                for (int j = 0; j < bitmapL.getWidth(); j++) {
                    pixelColorL[j] = pixelColorL[j] & 0xFFFF0000 | pixelColorR[j] & 0x0000FFFF;
                }
                //picLC.setPixels(pixelColorL, 0, bitmapL.getWidth(), 0, i, bitmapL.getWidth(), 1);
                picRC.setPixels(pixelColorL, 0, bitmapL.getWidth(), 0, i-maxCorr, bitmapL.getWidth(), 1);
            }
        }else {
            for (int i = 0; i < bitmapL.getHeight(); i++) {
                bitmapL.getPixels(pixelColorL, 0, bitmapL.getWidth(), 0, i, bitmapL.getWidth(), 1);
                bitmapR.getPixels(pixelColorR, 0, bitmapR.getWidth(), 0, i, bitmapR.getWidth(), 1);

                for (int j = 0; j < bitmapL.getWidth(); j++) {
                    pixelColorL[j] = pixelColorL[j] & 0xFFFF0000 | pixelColorR[j] & 0x0000FFFF;

                }
                //picLC.setPixels(pixelColorL, 0, bitmapL.getWidth(), 0, i, bitmapL.getWidth(), 1);
                picRC.setPixels(pixelColorL, 0, bitmapL.getWidth(), 0, i, bitmapL.getWidth(), 1);
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