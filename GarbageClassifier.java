import ai.onnxruntime.*;
import org.opencv.core.*;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.FloatBuffer;
import java.util.Collections;

public class GarbageClassifier {

    private static final String MODEL_PATH = "model.onnx";
    private static final String[] CLASS_NAMES = {"cardboard","glass","metal","paper","plastic","trash"};
    private static final int IMG_SIZE = 224;

    public static void main(String[] args) throws Exception {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Create Swing window
        JFrame frameWindow = new JFrame("Garbage Classifier");
        JLabel imageLabel = new JLabel();
        frameWindow.getContentPane().add(imageLabel);
        frameWindow.setSize(1920, 1080);
        frameWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frameWindow.setVisible(true);

        // Load ONNX model
        try (OrtEnvironment env = OrtEnvironment.getEnvironment();
             OrtSession session = env.createSession(MODEL_PATH, new OrtSession.SessionOptions())) {

            VideoCapture cap = new VideoCapture(0);
            if (!cap.isOpened()) {
                System.out.println("❌ Cannot access webcam");
                return;
            }

            Mat frameMat = new Mat();
            while (true) {
                cap.read(frameMat);
                if (frameMat.empty()) continue;

                // Preprocess for model
                Mat resized = new Mat();
                Imgproc.resize(frameMat, resized, new Size(IMG_SIZE, IMG_SIZE));
                float[] inputData = matToTensor(resized);

                OnnxTensor inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputData),
                        new long[]{1, 3, IMG_SIZE, IMG_SIZE});
                OrtSession.Result results = session.run(Collections.singletonMap("input", inputTensor));
                float[][] output = (float[][]) results.get(0).getValue();

                int predIdx = argMax(output[0]);
                float conf = softmax(output[0])[predIdx] * 100;
                String label = String.format("%s: %.2f%%", CLASS_NAMES[predIdx], conf);

                // Draw label on frame
                Imgproc.putText(frameMat, label, new org.opencv.core.Point(10, 30),
                Imgproc.FONT_HERSHEY_SIMPLEX, 1.0, new Scalar(0, 255, 0), 2);


                // Convert Mat to BufferedImage
                BufferedImage bufferedImage = matToBufferedImage(frameMat);
                imageLabel.setIcon(new ImageIcon(bufferedImage));
                imageLabel.repaint();
            }
        }
    }

    // Convert OpenCV Mat to BufferedImage
    private static BufferedImage matToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_3BYTE_BGR;
        if (mat.channels() == 1) type = BufferedImage.TYPE_BYTE_GRAY;

        byte[] b = new byte[mat.channels() * mat.cols() * mat.rows()];
        mat.get(0, 0, b);
        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);
        return image;
    }

    // Preprocess Mat to float tensor for ONNX
    private static float[] matToTensor(Mat mat) {
        float[] data = new float[3 * IMG_SIZE * IMG_SIZE];
        int idx = 0;
        float[] mean = {0.485f,0.456f,0.406f};
        float[] std = {0.229f,0.224f,0.225f};

        for (int y=0; y<IMG_SIZE; y++) {
            for (int x=0; x<IMG_SIZE; x++) {
                double[] px = mat.get(y,x);
                data[idx] = (float)((px[2]/255.0 - mean[0])/std[0]);
                data[idx + IMG_SIZE*IMG_SIZE] = (float)((px[1]/255.0 - mean[1])/std[1]);
                data[idx + 2*IMG_SIZE*IMG_SIZE] = (float)((px[0]/255.0 - mean[2])/std[2]);
                idx++;
            }
        }
        return data;
    }

    private static int argMax(float[] arr) {
        int idx=0; float max=arr[0];
        for(int i=1;i<arr.length;i++){
            if(arr[i]>max){ max=arr[i]; idx=i; }
        }
        return idx;
    }

    private static float[] softmax(float[] logits){
        float max=Float.NEGATIVE_INFINITY;
        for(float v: logits) if(v>max) max=v;
        float sum=0f;
        float[] exp=new float[logits.length];
        for(int i=0;i<logits.length;i++){
            exp[i]=(float)Math.exp(logits[i]-max);
            sum+=exp[i];
        }
        for(int i=0;i<exp.length;i++) exp[i]/=sum;
        return exp;
    }
}
