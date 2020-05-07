package com.galkins.model;

import java.applet.Applet;
import java.awt.Color;
import java.awt.Event;
import java.awt.Graphics;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamTokenizer;
import java.net.URL;

class FileFormatException extends Exception {
	private static final long serialVersionUID = 1L;

	public FileFormatException(String s) {
		super(s);
	}
}

class Model3D {
	float vert[];
	int tvert[];
	int nvert, maxvert;
	int con[];
	int ncon, maxcon;
	boolean transformed;
	Matrix mat;

	float xmin, xmax, ymin, ymax, zmin, zmax;

	Model3D() {
		mat = new Matrix();
		mat.xrot(20);
		mat.yrot(30);
	}

	Model3D(InputStream is) throws IOException, FileFormatException {
		this();
		@SuppressWarnings("deprecation")
		StreamTokenizer st = new StreamTokenizer(is);
		st.eolIsSignificant(true);
		st.commentChar('#');
		scan: while (true) {
			switch (st.nextToken()) {
			default:
				break scan;
			case StreamTokenizer.TT_EOL:
				break;
			case StreamTokenizer.TT_WORD:
				if ("v".equals(st.sval)) {
					double x = 0, y = 0, z = 0;
					if (st.nextToken() == StreamTokenizer.TT_NUMBER) {
						x = st.nval;
						if (st.nextToken() == StreamTokenizer.TT_NUMBER) {
							y = st.nval;
							if (st.nextToken() == StreamTokenizer.TT_NUMBER)
								z = st.nval;
						}
					}
					addVert((float) x, (float) y, (float) z);
					while (st.ttype != StreamTokenizer.TT_EOL
							&& st.ttype != StreamTokenizer.TT_EOF)
						st.nextToken();
				} else if ("f".equals(st.sval) || "fo".equals(st.sval)
						|| "l".equals(st.sval)) {
					int start = -1;
					int prev = -1;
					int n = -1;
					while (true)
						if (st.nextToken() == StreamTokenizer.TT_NUMBER) {
							n = (int) st.nval;
							if (prev >= 0)
								add(prev - 1, n - 1);
							if (start < 0)
								start = n;
							prev = n;
						} else if (st.ttype == '/')
							st.nextToken();
						else
							break;
					if (start >= 0)
						add(start - 1, prev - 1);
					if (st.ttype != StreamTokenizer.TT_EOL)
						break scan;
				} else {
					while (st.nextToken() != StreamTokenizer.TT_EOL
							&& st.ttype != StreamTokenizer.TT_EOF)
						;
				}
			}
		}
		is.close();
		if (st.ttype != StreamTokenizer.TT_EOF)
			throw new FileFormatException(st.toString());
	}

	int addVert(float x, float y, float z) {
		int i = nvert;
		if (i >= maxvert)
			if (vert == null) {
				maxvert = 100;
				vert = new float[maxvert * 3];
			} else {
				maxvert *= 2;
				float nv[] = new float[maxvert * 3];
				System.arraycopy(vert, 0, nv, 0, vert.length);
				vert = nv;
			}
		i *= 3;
		vert[i] = x;
		vert[i + 1] = y;
		vert[i + 2] = z;
		return nvert++;
	}

	void add(int p1, int p2) {
		int i = ncon;
		if (p1 >= nvert || p2 >= nvert)
			return;
		if (i >= maxcon)
			if (con == null) {
				maxcon = 100;
				con = new int[maxcon];
			} else {
				maxcon *= 2;
				int nv[] = new int[maxcon];
				System.arraycopy(con, 0, nv, 0, con.length);
				con = nv;
			}
		if (p1 > p2) {
			int t = p1;
			p1 = p2;
			p2 = t;
		}
		con[i] = (p1 << 16) | p2;
		ncon = i + 1;
	}

	void transform() {
		if (transformed || nvert <= 0)
			return;
		if (tvert == null || tvert.length < nvert * 3)
			tvert = new int[nvert * 3];
		mat.transform(vert, tvert, nvert);
	}

