/*-
 *******************************************************************************
 * Copyright (c) 2011, 2017 Diamond Light Source Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Peter Chang - initial API and implementation and/or initial documentation
 *    Tom Schoonjans - min and max methods
 *******************************************************************************/

package org.eclipse.january.dataset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.january.DatasetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mathematics class for lazy datasets
 */
public final class LazyMaths {

	private static final String INVALID_AXIS_ERROR = "Axis argument is outside allowed range";
	private static boolean allowDatasetMaths = true; // ensure this is set to false before running tests!

	private LazyMaths() {

	}

	/**
	 * Setup the logging facilities
	 */
	protected static final Logger logger = LoggerFactory.getLogger(LazyMaths.class);

	// Uncomment this next line when minimum JDK is set to 1.8
	// @FunctionalInterface
	private static interface IMathOperation {
		double perform(IDataset oneDSlice);
	}

	private enum MathOperation implements IMathOperation {
		// use lambdas here when moving to Java 8
		MAX(new IMathOperation() {
			@Override
			public double perform(IDataset oneDSlice) {
				return oneDSlice.max().doubleValue();
			}
		}),
		MIN(new IMathOperation() {
			@Override
			public double perform(IDataset oneDSlice) {
				return oneDSlice.min().doubleValue();
			}
		}),
		MEDIAN(new IMathOperation() {
			@Override
			public double perform(IDataset oneDSlice) {
				return (Double) Stats.median(DatasetUtils.convertToDataset(oneDSlice));
			}
		});

		private final IMathOperation operation;

		private MathOperation(IMathOperation operation) {
			this.operation = operation;
		}
		@Override
		public double perform(IDataset oneDSlice) {
			return operation.perform(oneDSlice);
		}

	}

	private static Dataset maxmin(final ILazyDataset data, int axis, MathOperation operation) throws DatasetException {
		int rank = data.getRank();
		if (axis < 0)
			axis += rank;
		if (axis < 0 || axis >= rank) {
			logger.error(INVALID_AXIS_ERROR);
			throw new IllegalArgumentException(INVALID_AXIS_ERROR);
		}

		final int[] oldShape = data.getShape();
		final int[] newShape = new int[rank-1];
		int counter = 0;
		for (int i = 0 ; i < rank ; i++) {
			if (i == axis)
				continue;
			newShape[counter++] = oldShape[i];
		}
		DoubleDataset result = DatasetFactory.zeros(DoubleDataset.class, newShape);

		SliceND sa = new SliceND(oldShape);
		SliceNDIterator it = new SliceNDIterator(sa, axis);
		
		while (it.hasNext()) {
			SliceND currentSlice = it.getCurrentSlice();
			IDataset slice = data.getSlice(currentSlice);
			result.setItem(operation.perform(slice), it.getUsedPos());
		}
		
		return result;
	}

	/**
	 * @param data
	 * @param axis (can be negative)
	 * @return maximum along axis in lazy dataset
	 * @throws DatasetException
	 */
	public static Dataset max(final ILazyDataset data, int axis) throws DatasetException {
		if (allowDatasetMaths && data instanceof Dataset)
			return ((Dataset) data).max(axis);
		return maxmin(data, axis, MathOperation.MAX);
	}

	/**
	 * @param data
	 * @param axis (can be negative)
	 * @return minimum along axis in lazy dataset
	 * @throws DatasetException
	 */
	public static Dataset min(final ILazyDataset data, int axis) throws DatasetException {
		if (allowDatasetMaths && data instanceof Dataset)
			return ((Dataset) data).min(axis);
		return maxmin(data, axis, MathOperation.MIN);
	}

	/**
	 * @param data
	 * @param axis (can be negative)
	 * @return median along axis in lazy dataset
	 * @throws DatasetException
	 */
	public static Dataset median(final ILazyDataset data, int axis) throws DatasetException {
		if (allowDatasetMaths && data instanceof Dataset)
			return Stats.median((Dataset) data, axis);
		return maxmin(data, axis, MathOperation.MEDIAN);
	}

	/**
	 * @param data
	 * @param axis (can be negative)
	 * @return sum along axis in lazy dataset
	 * @throws DatasetException 
	 */
	public static Dataset sum(final ILazyDataset data, int axis) throws DatasetException {
		if (allowDatasetMaths && data instanceof Dataset)
			return ((Dataset) data).sum(axis);
		int[][] sliceInfo = new int[3][];
		int[] shape = data.getShape();
		final Dataset result = prepareDataset(axis, shape, sliceInfo);

		final int[] start = sliceInfo[0];
		final int[] stop = sliceInfo[1];
		final int[] step = sliceInfo[2];
		final int length = shape[axis];

		for (int i = 0; i < length; i++) {
			start[axis] = i;
			stop[axis] = i + 1;
			result.iadd(data.getSlice(start, stop, step));
		}

		result.setShape(ShapeUtils.squeezeShape(shape, axis));
		return result;
	}

