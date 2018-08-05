import java.awt.image.BufferedImage;
import java.util.ArrayList;

import ddf.minim.analysis.FFT;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
//import org.opencv.highgui.VideoCapture;

public class VideoCap {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    VideoCapture cap;
    Mat mat = new Mat();
    BufferedImage img;
    byte[] dat;
    CascadeClassifier faceDetector = new CascadeClassifier();
    MatOfRect faceDetections = new MatOfRect();


    private float FORE_HEAD_DETECTION_PERCENTAGE = 80;
    private float FACE_DETECTION_PERCENTAGE = 90;
    private boolean isFaceTrackingEnabled = false;
    private String HAAR_CASCADE_FRONTAL_FACE_ALT = "C:\\Users\\pisha\\IdeaProjects\\FaceToBpm\\src\\haarcascade_frontalface_alt.xml";
    private boolean isHeartRateCalculationEnabled = false;
    private int startAfterFPS = 60;

    ArrayList<Float> poop = new ArrayList();
    float[] sample;
    int bufferSize = 128;
    int sampleRate = 512;
    int bandWidth = 20;
    int centerFreq = 80;

    FFT fft;


    VideoCap() {
        cap = new VideoCapture();
        cap.open(0);
    }

    VideoCap(String video_file) {
        cap = new VideoCapture(video_file);
//        cap.open(0);
    }

