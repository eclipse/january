/*-
 *******************************************************************************
 * Copyright (c) 2011, 2016 Diamond Light Source Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Peter Chang - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.eclipse.january.dataset;

import java.util.Arrays;

import org.eclipse.january.DatasetException;
import org.eclipse.january.IMonitor;
import org.eclipse.january.metadata.StatisticsMetadata;
import org.eclipse.january.metadata.internal.StatisticsMetadataImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic container class for data that is compound in nature
 * 
 * Each subclass has an array of compound types, items of this array are composed of primitive types
 * 
 * Data items can be Complex, Vector, etc
 * 
 */
public abstract class AbstractCompoundDataset extends AbstractDataset implements CompoundDataset {
	// pin UID to base class
	private static final long serialVersionUID = Dataset.serialVersionUID;

	private static final Logger logger = LoggerFactory.getLogger(AbstractCompoundDataset.class);

	protected int isize; // number of elements per item

	@Override
	public int getElementsPerItem() {
		return isize;
	}

	@Override
	protected int get1DIndex(final int i) {
		int n = super.get1DIndex(i);
		return stride == null ? isize * n : n;
	}

	@Override
	protected int get1DIndex(final int i, final int j) {
		int n = super.get1DIndex(i, j);
		return stride == null ? isize * n : n;
	}

	@Override
	protected int get1DIndexFromShape(final int[] n) {
		return isize * super.get1DIndexFromShape(n);
	}

	@Override
	public Dataset getUniqueItems() {
		throw new UnsupportedOperationException("Cannot sort compound datasets");
	}

	@Override
	public IndexIterator getIterator(final boolean withPosition) {
		if (stride != null) {
			return base.getSize() == 1 ? 
					(withPosition ? new PositionIterator(offset, shape) :  new SingleItemIterator(offset, size)) : new StrideIterator(isize, shape, stride, offset);
		}
		return withPosition ? getSliceIterator(null, null, null) :
			new ContiguousIterator(size, isize);
	}

	/**
	 * Get an iterator that picks out the chosen element from all items
	 * @param element
	 * @return an iterator
	 */
	public IndexIterator getIterator(int element) {
		if (element < 0)
			element += isize;
		if (element < 0 || element > isize) {
			logger.error("Invalid choice of element: {}/{}", element, isize);
			throw new IllegalArgumentException("Invalid choice of element: " + element + "/" + isize);
		}

		final IndexIterator it;
		if (stride != null) {
			it = base.getSize() == 1 ? new SingleItemIterator(offset + element, size) : new StrideIterator(isize, shape, stride, offset, element);
		} else {
			it = new ContiguousIterator(size, isize, element);
		}

		return it;
	}

	@Override
	public IndexIterator getSliceIterator(SliceND slice) {
		checkSliceND(slice);
		return internalGetSliceIterator(slice);
	}

	/**
	 * @param slice
	 * @return an slice iterator that operates like an IndexIterator
	 */
	@Override
	protected IndexIterator internalGetSliceIterator(SliceND slice) {
		if (ShapeUtils.calcLongSize(slice.getShape()) == 0) {
			return new NullIterator(shape, slice.getShape());
		}
		if (stride != null) {
			return new StrideIterator(isize, shape, stride, offset, slice);
		}

		return new SliceIterator(shape, size, isize, slice);
	}