	private void sort(int lo0, int hi0) {
		int a[] = con;
		int lo = lo0;
		int hi = hi0;
		if (lo >= hi)
			return;
		int mid = a[(lo + hi) / 2];
		while (lo < hi) {
			while (lo < hi && a[lo] < mid) {
				lo++;
			}
			while (lo < hi && a[hi] >= mid) {
				hi--;
			}
			if (lo < hi) {
				int T = a[lo];
				a[lo] = a[hi];
				a[hi] = T;
			}
		}
		if (hi < lo) {
			int T = hi;
			hi = lo;
			lo = T;
		}
		sort(lo0, lo);
		sort(lo == lo0 ? lo + 1 : lo, hi0);
	}

	void compress() {
		int limit = ncon;
		int c[] = con;
		sort(0, ncon - 1);
		int d = 0;
		int pp1 = -1;
		for (int i = 0; i < limit; i++) {
			int p1 = c[i];
			if (pp1 != p1) {
				c[d] = p1;
				d++;
			}
			pp1 = p1;
		}
		ncon = d;
	}

	static Color gr[];

	void paint(Graphics g) {
		if (vert == null || nvert <= 0)
			return;
		transform();
		if (gr == null) {
			gr = new Color[16];
			for (int i = 0; i < 16; i++) {
				int black = (int) (170 * (1 - Math.pow(i / 15.0, 0)));
				gr[i] = new Color(black, black, black);
			}
		}
		int lg = 0;
		int lim = ncon;
		int c[] = con;
		int v[] = tvert;
		if (lim <= 0 || nvert <= 0)
			return;
		for (int i = 0; i < lim; i++) {
			int T = c[i];
			int p1 = ((T >> 16) & 0xFFFF) * 3;
			int p2 = (T & 0xFFFF) * 3;
			int black = v[p1 + 2] + v[p2 + 2];
			if (black < 0)
				black = 0;
			if (black > 15)
				black = 15;
			if (black != lg) {
				lg = black;
				g.setColor(gr[black]);
			}
			g.drawLine(v[p1], v[p1 + 1], v[p2], v[p2 + 1]);
		}
	}

	void findBB() {
		float v[] = vert;
		float xmin = v[0], xmax = xmin;
		float ymin = v[1], ymax = ymin;
		float zmin = v[2], zmax = zmin;
		for (int i = nvert * 3; (i -= 3) > 0;) {
			float x = v[i];
			if (x < xmin)
				xmin = x;
			if (x > xmax)
				xmax = x;
			float y = v[i + 1];
			if (y < ymin)
				ymin = y;
			if (y > ymax)
				ymax = y;
			float z = v[i + 2];
			if (z < zmin)
				zmin = z;
			if (z > zmax)
				zmax = z;
		}
		this.xmax = xmax;
		this.xmin = xmin;
		this.ymax = ymax;
		this.ymin = ymin;
		this.zmax = zmax;
		this.zmin = zmin;
	}
}

public class Model extends Applet implements Runnable {
	private static final long serialVersionUID = 1L;
	Model3D md;
	float xfac;
	int prevx, prevy;
	float xtheta, ytheta;
	Matrix amat = new Matrix(), tmat = new Matrix();
	String mdname = null;
	int width = 1000, height = 1000;

	public void init() {
		mdname = "models/cube.obj";
	}

	public void run() {
		InputStream is = null;
		try {
			is = new URL("file", "", mdname).openStream();
			Model3D m = new Model3D(is);
			md = m;
			m.findBB();
			m.compress();
			float xw = m.xmax - m.xmin;
			float f1 = width / xw;
			float f2 = height / xw;
			xfac = 0.7f * (f1 < f2 ? f1 : f2);
		} catch (Exception e) {}
		try {
			if (is != null)
				is.close();
		} catch (Exception e) {
		}
	}

	public void start() {
		new Thread(this).start();
	}

	public boolean mouseDrag(Event e, int x, int y) {
		tmat.unit();
		float xtheta = (prevy - y) * 360.0f / width;
		float ytheta = (x - prevx) * 360.0f / height;
		tmat.xrot(xtheta);
		tmat.yrot(ytheta);
		amat.mult(tmat);
		repaint();
		prevx = x;
		prevy = y;
		return true;
	}

	public void paint(Graphics g) {
		if (md != null) {
			md.mat.unit();
			md.mat.translate(-(md.xmin + md.xmax) / 2,
					-(md.ymin + md.ymax) / 2, -(md.zmin + md.zmax) / 2);
			md.mat.mult(amat);
			md.mat.scale(xfac, -xfac, xfac);
			md.mat.translate(width, height / 2, 0);
			md.paint(g);
		}
	}
}