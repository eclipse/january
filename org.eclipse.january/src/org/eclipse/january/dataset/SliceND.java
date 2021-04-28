/*-
 * Copyright (c) 2014, 2016 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.january.dataset;

import java.util.Arrays;

/**	
 * The {@code SliceND} class represents a slice through all dimensions of a multi-dimensional {@link org.eclipse.january.dataset.Dataset}.<br><br>
 * A slice comprises a starting position array, a stopping position array (not included) and a stepping size array.<br>
 * If a maximum shape is specified, slicing past the original shape is supported for positive
 * steps otherwise it is ignored. With unlimited dimensions, extending past the original shape is only
 * allowed if the stopping value is given.
 */
public class SliceND {
	private int[] lstart;
	private int[] lstop;
	private int[] lstep;
	private transient int[] lshape; // latest shape
	private int[] oshape; // source or original shape
	private int[] mshape; // max shape

	private boolean expanded;

	private static int[] copy(int[] in) {
		return in == null ? null : in.clone();
	}

	/**
	 * Construct a nD Slice for whole of shape.
	 * 
	 * @param shape
	 *            Shape of the dataset, see {@link ILazyDataset#getShape()}
	 */
	public SliceND(final int[] shape) {
		final int rank = shape == null ? 0 : shape.length;
		lstart = new int[rank];
		lstop = copy(shape);
		lstep = new int[rank];
		Arrays.fill(lstep, 1);
		lshape = copy(shape);
		oshape = copy(shape);
		mshape = oshape;
		expanded = false;
	}

	/**
	 * Construct a nD Slice from an array of 1D slices.
	 * 
	 * @param shape
	 *            Shape of the dataset, see {@link ILazyDataset#getShape()}
	 * @param slice
	 *            Slice for each dimension of ND slice
	 */
	public SliceND(final int[] shape, Slice... slice) {
		this(shape, null, slice);
	}

	/**
	 * Construct a nD Slice from an array of 1D slices, if the maxShape is
	 * {@code null}, it will be set to the maximum shape of the nD Slice.
	 * 
	 * @param shape
	 *            Shape of the dataset, see {@link ILazyDataset#getShape()}
	 * @param maxShape,
	 *            may be {@code null}
	 * @param slice
	 *            Slice for each dimension of ND slice
	 */
	public SliceND(final int[] shape, final int[] maxShape, Slice... slice) {
		this(shape);

		if (maxShape != null) {
			initMaxShape(maxShape);
		}

		if (slice != null) {
			final int length = slice.length;
			final int rank = shape.length;
			if (length > rank) {
				throw new IllegalArgumentException("More slices have been specified than rank of shape");
			}
			for (int i = 0; i < length; i++) {
				Slice s = slice[i];
				if (s != null) {
					setSlice(i, s);
				}
			}
		}
	}

	private void initMaxShape(int[] maxShape) {
		final int rank = oshape.length;
		if (maxShape.length != rank) {
			throw new IllegalArgumentException("Maximum shape must have same rank as shape");
		}
		mshape = maxShape.clone();
		for (int i = 0; i < rank; i++) {
			int m = mshape[i];
			if (m != ILazyWriteableDataset.UNLIMITED && m < oshape[i]) {
				throw new IllegalArgumentException("Maximum shape must be greater than or equal to shape");
			}
		}
	}

	/**
	 * Construct a nD Slice from parameters, if the maxShape is {@code null}, it
	 * will be set to the maximum shape of the nD Slice, the start will be set
	 * to 0, stop is by default equal to the entire size of the set, step is
	 * defaultly set to 1.
	 * 
	 * @param shape
	 *            Shape of the dataset, see {@link ILazyDataset#getShape()}
	 * @param start
	 *            Array of starts points, may be {@code null}
	 * @param stop
	 *            Array of stops points, may be {@code null}
	 * @param step
	 *            Array of steps, may be {@code null}
	 */
	public SliceND(final int[] shape, final int[] start, final int[] stop, final int[] step) {
		this(shape, null, start, stop, step);
	}

