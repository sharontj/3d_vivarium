//****************************************************************************
//       Example Main Program for CS480 Programming Assignment 3
//****************************************************************************
// Description: This is a simple example program that demonstrates a teapot
//              spinning in a tank. 
// 
// 	     The following keys control the program:
// 
//              Q,q, <escape>: quit
//              R: reset viewing angle
//
//              Left mouse click + drag motion: rotate the tank 
// 
// 
//****************************************************************************
// History :
//   3 Mar 2008 Created by Tai-Peng Tian based on the C code by Stan Sclaroff
//


import javax.swing.*;
import java.awt.*;
import java.awt.event.*; 

import com.jogamp.opengl.util.gl2.GLUT;//for new version of gl

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.awt.GLCanvas;//for new version of gl
import javax.media.opengl.glu.GLU;
import com.jogamp.opengl.util.*;


public class PA3 extends JFrame
implements GLEventListener, KeyListener, MouseListener, MouseMotionListener
{
	private final int DEFAULT_WINDOW_WIDTH =512;
	private final int DEFAULT_WINDOW_HEIGHT=512;

	private GLCapabilities capabilities;
	private GLCanvas canvas;
	private FPSAnimator animator;
	private GLU glu;
	private GLUT glut;
	private Vivarium vivarium;
	private Quaternion viewing_quaternion; // world rotation controlled by mouse actions

	// State variables for the mouse actions
	int last_x, last_y;
	boolean rotate_world;

	public PA3()
	{

		capabilities = new GLCapabilities(null);
		capabilities.setDoubleBuffered(true);  // Enable Double buffering

		canvas  = new GLCanvas(capabilities);
		canvas.addGLEventListener(this);
		canvas.addMouseListener(this);
		canvas.addMouseMotionListener(this);
		canvas.addKeyListener(this);
		canvas.setAutoSwapBufferMode(true); // true by default. Just to be explicit
		getContentPane().add(canvas);

		animator = new FPSAnimator(canvas, 60); // drive the display loop @ 60 FPS

		glu  = new GLU();
		glut = new GLUT();

		setTitle("CS480/CS680 : Spinning Teapot");
		setSize( DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);

		last_x = last_y = 0;
		rotate_world = false;

		// Set initialization code for user created classes that involves OpenGL
		// calls after here. After this line, the opengGl context will be
		// correctly initialized.
		vivarium = new Vivarium(  );
		viewing_quaternion = new Quaternion();
	}

	public void run()
	{
		animator.start();
	}

	public static void main( String[] args )
	{
		PA3 P = new PA3();
		P.run();
	}

	//***************************************************************************
	//GLEventListener Interfaces
	//***************************************************************************
	//
	// Place all OpenGL related initialization here. Including display list
	// initialization for user created classes
	//
	public void init( GLAutoDrawable drawable) 
	{
		GL2 gl = (GL2)drawable.getGL();

		/* set up for shaded display of the vivarium*/
		float light0_position[] = {1,1,1,0};
		float light0_ambient_color[] = {0.25f,0.25f,0.25f,1};
		float light0_diffuse_color[] = {1,1,1,1};

		gl.glPolygonMode(GL2.GL_FRONT,GL2.GL_FILL);
		gl.glEnable(GL2.GL_COLOR_MATERIAL);
		gl.glColorMaterial(GL2.GL_FRONT,GL2.GL_AMBIENT_AND_DIFFUSE);

		gl.glDrawBuffer(GL2.GL_BACK);
		gl.glReadBuffer(GL2.GL_BACK);
		
		gl.glClearColor(0.0f,0.0f,0.0f,0.0f);
		gl.glClearAccum(0.0f, 0.0f, 0.0f, 0.0f);
		gl.glShadeModel(GL2.GL_SMOOTH);

		/* set up the light source */
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, light0_position, 0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, light0_ambient_color, 0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, light0_diffuse_color, 0);

		/* turn lighting and depth buffering on */
		gl.glEnable(GL2.GL_LIGHTING);
		gl.glEnable(GL2.GL_LIGHT0);
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glEnable(GL2.GL_NORMALIZE);

		/* turn on blending */
		gl.glEnable(GL2.GL_BLEND); /* allow blending to avoid blocking effect */
		//gl.glBlendFunc(GL2.GL_ONE,GL2.GL_SRC_ALPHA);
		gl.glDisable(GL2.GL_DEPTH_TEST);// Disable Depth Testing
		gl.glShadeModel (GL2.GL_SMOOTH);// Select Smooth Shading

		vivarium.init( gl );
	}

	// Redisplaying graphics
	public void display(GLAutoDrawable drawable)
	{
		GL2 gl = (GL2)drawable.getGL();

		// clear the display 
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glClear(GL2.GL_ACCUM_BUFFER_BIT);

//		gl.glColorMask(true, true, true, true);
//		gl.glColorMask(false, false, true, true);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();

		// rotate the world and then call world display list object 
		gl.glMultMatrixf( viewing_quaternion.to_matrix(), 0 );

		vivarium.update( gl );
		vivarium.draw( gl );
		gl.glColorMask(true, true, true, true);
		gl.glAccum(GL2.GL_LOAD, 1.0f);
		
		/*gl.glColorMask(true, true, true, true);
		gl.glColorMask(true, false, false, true);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glMultMatrixf( viewing_quaternion.to_matrix(), 0 );
		vivarium.update( gl );
		vivarium.draw( gl );
		gl.glColorMask(true, true, true, true);
		gl.glAccum(GL2.GL_ACCUM, 1.0f);
		gl.glAccum(GL2.GL_RETURN, 1.0f);*/
	}

	// Window size change
	public void reshape(GLAutoDrawable drawable, int x, int y, 
			int width, int height)
	{

		// Change viewport dimensions
		GL2 gl = (GL2)drawable.getGL();

		// Prevent a divide by zero, when window is too short (you cant make a
		// window of zero width).
		if(height == 0) height = 1;

		double ratio = 1.0f * width / height;

		// Reset the coordinate system before modifying 
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();

		// Set the viewport to be the entire window 
		gl.glViewport(0, 0, width, height);

		// Set the clipping volume 
		glu.gluPerspective(25,ratio,0.1,100);

		// Camera positioned at (0,0,6), look at point (0,0,0), Up Vector (0,1,0)
		glu.gluLookAt(0,0,12,0,0,0,0,1,0);

		gl.glMatrixMode(GL2.GL_MODELVIEW);

	}

	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged,
			boolean deviceChanged)
	{
	}


	//*********************************************** 
	//          KeyListener Interfaces
	//*********************************************** 
	public void keyTyped(KeyEvent key)
	{
		switch ( key.getKeyChar() ) {
		case 'Q' :
		case 'q' : new Thread() {
			public void run()
			{ animator.stop(); }
		}.start();
		System.exit(0);
		break;

		// set the viewing quaternion to 0 rotation 
		case 'R' :
		case 'r' : 
			viewing_quaternion.reset(); 
			break;
		case 'F':
		case 'f':
			vivarium.addFood();
			break;
		default :
			break;
		}
	}

	public void keyPressed(KeyEvent key)
	{
		switch (key.getKeyCode()) {
		case KeyEvent.VK_ESCAPE:
			new Thread()
			{
				public void run()
				{
					animator.stop();
				}
			}.start();
			System.exit(0);
			break;

		default:
			break;
		}
	}

	public void keyReleased(KeyEvent key)
	{
	}

	//************************************************** 
	// MouseListener and MouseMotionListener Interfaces
	//************************************************** 
	public void mouseClicked(MouseEvent mouse)
	{
	}

	public void mousePressed(MouseEvent mouse)
	{
		int button = mouse.getButton();
		if ( button == MouseEvent.BUTTON1 )
		{
			last_x = mouse.getX();
			last_y = mouse.getY();
			rotate_world = true;
		}
	}

	public void mouseReleased(MouseEvent mouse)
	{
		int button = mouse.getButton();
		if ( button == MouseEvent.BUTTON1 )
		{
			rotate_world = false;
		}
	}

	public void mouseMoved( MouseEvent mouse)
	{
	}

	public void mouseDragged( MouseEvent mouse )
	{
		if (rotate_world)
		{
			// vector in the direction of mouse motion
			int x = mouse.getX();
			int y = mouse.getY();
			float dx = x - last_x;
			float dy = y - last_y;

			// spin around axis by small delta
			float mag = (float) Math.sqrt( dx*dx + dy*dy );
			float[] axis = new float[3];
			axis[0] = dy/ mag;
			axis[1] = dx/ mag;
			axis[2] = 0.0f;

			// calculate appropriate quaternion
			float viewing_delta = 3.1415927f / 180.0f; // 1 degree
			float s = (float) Math.sin( 0.5f*viewing_delta );
			float c = (float) Math.cos( 0.5f*viewing_delta );

			Quaternion Q = new Quaternion( c, s*axis[0], s*axis[1], s*axis[2]);
			viewing_quaternion = Q.multiply( viewing_quaternion );

			// normalize to counteract acccumulating round-off error
			viewing_quaternion.normalize();

			// Save x, y as last x, y
			last_x = x;
			last_y = y;
		}
	}

	public void mouseEntered( MouseEvent mouse)
	{
	}

	public void mouseExited( MouseEvent mouse)
	{
	}

	public void dispose(GLAutoDrawable arg0) {
		// TODO Auto-generated method stub

	} 



}
