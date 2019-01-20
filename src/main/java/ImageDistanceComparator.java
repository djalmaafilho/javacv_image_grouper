import static org.bytedeco.javacpp.opencv_core.CV_PI;
import static org.bytedeco.javacpp.opencv_core.cvGetSeqElem;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_core.cvPointFrom32f;
import static org.bytedeco.javacpp.opencv_core.cvScalar;
import static org.bytedeco.javacpp.opencv_core.cvScalarAll;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgproc.CV_AA;
import static org.bytedeco.javacpp.opencv_imgproc.CV_CHAIN_APPROX_TC89_KCOS;
import static org.bytedeco.javacpp.opencv_imgproc.CV_HOUGH_GRADIENT;
import static org.bytedeco.javacpp.opencv_imgproc.CV_HOUGH_STANDARD;
import static org.bytedeco.javacpp.opencv_imgproc.CV_POLY_APPROX_DP;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RETR_LIST;
import static org.bytedeco.javacpp.opencv_imgproc.GaussianBlur;
import static org.bytedeco.javacpp.opencv_imgproc.cvApproxPoly;
import static org.bytedeco.javacpp.opencv_imgproc.cvCanny;
import static org.bytedeco.javacpp.opencv_imgproc.cvCircle;
import static org.bytedeco.javacpp.opencv_imgproc.cvContourPerimeter;
import static org.bytedeco.javacpp.opencv_imgproc.cvDilate;
import static org.bytedeco.javacpp.opencv_imgproc.cvDrawContours;
import static org.bytedeco.javacpp.opencv_imgproc.cvEqualizeHist;
import static org.bytedeco.javacpp.opencv_imgproc.cvErode;
import static org.bytedeco.javacpp.opencv_imgproc.cvFindContours;
import static org.bytedeco.javacpp.opencv_imgproc.cvHoughCircles;
import static org.bytedeco.javacpp.opencv_imgproc.cvHoughLines2;
import static org.bytedeco.javacpp.opencv_imgproc.cvSmooth;