    BufferedImage getOneFrame() {
        cap.read(mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2BGR);
        // Adding Time
        Imgproc.putText(mat, java.time.LocalTime.now().toString(), new Point(20, 20), Core.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 255, 0), 1);
        onFrameProcessing();
        return getImage();
    }

    void setFaceTrackingEnable(boolean toEnable) {
        if (toEnable) {
            if (faceDetector.load(HAAR_CASCADE_FRONTAL_FACE_ALT)) {
                System.out.println("HaarCascade Loaded Successfully");
                isFaceTrackingEnabled = true;
            } else {
                System.out.println("HaarCascade Loading Failed");
                isFaceTrackingEnabled = false;
            }
        } else {
            isFaceTrackingEnabled = false;
        }
    }


    void setHeartRateCalculationEnabled(boolean heartRateCalculationEnabled) {
        setFaceTrackingEnable(true);
        isHeartRateCalculationEnabled = heartRateCalculationEnabled;
    }


    double FPSshown = 0;

    private void onFrameProcessing() {
        FPSshown++;
        startAfterFPS--;
        if (startAfterFPS >= 0) {
            return;
        }
        if (isFaceTrackingEnabled) {
            // Detecting faces
            faceDetector.detectMultiScale(mat, faceDetections);


            // Creating a rectangular box showing faces detected
            for (Rect rect : faceDetections.toArray()) {

                Point face_start_point = new Point(rect.x + (rect.width * (100 - FACE_DETECTION_PERCENTAGE) / 100), rect.y + (rect.height * (100 - FACE_DETECTION_PERCENTAGE) / 100));
                Point face_end_point = new Point((rect.x + rect.width) - (rect.width * (100 - FACE_DETECTION_PERCENTAGE) / 100), (rect.y + rect.height) - (rect.height * (100 - FACE_DETECTION_PERCENTAGE) / 100));
                Imgproc.rectangle(mat, face_start_point, face_end_point, new Scalar(0, 255, 0));
                Rect rect_face = new Rect(face_start_point, face_end_point);

                Mat faceMat = mat.submat(rect_face);


                if (isHeartRateCalculationEnabled) {


                    fft = new FFT(bufferSize, sampleRate);
                    // now here is the process to calculate heart rate

                    // Creating a blue rectangular box showing fore head of face detected
                    Point start_point = new Point(rect.x + (rect.width * (100 - FORE_HEAD_DETECTION_PERCENTAGE) / 100), rect.y);
                    Point end_point = new Point((rect.x + rect.width) - (rect.width * (100 - FORE_HEAD_DETECTION_PERCENTAGE) / 100), rect.y + rect.height - (2 * rect.height / 3));
                    Imgproc.rectangle(mat, start_point, end_point, new Scalar(0, 0, 255)); // Drawing Blue Rectangle on forehead.
                    Rect rect_fore_head = new Rect(start_point, end_point);

                    Mat fore_head_mat = mat.submat(rect_fore_head);

                    Mat inputMatForHR = fore_head_mat;
//                    Mat inputMatForHR = faceMat;
//                    Mat inputMatForHR = mat;


                    Imgproc.cvtColor(inputMatForHR, inputMatForHR, Imgproc.COLOR_RGB2GRAY); // Now converted input to gray scale

                    float green_avg = 0;
                    int numPixels = inputMatForHR.rows() * inputMatForHR.cols();
                    for (int px_row = 0; px_row < inputMatForHR.rows(); px_row++) { // For each pixel in the video frame of forehead...
                        for (int px_col = 0; px_col < inputMatForHR.cols(); px_col++) {
                            double[] px_data = inputMatForHR.get(px_row, px_col);
                            int c = (int) px_data[0];
//                            System.out.println("Colour Value  : " + c);
//                            float luminG = c >> 010 & 0xFF;
//                            System.out.println("LuminG value :" + luminG);
                            // getting green color channel of pixel
                            float luminRangeG = (float) (c / 255.0);   //  [0][1][2] RGB or BGR .. G is always in center
                            green_avg = green_avg + luminRangeG;
                        }

                    }

                    green_avg = green_avg / numPixels;
//                    System.out.println("Green  Avg :" + green_avg);
                    if (poop.size() < bufferSize) {
                        poop.add(green_avg);
                    } else poop.remove(0);

                    sample = new float[poop.size()];
                    for (int i = 0; i < poop.size(); i++) {
                        float f = (float) poop.get(i);
                        sample[i] = f;
                    }


                    if (sample.length >= bufferSize) {
                        //fft.window(FFT.NONE);

                        fft.forward(sample, 0);
                        //    bpf = new BandPass(centerFreq, bandwidth, sampleRate);
                        //    in.addEffect(bpf);

                        float heartBeatRate = 0;
//                        System.out.println("FFT Secsize : " + fft.specSize());
                        for (int i = 0; i < fft.specSize(); i++) { // draw the line for frequency band i, scaling it up a bit so we can see it
                            heartBeatRate = Math.max(heartBeatRate, fft.getBand(i));
//                            System.out.println("Band value : " + fft.getBand(i));
                        }

                        float bw = fft.getBandWidth(); // returns the width of each frequency band in the spectrum (in Hz).
                        System.out.println("Bandwidth : " + bw); // returns 21.5332031 Hz for spectrum [0] & [512]

                        heartBeatRate = bw * heartBeatRate;

                        System.out.println("heartBeatRate" + heartBeatRate);
                        // Adding Text
                        Imgproc.putText(
                                mat,                          // Matrix obj of the image
                                "BPM: " + heartBeatRate,          // Text to be added
                                new Point(10, 50),               // point
                                Core.FONT_HERSHEY_SIMPLEX,      // front face
                                0.8,                               // front scale
                                new Scalar(0, 0, 0),             // Scalar object for color
                                1                                // Thickness
                        );
                        
                    } else {
                        System.out.println("Sample Length : " + sample.length + " Poop size : " + poop.size());
                    }

                }


            }

        }

    }

    void stopCap() {
        cap.release();
    }

    BufferedImage getImage() {

        int w = mat.cols(), h = mat.rows();
        if (dat == null || dat.length != w * h * 3)
            dat = new byte[w * h * 3];
        if (img == null || img.getWidth() != w || img.getHeight() != h
                || img.getType() != BufferedImage.TYPE_3BYTE_BGR)
            img = new BufferedImage(w, h,
                    BufferedImage.TYPE_3BYTE_BGR);
        mat.get(0, 0, dat);
        img.getRaster().setDataElements(0, 0,
                mat.cols(), mat.rows(), dat);
        return img;
    }

    boolean isFaceTrackingEnabled() {
        return isFaceTrackingEnabled;
    }

    float getFORE_HEAD_DETECTION_PERCENTAGE() {
        return FORE_HEAD_DETECTION_PERCENTAGE;
    }

    void setFORE_HEAD_DETECTION_PERCENTAGE(int FORE_HEAD_DETECTION_PERCENTAGE) {
        if (FORE_HEAD_DETECTION_PERCENTAGE >= 0 && FORE_HEAD_DETECTION_PERCENTAGE <= 100)
            this.FORE_HEAD_DETECTION_PERCENTAGE = FORE_HEAD_DETECTION_PERCENTAGE;
    }

    boolean isHeartRateCalculationEnabled() {
        return isHeartRateCalculationEnabled;
    }


}