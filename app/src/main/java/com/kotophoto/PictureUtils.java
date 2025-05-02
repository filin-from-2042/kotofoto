package com.kotophoto;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.support.media.ExifInterface;

public class PictureUtils {

    /**
     * Возвращает изображение в виде объекта Bitmap по переданному пути к файлу фото с заданными
     * шириной и высотой, но без учета ориентации.
     * @param path
     * @param destWidth
     * @param destHeight
     * @return
     */
    public static Bitmap getPictureBitmap(String path, int destWidth, int destHeight)
    {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path,options);

        float srcWidth = options.outWidth;
        float srcHeight = options.outHeight;

        int inSampleSize = 1;
        if(srcWidth>destWidth || srcHeight>destHeight){
            if(srcWidth> srcHeight){
                inSampleSize = Math.round(srcHeight/destHeight);
            }else{
                inSampleSize = Math.round(srcWidth/destWidth);
            }
        }

        options = new BitmapFactory.Options();
        options.inSampleSize = inSampleSize;
        return BitmapFactory.decodeFile(path,options);

    }

    /**
     * Возвращает изображение в виде объекта Bitmap с учетом ориентации в exif
     * @param path
     * @param destWidth
     * @param destHeight
     * @return
     */
    public static Bitmap getRotatedPictureBitmap(String path, int destWidth, int destHeight)
    {
        Bitmap rawBitmap = PictureUtils.getPictureBitmap(path,destWidth,destHeight);
        Bitmap rotatedBitmap = rawBitmap;
        try {
            ExifInterface ei = new ExifInterface(path);
            int rotation = 0;
            int exifOrientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (exifOrientation)
            {
                case ExifInterface.ORIENTATION_ROTATE_90: rotation = 90;break;
                case ExifInterface.ORIENTATION_ROTATE_180: rotation = 180;break;
                case ExifInterface.ORIENTATION_ROTATE_270: rotation = 270;break;
            }
            Matrix matrix = new Matrix();
            matrix.preRotate(rotation);
            rotatedBitmap = Bitmap.createBitmap(rawBitmap,0,0,rawBitmap.getWidth(),rawBitmap.getHeight(),matrix,true);
        } catch (Exception ex){
            ex.printStackTrace();
        }
        return rotatedBitmap;
    }
}
