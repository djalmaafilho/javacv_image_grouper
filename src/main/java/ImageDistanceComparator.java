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

	static File dir = new File("resources");
	static File resDir = new File(dir, "result");
	static final double SQUARE_FACTOR = 4;
	static final double CIRCLE_FACTOR = Math.PI;

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
			if (aux.getName().contains(".jpg") || aux.getName().contains(".png")) {
				try {
					double score = score(aux);
					System.out.println("Score:" + aux.getName() + "=>" + (score));
					File out2 = new File(resDir, "" + score + "_" + aux.getName() + ".png");
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
	
	public static double score(File path) {
		Mat Image1 = imread(path.getAbsolutePath(), opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
		IplImage img1 = new IplImage(Image1);

		CvMemStorage mem = CvMemStorage.create();
		CvSeq circles = cvHoughCircles(img1, // Input image
				mem, // Memory Storage
				CV_HOUGH_GRADIENT, // Detection method
				1, // Inverse ratio
				100, // Minimum distance between the centers of the detected circles
				100, // Higher threshold for canny edge detector
				100, // Threshold at the center detection stage
				15, // min radius
				500 // max radius
		);

		double countCircle = circles.total() * CIRCLE_FACTOR;
		double countSquare = 0.0;

		CvSeq contours = new CvSeq();
		mem = CvMemStorage.create();
		cvFindContours(img1, mem, contours, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE,
				cvPoint(0, 0));
		if (contours != null && !contours.isNull()) {
			CvSeq result = cvApproxPoly(contours, Loader.sizeof(CvContour.class), mem, CV_POLY_APPROX_DP,
					cvContourPerimeter(contours) * 0.02, 0);
			countSquare = (result.total() * SQUARE_FACTOR);
		}

		double featureCount = countSquare + countCircle;
		System.out.println("Total features " + featureCount);

		CvSeq lines = new CvSeq();
		mem = CvMemStorage.create();
		lines = cvHoughLines2(img1, mem, CV_HOUGH_STANDARD, 1, Math.PI / 180, 40, 1, 0, 0, CV_PI);

		double lineCount = lines.total();
		System.out.println("lines " + lineCount);

		lines.close();
		circles.close();

		double vector =  Math.sqrt((featureCount)*(featureCount) + (lineCount)*(lineCount));

		return vector;
	}
}