	/**
	 * Construct a nD Slice from parameters, if the maxShape is {@code null}, it
	 * will be set to the maximum shape of the nD Slice, the start will be set
	 * to 0, stop is by default equal to the entire size of the set, step is
	 * defaultly set to 1.
	 * 
	 * @param shape
	 *            Shape of the dataset, see {@link ILazyDataset#getShape()}
	 * @param maxShape
	 *            Array of maximals shapes, may be {@code null}
	 * @param start
	 *            Array of starts points, may be {@code null}
	 * @param stop
	 *            Array of stops points, may be {@code null}
	 * @param step
	 *            Array of steps, may be {@code null}
	 */
	public SliceND(final int[] shape, final int[] maxShape, final int[] start, final int[] stop, final int[] step) {
		// number of steps, or new shape, taken in each dimension is
		// shape = (stop - start + step - 1) / step if step > 0
		// (stop - start + step + 1) / step if step < 0
		//
		// thus the final index in each dimension is
		// start + (shape-1)*step
		final int rank = shape == null ? 0 : shape.length;

		if (start == null) {
			lstart = new int[rank];
		} else {
			lstart = start.clone();
		}
		if (stop == null) {
			lstop = new int[rank];
		} else {
			lstop = stop.clone();
		}
		if (step == null) {
			lstep = new int[rank];
			Arrays.fill(lstep, 1);
		} else {
			lstep = step.clone();
		}

		if (lstart.length != rank || lstop.length != rank || lstep.length != rank) {
			throw new IllegalArgumentException("No of indexes does not match data dimensions: you passed it start="
					+ lstart.length + ", stop=" + lstop.length + ", step=" + lstep.length + ", and it needs " + rank);
		}

		lshape = new int[rank];
		oshape = copy(shape);
		if (maxShape == null) {
			mshape = oshape;
		} else {
			initMaxShape(maxShape);
		}

		for (int i = 0; i < rank; i++) {
			internalSetSlice(i, start == null ? null : lstart[i], stop == null ? null : lstop[i], lstep[i]);
		}
	}

	/**
	 * Set slice for given dimension, if the start is {@code null} it will be
	 * set to 0, stop is by default equal to the entire size of the set.
	 * 
	 * @param i
	 *            dimension
	 * @param start
	 *            Start point, may be {@code null} to imply start of dimension
	 * @param stop
	 *            Stop point, may be {@code null} to imply end of dimension
	 * @param step
	 *            Slice step
	 */
	public void setSlice(int i, Integer start, Integer stop, int step) {
		internalSetSlice(i, start, stop, step);
	}

	/**
	 * Set slice for given dimension, if the start is {@code null} it will be
	 * set to 0, stop is by default equal to the entire size of the set.
	 * 
	 * @param i
	 *            dimension
	 * @param start
	 *            Start point, may be {@code null} to imply start of dimension
	 * @param stop
	 *            Stop point, may be {@code null} to imply end of dimension
	 * @param step
	 *            Slice step
	 */
	public void setSlice(int i, int start, int stop, int step) {
		internalSetSlice(i, start, stop, step);
	}

	/**
	 * Set slice for given dimension.
	 * 
	 * @param i
	 *            Dimension
	 * @param slice
	 *            Slice with wanted properties to set
	 * @since 2.0
	 */
	public void setSlice(int i, Slice slice) {
		internalSetSlice(i, slice.getStart(), slice.getStop(), slice.getStep());
	}

	/**
	 * Set slice for given dimension, if the start is {@code null} it will be
	 * set to 0, stop is by default equal to the entire size of the set.
	 * 
	 * @param i
	 *            dimension
	 * @param start
	 *            Start point, may be {@code null} to imply start of dimension
	 * @param stop
	 *            Stop point, may be {@code null} to imply end of dimension
	 * @param step
	 *            Slice step
	 */
	private void internalSetSlice(int i, Integer start, Integer stop, int step) {
		if (oshape == null) {
			throw new IllegalArgumentException("Cannot set slice on null dataset");
		}
		if (step == 0) {
			throw new IllegalArgumentException("Step size must not be zero");
		}
		i = ShapeUtils.checkAxis(oshape.length, i);
		final int s = oshape[i];
		final int m = mshape[i];

		if (start == null) {
			start = step > 0 ? 0 : s - 1;
		} else if (start < 0) {
			start += s;
		}
		if (step > 0) {
			if (start < 0) {
				start = 0;
			} else if (start > s) {
				if (m == s) {
					start = s;
				} else if (m != ILazyWriteableDataset.UNLIMITED && start > m) {
					start = m;
				}
			}

			if (stop == null) {
				if (start >= s && m == ILazyWriteableDataset.UNLIMITED) {
					throw new IllegalArgumentException(
							"To extend past current dimension in unlimited case, a stop value must be specified");
				}
				stop = s;
			} else if (stop < 0) {
				stop += s;
			}
			if (stop < 0) {
				stop = 0;
			} else if (stop > s) {
				if (m == s) {
					stop = s;
				} else if (m != ILazyWriteableDataset.UNLIMITED && stop > m) {
					stop = m;
				}
			}

			if (start >= stop) {
				if (start < s || m == s) {
					lstop[i] = start;
				} else { // override end
					stop = start + step;
					if (m != ILazyWriteableDataset.UNLIMITED && stop > m) {
						stop = m;
					}
					lstop[i] = stop;
				}
			} else {
				lstop[i] = stop;
			}

			if (lstop[i] > s) {
				oshape[i] = lstop[i];
				expanded = true;
			}
		} else {
			if (start < 0) {
				start = -1;
			} else if (start >= s) {
				start = s - 1;
			}

			if (stop == null) {
				stop = -1;
			} else if (stop < 0) {
				stop += s;
			}
			if (stop < -1) {
				stop = -1;
			} else if (stop >= s) {
				stop = s - 1;
			}
			if (stop >= start) {
				lstop[i] = start;
			} else {
				lstop[i] = stop;
			}
		}

		stop = lstop[i];
		lshape[i] = calcLength(start, stop, step);
		lstart[i] = start;
		lstep[i] = step;
	}