	/**
	 * @param data
	 * @param ignoreAxes axes to ignore
	 * @return sum when given axes are ignored in lazy dataset
	 * @throws DatasetException 
	 * @since 2.0
	 */
	public static Dataset sum(final ILazyDataset data, int... ignoreAxes) throws DatasetException {
		return sum(data, true, ignoreAxes);
	}
	
	/**
	 * @param data
	 * @param ignore if true, ignore the provided axes, otherwise use only the provided axes 
	 * @param axes axes to ignore or accept, depending on the preceding flag
	 * @return sum
	 * @throws DatasetException 
	 * @since 2.0
	 */
	public static Dataset sum(final ILazyDataset data, boolean ignore, int... axes) throws DatasetException {
		Arrays.sort(axes); // ensure they are properly sorted
	
		ILazyDataset rv = data;
		
		if (ignore) {
			List<Integer> goodAxes = new ArrayList<>();
			for (int i = 0 ; i < data.getRank() ; i++) {
				boolean found = false;
				for (int j = 0 ; j < axes.length ; j++) {
					if (i == axes[j]) {
						found = true;
						break;
					}
				}
				if (!found)		
					goodAxes.add(i);
			}

			for (int i = 0 ; i < goodAxes.size() ; i++) {
				rv = sum(rv, goodAxes.get(i) - i);
			}
		} else {
			for (int i = 0 ; i < axes.length ; i++) {
				rv = sum(rv, axes[i] - i);
			}
		}
		return DatasetUtils.sliceAndConvertLazyDataset(rv);
	}
	
	/**
	 * @param data
	 * @param axis (can be negative)
	 * @return product along axis in lazy dataset
	 * @throws DatasetException 
	 */
	public static Dataset product(final ILazyDataset data, int axis) throws DatasetException {
		int[][] sliceInfo = new int[3][];
		int[] shape = data.getShape();
		final Dataset result = prepareDataset(axis, shape, sliceInfo);
		result.fill(1);

		final int[] start = sliceInfo[0];
		final int[] stop = sliceInfo[1];
		final int[] step = sliceInfo[2];
		final int length = shape[axis];

		for (int i = 0; i < length; i++) {
			start[axis] = i;
			stop[axis] = i + 1;
			result.imultiply(data.getSlice(start, stop, step));
		}

		result.setShape(ShapeUtils.squeezeShape(shape, axis));
		return result;
	}

	/**
	 * @param start
	 * @param stop inclusive
	 * @param data
	 * @param ignoreAxes
	 * @return mean when given axes are ignored in lazy dataset
	 * @throws DatasetException 
	 */
	public static Dataset mean(int start, int stop, ILazyDataset data, int... ignoreAxes) throws DatasetException {
		int[] shape = data.getShape();
		PositionIterator iter = new PositionIterator(shape, ignoreAxes);
		int[] pos = iter.getPos();
		boolean[] omit = iter.getOmit();

		int rank = shape.length;
		int[] st = new int[rank];
		Arrays.fill(st, 1);
		int[] end = new int[rank];

		RunningAverage av = null;
		int c = 0;
		while (iter.hasNext() && c < stop) {
			if (c++ < start) continue;
			for (int i = 0; i < rank; i++) {
				end[i] = omit[i] ? shape[i] : pos[i] + 1;
			}
			IDataset ds = data.getSlice(pos, end, st);
			if (av == null) {
				av = new RunningAverage(ds);
			} else {
				av.update(ds);
			}
		}

		return  av != null ? av.getCurrentAverage().squeeze() : null;
	}
	
	public static Dataset mean(ILazyDataset data, int... ignoreAxes) throws DatasetException {
		return mean(0, Integer.MAX_VALUE -1 , data, ignoreAxes);
	}

	private static Dataset prepareDataset(int axis, int[] shape, int[][] sliceInfo) {
		int rank = shape.length;
		if (axis < 0)
			axis += rank;
		if (axis < 0 || axis >= rank) {
			logger.error(INVALID_AXIS_ERROR);
			throw new IllegalArgumentException(INVALID_AXIS_ERROR);
		}

		sliceInfo[0] = new int[rank];
		sliceInfo[1] = shape.clone();
		sliceInfo[2] = new int[rank];
		Arrays.fill(sliceInfo[2], 1);

		final int[] nshape = shape.clone();
		nshape[axis] = 1;

		return DatasetFactory.zeros(DoubleDataset.class, nshape);
	}

	/**
	 * @return the allowDatasetMaths
	 */
	public static boolean isAllowDatasetMaths() {
		return allowDatasetMaths;
	}

	/**
	 * @param allowDatasetMaths the allowDatasetMaths to set
	 */
	public static void setAllowDatasetMaths(boolean allowDatasetMaths) {
		LazyMaths.allowDatasetMaths = allowDatasetMaths;
	}
}