	/**
	 * Constructor required for serialisation.
	 */
	public AbstractCompoundDataset() {
	}

	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj)) {
			return false;
		}

		CompoundDataset other = (CompoundDataset) obj;
		return isize == other.getElementsPerItem();
	}

	@Override
	public int hashCode() {
		return getCompoundStats().getHash(shape);
	}

	@Override
	public CompoundDataset cast(boolean repeat, int dtype, int isize) {
		return (CompoundDataset) super.cast(repeat, dtype, isize);
	}

	@Override
	public CompoundDataset cast(int dtype) {
		return (CompoundDataset) super.cast(dtype);
	}

	@Override
	abstract public AbstractCompoundDataset clone();

	@Override
	public CompoundDataset flatten() {
		return (CompoundDataset) super.flatten();
	}

	@Override
	public CompoundDataset getBy1DIndex(IntegerDataset index) {
		return (CompoundDataset) super.getBy1DIndex(index);
	}

	@Override
	public CompoundDataset getByBoolean(Dataset selection) {
		return (CompoundDataset) super.getByBoolean(selection);
	}

	@Override
	public CompoundDataset getByIndexes(Object... indexes) {
		return (CompoundDataset) super.getByIndexes(indexes);
	}

	@Override
	public CompoundDataset getSlice(IMonitor mon, int[] start, int[] stop, int[] step) {
		return (CompoundDataset) super.getSlice(mon, start, stop, step);
	}

	@Override
	public CompoundDataset getSlice(IMonitor mon, Slice... slice) {
		return (CompoundDataset) super.getSlice(mon, slice);
	}

	@Override
	public CompoundDataset getSlice(IMonitor mon, SliceND slice) {
		return (CompoundDataset) super.getSlice(mon, slice);
	}

	@Override
	public CompoundDataset getSlice(int[] start, int[] stop, int[] step) {
		return (CompoundDataset) super.getSlice(start, stop, step);
	}

	@Override
	public CompoundDataset getSlice(Slice... slice) {
		return (CompoundDataset) super.getSlice(slice);
	}

	@Override
	public CompoundDataset getSlice(SliceND slice) {
		return (CompoundDataset) super.getSlice(slice);
	}

	@Override
	abstract public AbstractCompoundDataset getSlice(SliceIterator iterator);

	@Override
	public CompoundDataset getSliceView(int[] start, int[] stop, int[] step) {
		return (CompoundDataset) super.getSliceView(start, stop, step);
	}

	@Override
	public CompoundDataset getSliceView(Slice... slice) {
		return (CompoundDataset) super.getSliceView(slice);
	}

	@Override
	public CompoundDataset getSliceView(SliceND slice) {
		return (CompoundDataset) super.getSliceView(slice);
	}

	@Override
	public CompoundDataset getTransposedView(int... axes) {
		return (CompoundDataset) super.getTransposedView(axes);
	}

	@Override
	abstract public AbstractCompoundDataset getView(boolean deepCopyMetadata);

	@Override
	public CompoundDataset getBroadcastView(int... broadcastShape) {
		return (CompoundDataset) super.getBroadcastView(broadcastShape);
	}

	@Override
	public CompoundDataset ifloorDivide(Object o) {
		return (CompoundDataset) super.ifloorDivide(o);
	}

	@Override
	public CompoundDataset reshape(int... shape) {
		return (CompoundDataset) super.reshape(shape);
	}

	@Override
	public CompoundDataset setSlice(Object obj, int[] start, int[] stop, int[] step) {
		return (CompoundDataset) super.setSlice(obj, start, stop, step);
	}

	@Override
	public CompoundDataset setSlice(Object object, Slice... slice) {
		return (CompoundDataset) super.setSlice(object, slice);
	}

	@Override
	public CompoundDataset sort(Integer axis) {
		throw new UnsupportedOperationException("Cannot sort dataset");
	}

	@Override
	public CompoundDataset squeezeEnds() {
		return (CompoundDataset) super.squeezeEnds();
	}

	@Override
	public CompoundDataset squeeze() {
		return (CompoundDataset) super.squeeze();
	}

	@Override
	public CompoundDataset squeeze(boolean onlyFromEnd) {
		return (CompoundDataset) super.squeeze(onlyFromEnd);
	}

	@Override
	public CompoundDataset swapAxes(int axis1, int axis2) {
		return (CompoundDataset) super.swapAxes(axis1, axis2);
	}

	@Override
	public synchronized CompoundDataset synchronizedCopy() {
		return clone();
	}

	@Override
	public CompoundDataset transpose(int... axes) {
		return (CompoundDataset) super.transpose(axes);
	}

	/**
	 * @since 2.0
	 * @return first value
	 */
	abstract protected double getFirstValue();

	abstract protected double getFirstValue(final int i);

	abstract protected double getFirstValue(final int i, final int j);

	abstract protected double getFirstValue(final int...pos);

	@Override
	public boolean getBoolean() {
		return getFirstValue() != 0;
	}

	@Override
	public boolean getBoolean(final int i) {
		return getFirstValue(i) != 0;
	}

	@Override
	public boolean getBoolean(final int i, final int j) {
		return getFirstValue(i, j) != 0;
	}

	@Override
	public boolean getBoolean(final int... pos) {
		return getFirstValue(pos) != 0;
	}

	@Override
	public byte getByte() {
		return (byte) getFirstValue();
	}

	@Override
	public byte getByte(final int i) {
		return (byte) getFirstValue(i);
	}

	@Override
	public byte getByte(final int i, final int j) {
		return (byte) getFirstValue(i, j);
	}

	@Override
	public byte getByte(final int... pos) {
		return (byte) getFirstValue(pos);
	}

	@Override
	public short getShort() {
		return (short) getFirstValue();
	}

	@Override
	public short getShort(final int i) {
		return (short) getFirstValue(i);
	}

	@Override
	public short getShort(final int i, final int j) {
		return (short) getFirstValue(i, j);
	}

	@Override
	public short getShort(final int... pos) {
		return (short) getFirstValue(pos);
	}

	@Override
	public int getInt() {
		return (int) getFirstValue();
	}

	@Override
	public int getInt(final int i) {
		return (int) getFirstValue(i);
	}

	@Override
	public int getInt(final int i, final int j) {
		return (int) getFirstValue(i, j);
	}

	@Override
	public int getInt(final int... pos) {
		return (int) getFirstValue(pos);
	}

	@Override
	public long getLong() {
		return (long) getFirstValue();
	}

	@Override
	public long getLong(final int i) {
		return (long) getFirstValue(i);
	}

	@Override
	public long getLong(final int i, final int j) {
		return (long) getFirstValue(i, j);
	}

	@Override
	public long getLong(final int... pos) {
		return (long) getFirstValue(pos);
	}

	@Override
	public float getFloat() {
		return (float) getFirstValue();
	}

	@Override
	public float getFloat(final int i) {
		return (float) getFirstValue(i);
	}

	@Override
	public float getFloat(final int i, final int j) {
		return (float) getFirstValue(i, j);
	}

	@Override
	public float getFloat(final int... pos) {
		return (float) getFirstValue(pos);
	}

	@Override
	public double getDouble() {
		return getFirstValue();
	}

	@Override
	public double getDouble(final int i) {
		return getFirstValue(i);
	}

	@Override
	public double getDouble(final int i, final int j) {
		return getFirstValue(i, j);
	}

	@Override
	public double getDouble(final int... pos) {
		return getFirstValue(pos);
	}

	@Override
	public void getDoubleArray(final double[] darray) {
		getDoubleArrayAbs(getFirst1DIndex(), darray);
	}

	@Override
	public void getDoubleArray(final double[] darray, final int i) {
		getDoubleArrayAbs(get1DIndex(i), darray);
	}

	@Override
	public void getDoubleArray(final double[] darray, final int i, final int j) {
		getDoubleArrayAbs(get1DIndex(i, j), darray);
	}

	@Override
	public void getDoubleArray(final double[] darray, final int... pos) {
		getDoubleArrayAbs(get1DIndex(pos), darray);
	}

	/**
	 * @return statistics metadata
	 * @since 2.0
	 */
	@SuppressWarnings("unchecked")
	protected StatisticsMetadata<double[]> getCompoundStats() {
		StatisticsMetadata<double[]> md = getFirstMetadata(StatisticsMetadata.class);
		if (md == null || md.isDirty(this)) {
			md = new StatisticsMetadataImpl<double[]>();
			md.initialize(this);
			setMetadata(md);
		}
		return md;
	}

	@Override
	public IntegerDataset argMax(int axis, boolean... ignoreInvalids) {
		logger.error("Cannot compare compound numbers");
		throw new UnsupportedOperationException("Cannot compare compound numbers");
	}

	@Override
	public IntegerDataset argMin(int axis, boolean... ignoreInvalids) {
		logger.error("Cannot compare compound numbers");
		throw new UnsupportedOperationException("Cannot compare compound numbers");
	}

	@Override
	public Number max(boolean... ignoreInvalids) {
		logger.error("Cannot compare compound numbers");
		throw new UnsupportedOperationException("Cannot compare compound numbers");
	}

	@Override
	public CompoundDataset max(int axis, boolean... ignoreInvalids) {
		logger.error("Cannot compare compound numbers");
		throw new UnsupportedOperationException("Cannot compare compound numbers");
	}

	@Override
	public Number min(boolean... ignoreInvalids) {
		logger.error("Cannot compare compound numbers");
		throw new UnsupportedOperationException("Cannot compare compound numbers");
	}

	@Override
	public CompoundDataset min(int axis, boolean... ignoreInvalids) {
		logger.error("Cannot compare compound numbers");
		throw new UnsupportedOperationException("Cannot compare compound numbers");
	}


	@Override
	public int[] maxPos(boolean... ignoreNaNs) {
		logger.error("Cannot compare compound numbers");
		throw new UnsupportedOperationException("Cannot compare compound numbers");
	}

	@Override
	public int[] minPos(boolean... ignoreNaNs) {
		logger.error("Cannot compare compound numbers");
		throw new UnsupportedOperationException("Cannot compare compound numbers");
	}

	@Override
	public CompoundDataset peakToPeak(int axis, boolean... ignoreInvalids) {
		logger.error("Cannot compare compound numbers");
		throw new UnsupportedOperationException("Cannot compare compound numbers");
	}

	@Override
	public double[] maxItem() {
		return getCompoundStats().getMaximum();
	}

	@Override
	public double[] minItem() {
		return getCompoundStats().getMinimum();
	}

	@Override
	public Object mean(boolean... ignoreInvalids) {
		return getCompoundStats().getMean();
	}

	@Override
	public CompoundDataset mean(int axis, boolean... ignoreInvalids) {
		return (CompoundDataset) super.mean(axis, ignoreInvalids);
	}

	@Override
	public CompoundDataset product(int axis, boolean... ignoreInvalids) {
		return (CompoundDataset) super.product(axis, ignoreInvalids);
	}

	@Override
	public CompoundDataset rootMeanSquare(int axis, boolean... ignoreInvalids) {
		return (CompoundDataset) super.rootMeanSquare(axis, ignoreInvalids);
	}

	@Override
	public CompoundDataset stdDeviation(int axis) {
		return (CompoundDataset) super.stdDeviation(axis, false);
	}

	@Override
	public CompoundDataset stdDeviation(int axis, boolean isWholePopulation, boolean... ignoreInvalids) {
		return (CompoundDataset) super.stdDeviation(axis, isWholePopulation, ignoreInvalids);
	}

	@Override
	public Object sum(boolean... ignoreInvalids) {
		return getCompoundStats().getSum();
	}

	@Override
	public CompoundDataset sum(int axis, boolean... ignoreInvalids) {
		return (CompoundDataset) super.sum(axis, ignoreInvalids);
	}

	@Override
	public double variance(boolean isWholePopulation, boolean... ignoreInvalids) {
		return getCompoundStats().getVariance(isWholePopulation, ignoreInvalids);
	}

	@Override
	public CompoundDataset variance(int axis) {
		return (CompoundDataset) super.variance(axis, false);
	}

	@Override
	public CompoundDataset variance(int axis, boolean isWholePopulation, boolean... ignoreInvalids) {
		return (CompoundDataset) super.variance(axis, isWholePopulation, ignoreInvalids);
	}

	@Override
	public double rootMeanSquare(boolean... ignoreInvalids) {
		StatisticsMetadata<double[]> stats = getCompoundStats();

		double[] mean = stats.getMean(ignoreInvalids);
		double result = 0;
		for (int i = 0; i < isize; i++) {
			double m = mean[i];
			result += m * m;
		}
		return Math.sqrt(result + stats.getVariance(true));
	}

	/**
	 * @return error
	 */
	private CompoundDataset getInternalError() {
		ILazyDataset led = super.getErrors();
		if (led == null)
			return null;

		Dataset ed = null;
		try {
			ed = DatasetUtils.sliceAndConvertLazyDataset(led);
		} catch (DatasetException e) {
			logger.error("Could not get data from lazy dataset", e);
		}

		CompoundDataset ced; // ensure it has the same number of elements
		if (!(ed instanceof CompoundDataset) || ed.getElementsPerItem() != isize) {
			ced = new CompoundDoubleDataset(isize, true, ed);
		} else {
			ced = (CompoundDataset) ed;
		}
		
		if (led != ced) {
			setErrors(ced); // set back
		}
		return ced;
	}

	@Override
	public CompoundDataset getErrors() {
		CompoundDataset ed = getInternalError();
		if (ed == null)
			return null;

		return ed.getBroadcastView(shape);
	}

	@Override
	public double getError(final int i) {
		return calcError(getInternalErrorArray(true, i));
	}

	@Override
	public double getError(final int i, final int j) {
		return calcError(getInternalErrorArray(true, i, j));
	}

	@Override
	public double getError(final int... pos) {
		return calcError(getInternalErrorArray(true, pos));
	}

	private double calcError(double[] es) {
		if (es == null)
			return 0;

		// assume elements are independent
		double e = 0;
		for (int k = 0; k < isize; k++) {
			e += es[k];
		}

		return Math.sqrt(e);
	}

	@Override
	public double[] getErrorArray(final int i) {
		return getInternalErrorArray(false, i);
	}

	@Override
	public double[] getErrorArray(final int i, final int j) {
		return getInternalErrorArray(false, i, j);
	}

	@Override
	public double[] getErrorArray(final int... pos) {
		return getInternalErrorArray(false, pos);
	}

	private Dataset getInternalError(final boolean squared) {
		Dataset sed = squared ? getInternalSquaredError() : getInternalError();
		if (sed == null)
			return null;

		return sed.getBroadcastView(shape);
	}

	private double[] getInternalErrorArray(final boolean squared, final int i) {
		Dataset sed = getInternalError(squared);
		if (sed == null)
			return null;

		double[] es;
		if (sed instanceof CompoundDoubleDataset) {
			es = ((CompoundDoubleDataset) sed).getDoubleArray(i);
			if (sed.getElementsPerItem() != isize) { // ensure error is broadcasted
				Arrays.fill(es, es[0]);
			}
		} else {
			es = new double[isize];
			Arrays.fill(es, ((DoubleDataset) sed).getDouble(i));
		}
		return es;
	}

	private double[] getInternalErrorArray(final boolean squared, final int i, final int j) {
		Dataset sed = getInternalError(squared);
		if (sed == null)
			return null;

		double[] es;
		if (sed instanceof CompoundDoubleDataset) {
			es = ((CompoundDoubleDataset) sed).getDoubleArray(i, j);
			if (sed.getElementsPerItem() != isize) { // ensure error is broadcasted
				Arrays.fill(es, es[0]);
			}
		} else {
			es = new double[isize];
			Arrays.fill(es, ((DoubleDataset) sed).getDouble(i, j));
		}
		return es;
	}

	private double[] getInternalErrorArray(final boolean squared, final int... pos) {
		Dataset sed = getInternalError(squared);
		if (sed == null)
			return null;

		double[] es = new double[isize];
		if (sed instanceof CompoundDoubleDataset) {
			es = ((CompoundDoubleDataset) sed).getDoubleArray(pos);
			if (sed.getElementsPerItem() != isize) { // ensure error is broadcasted
				Arrays.fill(es, es[0]);
			}
		} else {
			es = new double[isize];
			Arrays.fill(es, ((DoubleDataset) sed).getDouble(pos));
		}
		return es;
	}
}

