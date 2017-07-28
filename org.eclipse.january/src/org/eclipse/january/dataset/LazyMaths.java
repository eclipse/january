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
	private static final String DUPLICATE_AXIS_ERROR = "Axis arguments must be unique";
	private static final String TOO_MANY_AXES_ERROR = "Number of axes cannot be greater than the rank";
	private static boolean allowDatasetMaths = true; // ensure this is set to false before running tests!

	private LazyMaths() {

	}

	/**
	 * Setup the logging facilities
	 */
	protected static final Logger logger = LoggerFactory.getLogger(LazyMaths.class);

	// TODO Uncomment this next line when minimum JDK is set to 1.8
	// @FunctionalInterface
	private static interface IMathOperation {
		void execute(IDataset a, IDataset b, Dataset c);
	}

	private enum MathOperation implements IMathOperation {
		// TODO use lambdas here when moving to Java 8
		MAX(new IMathOperation() {
			@Override
			public void execute(IDataset a, IDataset b, Dataset c) {
				Maths.maximum(a, b, c);
			}
		}, "maximum"),
		MIN(new IMathOperation() {
			@Override
			public void execute(IDataset a, IDataset b, Dataset c) {
				Maths.minimum(a, b, c);
			}
		}, "minimum");

		private final IMathOperation operation;
		private final String operationName;

		private MathOperation(IMathOperation operation, String operationName) {
			this.operation = operation;
			this.operationName = operationName;
		}
		
		@Override
		public void execute(IDataset a, IDataset b, Dataset c) {
			operation.execute(a, b, c);
		}

		/**
		 * @return the operationName
		 */
		public String getOperationName() {
			return operationName;
		}

	}

	private static int[] requireSortedAxes(final ILazyDataset data, int[] axes) {
		int rank = data.getRank();
		if (axes == null || axes.length == 0) { // take to mean use all axes
			axes = new int[rank];
			for (int i = 0; i < rank; i++) {
				axes[i] = i;
			}
		} else {
			Arrays.sort(axes);
			if (rank < axes.length) {
				logger.error(TOO_MANY_AXES_ERROR);
				throw new IllegalArgumentException(TOO_MANY_AXES_ERROR);
			}
				
			for (int axisIndex = 0 ; axisIndex < axes.length ; axisIndex++) {
				if (axes.length > 1 && axisIndex > 0 && axes[axisIndex] == axes[axisIndex-1]) {
					logger.error(DUPLICATE_AXIS_ERROR);
					throw new IllegalArgumentException(DUPLICATE_AXIS_ERROR);
				}
				if (axes[axisIndex] < 0)
					axes[axisIndex] += rank;
				if (axes[axisIndex] < 0 || axes[axisIndex] >= rank) {
					logger.error(INVALID_AXIS_ERROR);
					throw new IllegalArgumentException(INVALID_AXIS_ERROR);
				}
			}
		}
		return axes;
	}

	private static Dataset maxmin(final ILazyDataset data, MathOperation operation, int...axes) throws DatasetException {
		axes = requireSortedAxes(data, axes);
		
		// we will be working here with the "ignoreAxes" instead to improve performance dramatically
		int[] ignoreAxes = new int[data.getRank()-axes.length];
		
		int k = 0;
		for (int i = 0 ; i < data.getRank() ; i++) {
			boolean found = false;
			for (int j = 0 ; j < axes.length ; j++) {
				if (i == axes[j]) {
					found = true;
					break;
				}
			}
			if (!found)		
				ignoreAxes[k++] = i;
		}
		
		final int[] oldShape = data.getShape();

		SliceND sa = new SliceND(oldShape);
		SliceNDIterator it = new SliceNDIterator(sa, ignoreAxes);
		Dataset result = null;
		
		while (it.hasNext()) {
			SliceND currentSlice = it.getCurrentSlice();
			IDataset slice = data.getSlice(currentSlice);
			if (result == null)
				result = DatasetUtils.convertToDataset(slice);
			else
				operation.execute(result, slice, result);
		}
		if (result != null) {
			result.setName(operation.getOperationName());
			result.squeeze();
		}
		return result;
	}

	/**
	 * @param data
	 * @param axes (can be negative). If null or empty then use all axes
	 * @return maximum along axes in lazy dataset
	 * @throws DatasetException
	 * @since 2.1
	 */
	public static Dataset max(final ILazyDataset data, int... axes) throws DatasetException {
		if (allowDatasetMaths && data instanceof Dataset) {
			Dataset tmp = (Dataset) data;
			axes = requireSortedAxes(data, axes);
			for (int i = axes.length - 1; i >= 0; i--) {
				tmp = tmp.max(axes[i]);
			}
			
			return tmp;
		}
		return maxmin(data, MathOperation.MAX, axes);
	}

	/**
	 * @param data
	 * @param axes (can be negative). If null or empty then use all axes
	 * @return minimum along axes in lazy dataset
	 * @throws DatasetException
	 * @since 2.1
	 */
	public static Dataset min(final ILazyDataset data, int... axes) throws DatasetException {
		if (allowDatasetMaths && data instanceof Dataset) {
			Dataset tmp = (Dataset) data;
			axes = requireSortedAxes(data, axes);
			for (int i = axes.length - 1; i >= 0; i--) {
				tmp = tmp.min(axes[i]);
			}
			
			return tmp;
		}
		return maxmin(data, MathOperation.MIN, axes);
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

		return DatasetFactory.zeros(nshape);
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