	private static int calcLength(int start, int stop, int step) {
		int l;
		if (start == stop) {
			l = 0;
		} else if (step > 0) {
			l = Math.max(0, (stop - start - 1) / step + 1);
		} else {
			l = Math.max(0, (stop - start + 1) / step + 1);
		}
		return l;
	}

	/**
	 * Returns an array of shapes of the source Dataset (this can change for
	 * dynamic Datasets).
	 * 
	 * @return shape of source Dataset
	 */
	public int[] getSourceShape() {
		return oshape;
	}

	/**
	 * Returns an array of maximals shapes
	 * 
	 * @return maximum shape
	 */
	public int[] getMaxShape() {
		return mshape;
	}

	/**
	 * Returns {@code true} if the slice makes shape larger, else {@code false}.
	 * 
	 * @return {@code true} if slice makes shape larger, {@code false} in the
	 *         other case
	 */
	public boolean isExpanded() {
		return expanded;
	}

	/**
	 * Returns an array of resulting shapes (this can change if the start, stop,
	 * step arrays are changed).
	 * 
	 * @return resulting shape
	 */
	public int[] getShape() {
		return lshape;
	}

	/**
	 * Returns an array of the starts values.
	 * 
	 * @return start values
	 */
	public int[] getStart() {
		return lstart;
	}

	/**
	 * Returns an array of stops values.
	 * <p>
	 * Note : stop values are clamped to -1 for <b>negative</b> steps
	 * </p>
	 * 
	 * @return stop values
	 */
	public int[] getStop() {
		return lstop;
	}

	/**
	 * Returns an array of the steps values.
	 * 
	 * @return step values
	 */
	public int[] getStep() {
		return lstep;
	}

	/**
	 * Update source shape (for use with dynamic datasets
	 * @param shape
	 * @since 2.3
	 */
	public void updateSourceShape(int... shape) {
		if (shape != null) {
			if (oshape != null) {
				if (shape.length != oshape.length) {
					throw new IllegalArgumentException("Updated source shape must have same rank");
				}
				for (int i = 0; i < shape.length; i++) {
					int s = shape[i];
					if (!isSliceWithinShape(s, lstart[i], lstop[i], lstep[i])) {
						throw new IllegalArgumentException("Updated source shape must be outside latest slice");
					}
				}
				System.arraycopy(shape, 0, oshape, 0, shape.length);
			} else {
				throw new IllegalArgumentException("Cannot update null source shape with non-null");
			}
		} else if (oshape != null) {
			throw new IllegalArgumentException("Cannot update non-null source shape with null");
		}
	}

	/**
	 * Check slice configure
	 * @param l length of dimension
	 * @param b beginning
	 * @param e end
	 * @param d delta
	 * @return true if okay
	 */
	static boolean isSliceWithinShape(int l, int b, int e, int d) {
		if (l == 0) {
			return b == 0 && e == 0;
		}
		if (d > 0) {
			return b >= 0 && b <= e && b < l && e <= l;
		}
		return b >= 0 && b >= e && b < l && e >= -1;
	}

	/**
	 * Returns {@code true} if all of originals shapes are covered by positive
	 * steps slices, else {@code false}.
	 * 
	 * @return {@code true} if all of originals shapes is covered by this slice
	 *         with positive steps, {@code false} in the other case.
	 */
	public boolean isAll() {
		if (expanded) {
			return false;
		}

		if (oshape == null) {
			return true;
		}

		boolean allData = Arrays.equals(oshape, lshape);
		if (allData) {
			for (int i = 0; i < lshape.length; i++) {
				if (lstep[i] < 0) {
					allData = false;
					break;
				}
			}
		}
		return allData;
	}

