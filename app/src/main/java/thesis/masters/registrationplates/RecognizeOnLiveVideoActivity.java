package thesis.masters.registrationplates;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Surface;
import android.view.SurfaceView;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.ListIterator;

public class RecognizeOnLiveVideoActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    CameraBridgeViewBase cameraBridgeViewBase;
    Mat mat1, mat2, mat3, mat4, tmp, mat5, mat6, mat7, mat8, mat9;
    Bitmap recognitionBitmap, recognitionBitmapPortrait, recognitionBitmapLandscape;
    BaseLoaderCallback baseLoaderCallback;
    TextView textViewVideoRecognitionOutput;
    CheckBox checkBoxOCRActive;
    TextRecognizer textRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognize_on_live_video);
        this.cameraBridgeViewBase = (JavaCameraView) findViewById(R.id.rearCameraView);
        this.textViewVideoRecognitionOutput = findViewById(R.id.recognitionVideoOutputTextView);
        this.checkBoxOCRActive = findViewById(R.id.enableOCRLiveVideoCheckBox);
        textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);
        this.baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {

                switch (status)
                {
                    case BaseLoaderCallback.SUCCESS:
                        cameraBridgeViewBase.enableView();
                        break;
                    default :
                         super.onManagerConnected(status);
                         break;



                }
            }
        };
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mat1 = inputFrame.rgba();

        mat7 = mat1;


        // recognize code
        //int initialImageBitmapHeight = imageBitmap.getHeight();
       // int initialImageBitmapWidth = imageBitmap.getWidth();

       // Mat imageMat1 = new Mat(initialImageBitmapHeight,initialImageBitmapWidth,CvType.CV_8U,new Scalar(4));
       // Mat imageMat2 = new Mat(initialImageBitmapHeight,initialImageBitmapWidth,CvType.CV_8U,new Scalar(4));
       // Mat imageMat3 = new Mat(initialImageBitmapHeight,initialImageBitmapWidth,CvType.CV_8U,new Scalar(4));
       // Mat imageMat4 = new Mat(initialImageBitmapHeight,initialImageBitmapWidth,CvType.CV_8U,new Scalar(4));

       // Mat tmp = new Mat(initialImageBitmapHeight,initialImageBitmapWidth,CvType.CV_8U,new Scalar(4));

        //Bitmap copyBitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888,true);
       // Utils.bitmapToMat(copyBitmap,imageMat1);

        //RGB -> Gray
        Imgproc.cvtColor(mat1,mat3,Imgproc.COLOR_RGB2GRAY,8);

        Imgproc.dilate(mat3,tmp,Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(9,9)));
        Imgproc.erode(tmp,mat4,Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(9,9)));

        Core.absdiff(mat4,mat3,mat2);

        Imgproc.Sobel(mat2,mat2,CvType.CV_8U,1,0,3,1,0.4,4);

        Imgproc.GaussianBlur(mat2,mat2,new Size(5,5),2);

        Imgproc.dilate(mat2,mat2,Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3)));

        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(17,3));

        Imgproc.morphologyEx(mat2,mat2,Imgproc.MORPH_CLOSE,element);

        Imgproc.threshold(mat2, mat2, 0 ,255, Imgproc.THRESH_OTSU+Imgproc.THRESH_BINARY);


        ArrayList<RotatedRect> rects = new  ArrayList<RotatedRect>();
        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(mat2, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

        ListIterator<MatOfPoint> itc = contours.listIterator();
        while(itc.hasNext())
        {
            MatOfPoint2f mp2f = new MatOfPoint2f(itc.next().toArray());
            RotatedRect mr = Imgproc.minAreaRect(mp2f);
            double area = Math.abs(Imgproc.contourArea(mp2f));

            double bbArea= mr.size.area();
            double ratio = area / bbArea;
            if( (ratio < 0.45) || (bbArea < 400) )
            {
                itc.remove();  // other than deliberately making the program slow,
                // does erasing the contour have any purpose?
            }
            else
            {
                rects.add(mr);
            }



        }

        for (int i = 0; i < contours.size(); i++) {
            Imgproc.drawContours(mat1, contours, i, new Scalar(100, 255, 255), 5);
        }

        // END recognize code




        //frame rotation
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            Core.transpose(mat1,mat5);
            Imgproc.resize(mat5,mat6,mat6.size(),0,0,0);
            Core.flip(mat6,mat1,1);
        }

        if (checkBoxOCRActive.isChecked())
                getTextFromVideo(mat7,orientation);


        return mat1;
    }

    @Override
    public void onCameraViewStopped() {
        mat1.release();
        mat2.release();
        mat3.release();

    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mat1 = new Mat(width,height,CvType.CV_8U,new Scalar(4));
        mat2 = new Mat(width,height,CvType.CV_8U,new Scalar(4));
        mat3 = new Mat(width,height,CvType.CV_8U,new Scalar(4));
        mat4 = new Mat(width,height,CvType.CV_8U,new Scalar(4));
        tmp = new Mat(width,height,CvType.CV_8U,new Scalar(4));
        mat5 = new Mat(width,height,CvType.CV_8U,new Scalar(4));
        mat6 = new Mat(width,height,CvType.CV_8U,new Scalar(4));
        mat7 = new Mat(width,height,CvType.CV_8U,new Scalar(4));
        mat8 = new Mat(width,height,CvType.CV_8U,new Scalar(4));
        mat9 = new Mat(width,height,CvType.CV_8U,new Scalar(4));
        recognitionBitmapPortrait = Bitmap.createBitmap(mat1.cols(),mat1.rows(),Bitmap.Config.ARGB_8888);
        recognitionBitmapLandscape =  Bitmap.createBitmap(mat1.rows(),mat1.cols(),Bitmap.Config.ARGB_8888);




    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraBridgeViewBase!=null)
        {
            //cameraBridgeViewBase.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!OpenCVLoader.initDebug())
        {
            Toast.makeText(getApplicationContext(),"OpenCV problem",Toast.LENGTH_SHORT).show();
        }
        else
        {
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(cameraBridgeViewBase!=null)
        {
            cameraBridgeViewBase.disableView();
        }
    }

    public void getTextFromVideo(Mat mat, int orientation){
       /* Bitmap recognitionBitmap = Bitmap.createBitmap(mat.cols(),mat.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat,recognitionBitmap);
        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();*/

       if (orientation == Configuration.ORIENTATION_PORTRAIT) {
           Utils.matToBitmap(mat, recognitionBitmapPortrait);
           recognitionBitmap = recognitionBitmapPortrait;
       } else
       {
           Utils.matToBitmap(mat, recognitionBitmapLandscape);
           recognitionBitmap = recognitionBitmapLandscape;
       }
        if (textRecognizer.isOperational())
        {
            Frame frame = new Frame.Builder().setBitmap(recognitionBitmap).build();
            final SparseArray<TextBlock> items = textRecognizer.detect(frame);
            if (items.size()!=0) {
                textViewVideoRecognitionOutput.post(new Runnable() {
                    @Override
                    public void run() {
                        StringBuilder sb = new StringBuilder();

                        for (int i = 0; i < items.size(); i++) {
                            TextBlock myItems = items.valueAt(i);
                            sb.append(myItems.getValue());
                            sb.append("\n");
                        }
                        textViewVideoRecognitionOutput.setText(sb.toString());
                    }
                });
            }
        }

    }
}
