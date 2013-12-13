package org.cornellASL.ltlmoplocalize;

/*
 * Author: Abhishek Sriraman
 * Last Modified: 12/12/13
 */

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import com.ASL.ltlmoplocalize.R;

import android.hardware.Camera.Size;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/*
 Main activity involved with all image processing. On every frame, 4-sided contours are found for Marker color and Robot color. 

 Users can click on a contour to increment it's ID number. When all markers and all robot iD's have been defined, UDP message
 containing relevant information is sent to host computer. 

 If camera is stationary, users can "LOCK" marker tags once marker tags have been defined. This assumes the marker tags are not moving in
 the image coordinates (they shouldn't be if the camera is stationary) and by-passes thresholding for the Marker color. This in turn increases
 frame rate. 

 Majorty of processing occurs in onCameraFrame()
 */
public class MainProcess extends Activity implements CvCameraViewListener2,
		OnTouchListener {
	private ProcessView mOpenCvCameraView;

	private ColorBlobDetector c1Detector;
	private ColorBlobDetector c2Detector;

	private List<Size> mResolutionList;
	private MenuItem[] mResolutionMenuItems;
	private SubMenu mResolutionMenu;

	private static final String TAG = "MainProcess";
	private String RobotColor, MarkerColor, UDP_IP_ADDRESS;

	String[] markerinfo;

	private Mat mRgba, mHSVMat, mContours;

	private List<MatOfPoint> c1_contours, c2_contours;

	private Scalar colorRed, colorGreen, colorBlue, robotHSV, markerHSV;

	private List<Point> pts;
	private List<Marker> allMarkers;
	private List<Integer> inactive;

	private All_ID markers;

	private int idx, UDP_PORTNUM, iContourAreaMin = 1000, iLineThickness = 3,
			contOfInterest, touchFlag, movement_threshold = 50,
			touch_thresh = 50, robot_threshold = 50, locked = 0;

	private double val, sq_x, sq_y, base_x, base_y, sq_rat, base_rat, xOffset,
			yOffset, x_mod, y_mod, x_touch, y_touch;

	private byte[] byteColourTrackCentreHue;

	private Point line_Intersec, lastClick;

	private MatOfPoint2f mMOP2f1, mMOP2f2;

	private WifiManager wifiManager;

	//Required for initializing OpenCV libraries on Android
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV Loaded Successfully");
				mOpenCvCameraView.enableView();
				mOpenCvCameraView.setOnTouchListener(MainProcess.this);
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;

			}// End Switch
		} // End OnManagerConnected
	};// End BaseLoaderCallback

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.process);

		mOpenCvCameraView = (ProcessView) findViewById(R.id.main_process_view);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);

		wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
		markerinfo = new String[4];
		loadPref();

		final Button lockButton = (Button) findViewById(R.id.lock);
		lockButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (locked == 0) {
					if (markers.allMarkDefined() > 0) {
						locked = 1;
					} else {
						Toast.makeText(getApplicationContext(),
								"All Markers Not Defined", Toast.LENGTH_SHORT)
								.show();
					}
				} else if (locked == 1) {
					locked = 0;
				}

			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mResolutionMenu = menu.addSubMenu("Resolution");
		mResolutionList = mOpenCvCameraView.getResolutionList();
		Log.d(TAG, "" + mResolutionList.size());
		mResolutionMenuItems = new MenuItem[mResolutionList.size()];

		ListIterator<Size> resolutionItr = mResolutionList.listIterator();
		int idx = 0;
		while (resolutionItr.hasNext()) {
			Size element = resolutionItr.next();
			mResolutionMenuItems[idx] = mResolutionMenu.add(1, idx, Menu.NONE,
					Integer.valueOf(element.width).toString() + "x"
							+ Integer.valueOf(element.height).toString());
			idx++;
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getGroupId() == 1) {
			int id = item.getItemId();
			Size resolution = mResolutionList.get(id);
			mOpenCvCameraView.setResolution(resolution);
			resolution = mOpenCvCameraView.getResolution();
			String caption = Integer.valueOf(resolution.width).toString() + "x"
					+ Integer.valueOf(resolution.height).toString();
			Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
		}

		return true;
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null) {
			mOpenCvCameraView.disableView();
		}
	}

	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null) {
			mOpenCvCameraView.disableView();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this,
				mLoaderCallback);
	}

	// All required variables are initialized
	@Override
	public void onCameraViewStarted(int width, int height) {
		byteColourTrackCentreHue = new byte[3];
		byteColourTrackCentreHue[0] = 27;
		byteColourTrackCentreHue[1] = 100;
		byteColourTrackCentreHue[2] = (byte) 255;

		c1Detector = new ColorBlobDetector(2);
		c1Detector.setHsvColor(markerHSV);

		c2Detector = new ColorBlobDetector(2);
		c2Detector.setHsvColor(robotHSV);

		mRgba = new Mat(height, width, CvType.CV_8UC4);
		mHSVMat = new Mat();
		mContours = new Mat();

		c1_contours = new ArrayList<MatOfPoint>();
		c2_contours = new ArrayList<MatOfPoint>();

		colorRed = new Scalar(255, 0, 0, 255);
		colorGreen = new Scalar(0, 255, 0, 255);
		colorBlue = new Scalar(0, 0, 255, 255);

		pts = new ArrayList<Point>();

		markers = new All_ID(movement_threshold, robot_threshold, wifiManager,
				UDP_IP_ADDRESS, UDP_PORTNUM);

		line_Intersec = new Point(0, 0);
		lastClick = new Point(0, 0);

		mMOP2f1 = new MatOfPoint2f();
		mMOP2f2 = new MatOfPoint2f();

	}

	@Override
	public void onCameraViewStopped() {
		releaseMats();
	}

	public void releaseMats() {
		mRgba.release();
		mHSVMat.release();
		mContours.release();
	}

	/*
	 Image is thresholded for 2 colors: Robot color and Marker Color.
	 Once thresholded, contours are found and are fitted to polygons.
	 4-sided polygons above a minimum contour area (defined in ColorBlobDetector) are displayed on screen.
	 If the user touched near a polygon, the polygon is considered a MARKER and it's ID is incremented. 
	 	->Color of tag defines whether the marker is a regular MARKER or a robot MARKER.
	*/
	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		mRgba = inputFrame.rgba();
		c1_contours.clear();
		c2_contours.clear();
		c1Detector.process(mRgba);
		c2Detector.process(mRgba);
		c1_contours = c1Detector.getContours(); //color 1 detector - Markers
		c2_contours = c2Detector.getContours();// color 2 detector - Robots

		contOfInterest = 0;

		for (int i = 0; i < markers.allRobots.size(); i++) { // Set all Robot Markers to NOT Live
			markers.allRobots.get(i).resetLive();
		}
		if (locked == 0) {
			for (int i = 0; i < markers.allMarkers.size(); i++) { // Set all markers to NOT Live
				markers.allMarkers.get(i).resetLive();
			}

			for (idx = 0; idx < c1_contours.size(); idx++) { //For each of the contours found (for color 1)
				c1_contours.get(idx).convertTo(mMOP2f1, CvType.CV_32FC2);
				
				//Contour approximated to polygon
				Imgproc.approxPolyDP(mMOP2f1, mMOP2f2, 15, true);

				// convert back to MatOfPoint and put it back in the list
				mMOP2f2.convertTo(c1_contours.get(idx), CvType.CV_32S);

				if (c1_contours.get(idx).rows() == 4) { // Check to see if polygon is 4-sided
					contOfInterest = contOfInterest + 1;
					Converters.Mat_to_vector_Point2f(c1_contours.get(idx), pts);

					Imgproc.drawContours(mRgba, c1_contours, idx, colorRed,
							iLineThickness);

					Core.line(mRgba, pts.get(0), pts.get(2), colorRed,
							iLineThickness);
					Core.line(mRgba, pts.get(1), pts.get(3), colorRed,
							iLineThickness);

					//Find center of contour 
					line_Intersec = findIntersec(pts.get(0), pts.get(2),
							pts.get(1), pts.get(3));

					// If markers have previously been ID'd, the following code
					// matches current found contours to the previously ID'd
					// contours
					if (!(markers.allMarkers.isEmpty())) {
						int temp = markers.closeToMarker(line_Intersec);
						if (temp >= 0) {
							markers.allMarkers.get(temp)
									.setPoint(line_Intersec);
							markers.allMarkers.get(temp).setLive();
						}

					}
					if (touchFlag == 1) {
						if (dist(line_Intersec, lastClick) < touch_thresh) { // This ensures touch is near
																				// SOME valid contour of interest
							// Contour of Interest may or may not be in allMarkers
							markers.incrementID(line_Intersec);
							touchFlag = 0;
						}
					}// end if(touchFlag)
				}// end if(contour is 4sided)
			}// end for each contour
				
			// Clear all markers that are not LIVE
			for (int i = (markers.allMarkers.size() - 1); i >= 0; i--) {
				if (markers.allMarkers.get(i).getLive() == 0) {
					markers.allMarkers.remove(i);
				}
			}
		}
		
		//Repeats above process for new color
		for (idx = 0; idx < c2_contours.size(); idx++) {
			c2_contours.get(idx).convertTo(mMOP2f1, CvType.CV_32FC2);
			Imgproc.approxPolyDP(mMOP2f1, mMOP2f2, 15, true);

			// convert back to MatOfPoint and put it back in the list
			mMOP2f2.convertTo(c2_contours.get(idx), CvType.CV_32S);

			if (c2_contours.get(idx).rows() == 4) {
				contOfInterest = contOfInterest + 1;
				Converters.Mat_to_vector_Point2f(c2_contours.get(idx), pts);

				Imgproc.drawContours(mRgba, c2_contours, idx, colorRed,
						iLineThickness);

				Core.line(mRgba, pts.get(0), pts.get(2), colorRed,
						iLineThickness);
				Core.line(mRgba, pts.get(1), pts.get(3), colorRed,
						iLineThickness);

				line_Intersec = findIntersec(pts.get(0), pts.get(2),
						pts.get(1), pts.get(3));

				if (!(markers.allRobots.isEmpty())) {
					int temp = markers.closeToRobot(line_Intersec);
					if (temp >= 0) {
						markers.allRobots.get(temp).setPoint(line_Intersec);
						markers.allRobots.get(temp).setLive();
					}
				}
				if (touchFlag == 1) {
					if (dist(line_Intersec, lastClick) < touch_thresh) { // This ensures touch is near
																		// SOME valid contour of interest
						
						markers.incrementRb(line_Intersec);
						touchFlag = 0;
					}
				}
			} 
		}

		// Clear all robot markers that are not LIVE
		for (int i = (markers.allRobots.size() - 1); i >= 0; i--) {
			if (markers.allRobots.get(i).getLive() == 0) {
				markers.allRobots.remove(i);
			}
		}

		//For each marker and robot tag on the screen, print the ID number on the frame next to contour
		for (int i = 0; i < markers.allMarkers.size(); i++) {
			addNum("" + markers.allMarkers.get(i).getID(), markers.allMarkers
					.get(i).getPoint());
		}

		for (int i = 0; i < markers.allRobots.size(); i++) {
			addNum("" + markers.allRobots.get(i).getID(), markers.allRobots
					.get(i).getPoint());
		}

		
		if (!(markers.allMarkDefined() > 0)) {
			markers.resetPass(1);
			//Log.d(TAG, "markers not defined");
		}

		if (!(markers.allRobotDefined() > 0)) {
			markers.resetPass(2);
			//Log.d(TAG, "robots not defined");
		}

		//Check to see if all tags are properly defined
		if ((markers.allRobotDefined() > 0) && (markers.allMarkDefined() > 0)) {
			//Log.d(TAG, "all defined!");
			bindAnchors();
			markers.sendInfo(1); //Send UDP Message with Marker information
		} else {
			markers.sendInfo(2); //Send UDP Message with Null information
		}

		return mRgba;
	}

	//Find distance between two points
	public double dist(Point p1, Point p2) {
		double dist_val;
		dist_val = Math.sqrt(Math.pow((p2.y - p1.y), 2)
				+ Math.pow((p2.x - p1.x), 2));

		return dist_val;
	}
	//Find point of intersection between a line defined by (p1, p2)  and (p3, p4)
	public Point findIntersec(Point p1, Point p2, Point p3, Point p4) {
		double m1 = (p2.y - p1.y) / (p2.x - p1.x);
		double m2 = (p4.y - p3.y) / (p4.x - p3.x);

		double x_int = (p3.y - m2 * p3.x + m1 * p1.x - p1.y) / (m1 - m2);
		double y_int = m1 * x_int + p1.y - m1 * p1.x;
		Point intersec = new Point(x_int, y_int);

		return intersec;
	}

	//Each MARKER in markers.allMarkers has a field in it that stores the marker's corresponding Anchor point. 
	//This method add the correct anchor point to each marker
	//i.e. The values of Anchor#2 will be added to the marker with id=2
	public void bindAnchors() {
		for (int j = 0; j < 4; j++) {
			String vals[] = markerinfo[j].split(",");
			markers.allMarkers.get(markers.pass_m[j]).setAnchor(
					new Point(Double.parseDouble(vals[0]), Double
							.parseDouble(vals[1])));
		}
	}

	//Save location of user's touch to lastClick
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		//Number of pixels in each frame grabbed by OpenCV - defined by user
		sq_x = mRgba.cols(); // sq_y;
		sq_y = mRgba.rows(); // sq_x;
		
		//Number of pixels of screen - based on Device. 
		base_x = mOpenCvCameraView.getWidth();
		base_y = mOpenCvCameraView.getHeight();

		//The mRgba image is automatically fitted to the screens size of the device
		//The following scales the coordinates of the touch relative 
		//to the Device screen to the coordinates of the OpenCV frame
		sq_rat = sq_x / sq_y;
		base_rat = base_x / base_y;

		if (sq_rat > base_rat) {
			x_mod = base_x;
			y_mod = base_x * (sq_y / sq_x);
		} else {
			y_mod = base_y;
			x_mod = base_y * (sq_rat);
		}

		xOffset = (base_x - x_mod) / 2;
		yOffset = (base_y - y_mod) / 2;

		x_touch = (event.getX() - xOffset) * (sq_x / x_mod);
		y_touch = (event.getY() - yOffset) * (sq_y / y_mod);
		lastClick = new Point(x_touch, y_touch);

		touchFlag = 1;
		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		loadPref();
	}

	//Add the string 's' to location "tagLoc" on current image (mRgba)
	private void addNum(String s, Point tagLoc) {
		Core.putText(mRgba, s, tagLoc, Core.FONT_HERSHEY_SIMPLEX, 1.2,
				colorBlue, 2);
	}

	private void loadPref() {
		SharedPreferences mSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		RobotColor = mSharedPreferences
				.getString("RobotColor", "151,255,174,0");
		MarkerColor = mSharedPreferences.getString("MarkerColor",
				"49,229,221,0");
		markerHSV = conv2Scalar(MarkerColor);
		robotHSV = conv2Scalar(RobotColor);

		UDP_IP_ADDRESS = mSharedPreferences.getString("UDP_ip_add",
				"192.169.10.1");
		UDP_PORTNUM = Integer.parseInt(mSharedPreferences.getString(
				"UDP_port_num", "5005"));

		markerinfo[0] = mSharedPreferences.getString("marker_1", "0,0");
		markerinfo[1] = mSharedPreferences.getString("marker_2", "0,10");
		markerinfo[2] = mSharedPreferences.getString("marker_3", "10,10");
		markerinfo[3] = mSharedPreferences.getString("marker_4", "10,0");

	}
	
	//Return a scalar value from a string of the format "x,x,x,x"
	private Scalar conv2Scalar(String input) {
		String[] vals = input.split(",");
		Log.d(TAG, "Vals 1: " + Double.parseDouble(vals[0]));
		return (new Scalar(Float.parseFloat(vals[0]),
				Float.parseFloat(vals[1]), Float.parseFloat(vals[2]),
				Float.parseFloat(vals[3])));
	}

}