import java.io.File;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_core.CvContour;
import org.bytedeco.javacpp.opencv_core.CvMemStorage;
import org.bytedeco.javacpp.opencv_core.CvPoint;
import org.bytedeco.javacpp.opencv_core.CvPoint2D32f;
import org.bytedeco.javacpp.opencv_core.CvPoint3D32f;
import org.bytedeco.javacpp.opencv_core.CvSeq;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_imgcodecs;
import org.bytedeco.javacpp.helper.opencv_core.CvArr;

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
		 
		 @Override
			public String toString() {
				return (Math.round(getModule() * 100))+"_"+getAngle();
			}
	}
	
	static File dir = new File("resources");
	static File resDir = new File("resources/result");
	static final double SQUARE_WEIGTH = 1;
	static final double CIRCLE_WEIGTH = 1;
	static final double LINE_WEIGTH = 1;
	static final boolean saveCannyImage = false;
	static final boolean saveGrayImage = true;
	static final boolean saveWithLinesImage = false;
	
	

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
					
					Mat mat = imread(aux.getAbsolutePath(), opencv_imgcodecs.CV_LOAD_IMAGE_ANYDEPTH);
					IplImage img1 = makeImage(mat.clone());
					IplImage img2 = prepareImage(img1);
					
					Complex score = score(img1, img2);
					
					//two fraction digits
					String strScore = score.toString();
					System.out.println("Score:" + aux.getName() + "=>" + (strScore));
					
					if(saveGrayImage) {						
						File out2 = new File(resDir, "" + strScore + "gray.png");
						org.bytedeco.javacpp.opencv_imgcodecs.imwrite(out2.getAbsolutePath(), mat);						
					}
					
					if(saveWithLinesImage) {
						File out2 = new File(resDir, "" + strScore + "_withlines.png");
						org.bytedeco.javacpp.opencv_imgcodecs.cvSaveImage(out2.getAbsolutePath(),img1);
					}
					
					if(saveCannyImage) {
						File out2 = new File(resDir, "" + strScore + "_canny.png");
						org.bytedeco.javacpp.opencv_imgcodecs.cvSaveImage(out2.getAbsolutePath(),img2);						
					}
					
					sucessCount++;
				} catch (Exception e) {
					e.printStackTrace();
				}
				count++;
			}
		}
		System.out.println("Hits " + sucessCount + " From " + count);
	}
	
	public static IplImage makeImage(Mat imageMat) {
		GaussianBlur(imageMat, imageMat, new Size(3,3), 0);
		IplImage img1 = new IplImage(imageMat);
		return img1;
	}
	
	public static IplImage prepareImage(IplImage img1) {
		IplImage evalImag = img1.clone();	
		cvSmooth(evalImag, evalImag);
		cvErode(evalImag, evalImag);
		cvDilate(evalImag, evalImag);
		cvEqualizeHist(evalImag, evalImag);
		cvCanny(evalImag, evalImag, 1, 5, 3);
		
		return evalImag;
	}
	
	
	public static Complex score(IplImage img1, IplImage evalImag) {

		CvMemStorage mem = CvMemStorage.create();
		int imgWLimit = 540;
		int minDistance = evalImag.width() > imgWLimit ? 100 : 10;
		int higherTresh = evalImag.width() > imgWLimit ? 100 : 10;
		int minRadius = evalImag.width() > imgWLimit ? 30 : 15;
		int maxRadius = evalImag.width() > imgWLimit ? 500 : 150;
		int centerTrash = evalImag.width() > imgWLimit ? 100 : 50;
		
		CvSeq circles = cvHoughCircles(evalImag, // Input image
				mem, // Memory Storage
				CV_HOUGH_GRADIENT, // Detection method
				1, // Inverse ratio
				minDistance, // Minimum distance between the centers of the detected circles
				higherTresh, // Higher threshold for canny edge detector
				centerTrash, // Threshold at the center detection stage
				minRadius, // min radius
				maxRadius // max radius
		);		
		for(int i = 0; i < circles.total(); i++){
		      CvPoint3D32f circle = new CvPoint3D32f(cvGetSeqElem(circles, i));
		      CvPoint center = cvPointFrom32f(new CvPoint2D32f(circle.x() * 2.0f, circle.y() * 2.0f));
		      int radius = Math.round(circle.z() * 0.1f);      
		      cvCircle(img1, center, radius, cvScalar(0,100,0,0), 1, CV_AA, 1); 
		      circle.close();
		}
		
		double countCircle = circles.total();
		circles.close();		
		
		double countSquare = 0.0;
		CvSeq squares = new CvSeq();
		mem = CvMemStorage.create();
		cvFindContours(evalImag, mem, squares, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_CHAIN_APPROX_TC89_KCOS,
				cvPoint(0, 0));
		if (squares != null && !squares.isNull()) {
			CvSeq result = cvApproxPoly(squares, Loader.sizeof(CvContour.class), mem, CV_POLY_APPROX_DP,
					cvContourPerimeter(squares) * 0.02, 0);
			countSquare = result.total();
			cvDrawContours((CvArr)img1, squares, cvScalar(100,255,100,0), cvScalarAll(0), 1);
		}
		
		mem = CvMemStorage.create();
		CvSeq lines = cvHoughLines2(evalImag, mem, CV_HOUGH_STANDARD, 1, Math.PI / 180, 40, 1, 0, 0, CV_PI);
		
		double lineCount = lines.total();
		lines.close();
		
		Complex a = new Complex(lineCount * LINE_WEIGTH, countCircle * CIRCLE_WEIGTH);			
		Complex b = new Complex(lineCount * LINE_WEIGTH, countSquare * SQUARE_WEIGTH);
		
		Complex score = a.plus(b);
		
		return score;
	}
}