	/**
	 * Flips the slice direction in given dimension, this means that slice
	 * begins at previous end point, steps in the opposite direction, and
	 * finishes at the previous start point.
	 * 
	 * @param i
	 *            dimension to flip
	 */
	public SliceND flip(int i) {
		if (lshape == null) {
			return this;
		}

		i = ShapeUtils.checkAxis(lshape.length, i);

		int beg = lstart[i];
		int end = lstop[i];
		int step = lstep[i];
		int del = lstep[i] > 0 ? 1 : -1;

		int num = (end - beg - del) / step + 1; // number of steps
		lstart[i] = beg + (num - 1) * step;
		lstop[i] = Math.max(beg - step, -1);
		lstep[i] = -step;

		return this;
	}

	/**
	 * Flips slices directions in all dimensions, this means that all slices are
	 * beginning at previous end point, steps are in the opposite direction, and
	 * finishes are at the previous start point.
	 */
	public SliceND flip() {
		if (lshape == null) {
			return this;
		}
		int orank = lshape.length;
		for (int i = 0; i < orank; i++) {
			flip(i);
		}

		return this;
	}

	/**
	 * Converts to a slice array all the Slices of the SliceND.
	 * 
	 * @return a Slice array
	 */
	public Slice[] convertToSlice() {
		if (lshape == null) {
			return null;
		}

		int orank = lshape.length;

		Slice[] slice = new Slice[orank];

		for (int j = 0; j < orank; j++) {
			slice[j] = new Slice(lstart[j], lstop[j], lstep[j]);
		}

		return slice;
	}

	/**
	 * Creates a deep copy of the SliceND.
	 * 
	 * @return New SliceND with the current SliceND properties
	 */
	@Override
	public SliceND clone() {
		SliceND c = new SliceND(oshape);
		if (lshape != null) {
			for (int i = 0; i < lshape.length; i++) {
				c.lstart[i] = lstart[i];
				c.lstop[i] = lstop[i];
				c.lstep[i] = lstep[i];
				c.lshape[i] = lshape[i];
			}
		}
		c.expanded = expanded;
		return c;
	}

	/**
	 * Returns a string construction of the sliceND with the python form.
	 * 
	 * @return Constructed String of all Slices
	 */
	@Override
	public String toString() {
		final int rank = lshape.length;
		if (rank == 0) {
			return "";
		}
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < rank; i++) {
			Slice.appendSliceToString(s, oshape[i], lstart[i], lstop[i], lstep[i]);
			s.append(',');
		}

		return s.substring(0, s.length() - 1);
	}

	/**
	 * Creats SliceND from dataset.
	 * 
	 * @param data
	 *            ILazyDataset to treat
	 * @param start
	 *            Array of starts indexes
	 * @param stop
	 *            Array of stops indexes
	 * @return Constructed SliceND
	 */
	public static SliceND createSlice(ILazyDataset data, int[] start, int[] stop) {
		return createSlice(data, start, stop, null);
	}

	/**
	 * Creating SliceND from dataset.
	 * 
	 * @param data
	 *            ILazyDataset to treat
	 * @param start
	 *            Array of starts indexes
	 * @param stop
	 *            Array of stops indexes
	 * @param step
	 *            Array of steps
	 * @return Constructed SliceND
	 */
	public static SliceND createSlice(ILazyDataset data, int[] start, int[] stop, int[] step) {
		if (data instanceof IDynamicDataset) {
			return new SliceND(data.getShape(), ((IDynamicDataset) data).getMaxShape(), start, stop, step);
		}
		return new SliceND(data.getShape(), start, stop, step);
	}

	/**
	 * Create SliceND without sanity checks on start, stop and step
	 * @param shape
	 * @param maxShape
	 * @param start
	 * @param stop
	 * @param step
	 * @return slice
	 */
	static SliceND createSlice(final int[] shape, final int[] maxShape, final int[] start, final int[] stop, final int[] step) {
		if (shape == null) {
			throw new IllegalArgumentException("Shape must not be null");
		}
		int rank = shape.length;

		if (maxShape != null && maxShape.length != rank) {
			throw new IllegalArgumentException("Max shape must have same rank as shape");
		}
		if (start.length != rank || stop.length != rank || step.length != rank) {
			throw new IllegalArgumentException("No of indexes does not match data dimensions: you passed it start="
					+ start.length + ", stop=" + stop.length + ", step=" + step.length + ", and it needs " + rank);
		}

		SliceND s = new SliceND(shape);
		if (maxShape != null) {
			s.initMaxShape(maxShape);
		}
		s.lstart = start;
		s.lstop  = stop;
		s.lstep  = step;

		for (int i = 0; i < rank; i++) {
			int e = stop[i];
			s.lshape[i] = calcLength(start[i], e, step[i]);
			if (e > shape[i]) {
				s.oshape[i] = e;
				s.expanded = true;
			}
		}
		return s;
	}

}
