import static org.bytedeco.javacpp.opencv_core.CV_PI;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import java.io.File;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_core.CvContour;
import org.bytedeco.javacpp.opencv_core.CvMemStorage;
import org.bytedeco.javacpp.opencv_core.CvSeq;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_imgcodecs;

/**
 * 
 * This program attempts to order images based on the principle that the
 * similarity between images is linked to the relation between the number of
 * lines and the number of basic figure structures formed in the images. Thus we
 * try to establish a relation between the quantity of squares and circles in a
 * figure and the total number of lines.
 * 
 * @author djalmapassos
 *
 */
public class ImageDistanceComparator {

	static class Complex{
		public double real, img;
		
		public Complex(double real, double img) {
			this.real = real;
			this.img = img;
		}
		
		public double getModule() {
			return Math.hypot(real, img);
		}
		public double getAngle() {
			return Math.atan2(real,img);			
		}
		
		 public Complex plus(Complex b) {
		        Complex a = this;
		        double real = a.real + b.real;
		        double imag = a.img + b.img;
		        return new Complex(real, imag);
		 }
	}
	
	static File dir = new File("resources");
	static File resDir = new File("resources/result");
	static final double SQUARE_WEIGTH = 100;
	static final double CIRCLE_WEIGTH = 10;
	static final double LINE_WEIGTH = 1;
	
	

	public static void main(String[] args) throws Exception {
		
		if (!resDir.exists())
			resDir.mkdir();
		if (resDir.listFiles() != null) {
			for (File f2 : resDir.listFiles()) {
				f2.delete();
			}
		}
		int count = 0;
		int sucessCount = 0;
		for (File aux : dir.listFiles()) {
			if (aux.getName().contains(".jpg") || aux.getName().contains(".png") || aux.getName().contains(".jpeg")) {
				try {
					
					Complex score = score(aux);
					//two fraction digits
					String strScore = (Math.round(score.getModule() * 100))+"_"+score.getAngle();
					
					System.out.println("Score:" + aux.getName() + "=>" + (strScore));
					File out2 = new File(resDir, "" + strScore + "_" + System.currentTimeMillis() + ".png");
					org.bytedeco.javacpp.opencv_imgcodecs.imwrite(out2.getAbsolutePath(),
							imread(aux.getAbsolutePath(), opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE));

					sucessCount++;
				} catch (Exception e) {
					e.printStackTrace();
				}
				count++;
			}
		}
		System.out.println("Hits " + sucessCount + " From " + count);
	}
	
	public static Complex score(File path) {
		Mat Image1 = imread(path.getAbsolutePath(), opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
		IplImage img1 = new IplImage(Image1);

		CvMemStorage mem = CvMemStorage.create();
		CvSeq circles = cvHoughCircles(img1, // Input image
				mem, // Memory Storage
				CV_HOUGH_GRADIENT, // Detection method
				1, // Inverse ratio
				100, // Minimum distance between the centers of the detected circles
				10, // Higher threshold for canny edge detector
				10, // Threshold at the center detection stage
				15, // min radius
				500 // max radius
		);

		double countCircle = circles.total();
		double countSquare = 0.0;

		CvSeq contours = new CvSeq();
		mem = CvMemStorage.create();
		cvFindContours(img1, mem, contours, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE,
				cvPoint(0, 0));
		if (contours != null && !contours.isNull()) {
			CvSeq result = cvApproxPoly(contours, Loader.sizeof(CvContour.class), mem, CV_POLY_APPROX_DP,
					cvContourPerimeter(contours) * 0.02, 0);
			countSquare = result.total();
		}

		CvSeq lines = new CvSeq();
		mem = CvMemStorage.create();
		lines = cvHoughLines2(img1, mem, CV_HOUGH_STANDARD, 1, Math.PI / 180, 40, 1, 0, 0, CV_PI);
		double lineCount = lines.total();

		lines.close();
		circles.close();
		
		Complex a = new Complex(lineCount * LINE_WEIGTH, countCircle * CIRCLE_WEIGTH);			
		Complex b = new Complex(lineCount * LINE_WEIGTH, countSquare * SQUARE_WEIGTH);
		return a.plus(b);
	}
}