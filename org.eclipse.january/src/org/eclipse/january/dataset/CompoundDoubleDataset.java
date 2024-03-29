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

// GEN_COMMENT

package org.eclipse.january.dataset;

import java.util.Arrays;

import org.apache.commons.math3.complex.Complex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extend compound dataset for double values // PRIM_TYPE
 */
public class CompoundDoubleDataset extends AbstractCompoundDataset {
	// pin UID to base class
	private static final long serialVersionUID = Dataset.serialVersionUID;

	private static final Logger logger = LoggerFactory.getLogger(CompoundDoubleDataset.class);

	protected double[] data; // subclass alias // PRIM_TYPE

	@Override
	protected void setData() {
		data = (double[]) odata; // PRIM_TYPE
	}

	protected double[] createArray(final int size) { // PRIM_TYPE
		double[] array = null; // PRIM_TYPE

		try {
			array = new double[isize * size]; // PRIM_TYPE
		} catch (OutOfMemoryError e) {
			logger.error("The size of the dataset ({}) that is being created is too large "
					+ "and there is not enough memory to hold it.", size);
			throw new OutOfMemoryError("The dimensions given are too large, and there is "
					+ "not enough memory available in the Java Virtual Machine");
		}
		return array;
	}

	/**
	 * Create a null dataset
	 */
	CompoundDoubleDataset() {
	}

	/**
	 * Create a null dataset
	 * @param itemSize
	 */
	CompoundDoubleDataset(final int itemSize) {
		isize = itemSize;
	}

	/**
	 * Create a zero-filled dataset of given item size and shape
	 * @param itemSize
	 * @param shape
	 */
	CompoundDoubleDataset(final int itemSize, final int[] shape) {
		isize = itemSize;
		if (shape != null) {
			size = ShapeUtils.calcSize(shape);
			this.shape = shape.clone();
	
			try {
				odata = data = createArray(size);
			} catch (Throwable t) {
				logger.error("Could not create a dataset of shape {}", Arrays.toString(shape), t);
				throw new IllegalArgumentException(t);
			}
		}
	}

	/**
	 * Copy a dataset
	 * @param dataset
	 */
	CompoundDoubleDataset(final CompoundDoubleDataset dataset) {
		isize = dataset.isize;

		copyToView(dataset, this, true, true);
		try {
			if (dataset.stride == null) {
				if (dataset.data != null) {
					odata = data = dataset.data.clone();
				}
			} else {
				offset = 0;
				stride = null;
				base = null;
				odata = data = createArray(size);
				IndexIterator iter = dataset.getIterator();
				for (int j = 0; iter.hasNext();) {
					for (int i = 0; i < isize; i++) {
						data[j++] = dataset.data[iter.index + i];
					}
				}
			}
		} catch (Throwable t) {
			logger.error("Could not create a dataset of shape {}", Arrays.toString(shape), t);
			throw new IllegalArgumentException(t);
		}
	}

	/**
	 * Create a dataset using given dataset
	 * @param dataset
	 */
	CompoundDoubleDataset(final CompoundDataset dataset) {
		copyToView(dataset, this, true, false);
		offset = 0;
		stride = null;
		base = null;
		isize = dataset.getElementsPerItem();
		try {
			odata = data = createArray(size);
		} catch (Throwable t) {
			logger.error("Could not create a dataset of shape {}", Arrays.toString(shape), t);
			throw new IllegalArgumentException(t);
		}

		IndexIterator iter = dataset.getIterator();
		for (int j = 0; iter.hasNext();) {
			for (int i = 0; i < isize; i++) {
				data[j++] = dataset.getElementDoubleAbs(iter.index + i); // GET_ELEMENT_WITH_CAST
			}
		}
	}

	/**
	 * Create a dataset using given data (elements are grouped together)
	 * @param itemSize
	 * @param data
	 * @param shape
	 *            (can be null to create 1D dataset)
	 */
	CompoundDoubleDataset(final int itemSize, final double[] data, int... shape) { // PRIM_TYPE
		isize = itemSize;
		if (data != null) {
			if (shape == null || (shape.length == 0 && data.length > isize)) {
				shape = new int[] { data.length / isize };
			}
			size = ShapeUtils.calcSize(shape);
			if (size * isize != data.length) {
				throw new IllegalArgumentException(String.format("Shape %s is not compatible with size of data array, %d",
						Arrays.toString(shape), data.length / isize));
			}
			this.shape = size == 0 ? null : shape.clone();
	
			odata = this.data = data;
		}
	}

	/**
	 * Create a dataset using given datasets
	 * @param datasets
	 */
	CompoundDoubleDataset(final Dataset... datasets) {
		if (datasets.length < 1) {
			throw new IllegalArgumentException("Array of datasets must have length greater than zero");
		}

		for (int i = 1; i < datasets.length; i++) {
			datasets[0].checkCompatibility(datasets[i]);
		}

		isize = datasets.length;
		size = ShapeUtils.calcSize(datasets[0].getShapeRef());
		shape = datasets[0].getShape();

		try {
			odata = data = createArray(size);
		} catch (Throwable t) {
			logger.error("Could not create a dataset of shape {}", Arrays.toString(shape), t);
			throw new IllegalArgumentException(t);
		}

		IndexIterator[] iters = new IndexIterator[isize];
		for (int i = 0; i < datasets.length; i++) {
			iters[i] = datasets[i].getIterator();
		}

		for (int j = 0; iters[0].hasNext();) {
			data[j++] = datasets[0].getElementDoubleAbs(iters[0].index); // GET_ELEMENT_WITH_CAST
			for (int i = 1; i < datasets.length; i++) {
				iters[i].hasNext();
				data[j++] = datasets[i].getElementDoubleAbs(iters[i].index); // GET_ELEMENT_WITH_CAST
			}
		}
	}

	/**
	 * Cast a dataset to this compound type. If repeat is set, the first element of each item in the given dataset is
	 * repeated across all elements of an item. Otherwise, each item comprises a truncated or zero-padded copy of
	 * elements from the given dataset.
	 * @param itemSize
	 * @param repeat
	 *            repeat first element
	 * @param dataset
	 */
	CompoundDoubleDataset(final int itemSize, final boolean repeat, final Dataset dataset) {
		isize = itemSize;
		size = dataset.getSize();
		shape = dataset.getShape();
		name = new String(dataset.getName());

		try {
			odata = data = createArray(size);
		} catch (Throwable t) {
			logger.error("Could not create a dataset of shape {}", Arrays.toString(shape), t);
			throw new IllegalArgumentException(t);
		}
		final int os = dataset.getElementsPerItem();

		IndexIterator iter = dataset.getIterator();
		if (repeat) {
			int i = 0;
			while (iter.hasNext()) {
				final double v = dataset.getElementDoubleAbs(iter.index); // PRIM_TYPE // GET_ELEMENT_WITH_CAST
				for (int k = 0; k < isize; k++) {
					data[i++] = v;
				}
			}
		} else {
			final int kmax = Math.min(isize, os);
			int i = 0;
			while (iter.hasNext()) {
				for (int k = 0; k < kmax; k++) {
					data[i + k] = dataset.getElementDoubleAbs(iter.index + k); // GET_ELEMENT_WITH_CAST
				}
				i += isize;
			}
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (!getClass().equals(obj.getClass())) {
			if (getRank() == 0) { // for zero-rank datasets
				return obj.equals(getObjectAbs(offset));
			}
			return false;
		}

		CompoundDoubleDataset other = (CompoundDoubleDataset) obj;
		if (isize != other.isize) {
			return false;
		}
		if (size != other.size) {
			return false;
		}
		if (!Arrays.equals(shape, other.shape)) {
			return false;
		}
		if (data == other.data && stride == null && other.stride == null) {
			return true;
		}

		IndexIterator iter = getIterator();
		IndexIterator oiter = other.getIterator();
		while (iter.hasNext() && oiter.hasNext()) {
			for (int j = 0; j < isize; j++) {
				if (data[iter.index+j] != other.data[oiter.index+j]) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public CompoundDoubleDataset clone() {
		return new CompoundDoubleDataset(this);
	}

	/**
	 * Create a dataset from an object which could be a Java list, array (of arrays...) or Number. Ragged
	 * sequences or arrays are padded with zeros. The item size is the last dimension of the corresponding
	 * elemental dataset
	 * 
	 * @param obj
	 * @return dataset with contents given by input
	 */
	static CompoundDoubleDataset createFromObject(final Object obj) {
		DoubleDataset result = DoubleDataset.createFromObject(obj); // CLASS_TYPE
		return createCompoundDatasetWithLastDimension(result, true);
	}

	/**
	 * Create a 1D dataset from an object which could be a Java list, array (of arrays...) or Number. Ragged
	 * sequences or arrays are padded with zeros.
	 * 
	 * @param itemSize item size
	 * @param obj object
	 * @return dataset with contents given by input
	 */
	public static CompoundDoubleDataset createFromObject(final int itemSize, final Object obj) {
		DoubleDataset result = DoubleDataset.createFromObject(obj); // CLASS_TYPE
		boolean zeroRank = result.shape == null ? false : result.shape.length == 0;
		if (zeroRank) {
			result.resize(itemSize); // special case of single item
			result.fill(obj);
		}
		CompoundDoubleDataset ds = new CompoundDoubleDataset(itemSize, result.getData(), null);
		if (zeroRank) {
			ds.setShape(new int[0]);
		}
		return ds;
	}

	/**
	 * @param stop
	 * @return a new 1D dataset, filled with values determined by parameters
	 */
	static CompoundDoubleDataset createRange(final int itemSize, final double stop) {
		return createRange(itemSize, 0., stop, 1.);
	}

	/**
	 * @param start
	 * @param stop
	 * @param step
	 * @return a new 1D dataset, filled with values determined by parameters
	 */
	static CompoundDoubleDataset createRange(final int itemSize, final double start, final double stop,
			final double step) {
		int size = calcSteps(start, stop, step);
		CompoundDoubleDataset result = new CompoundDoubleDataset(itemSize, new int[] { size });
		for (int i = 0; i < size; i++) {
			result.data[i * result.isize] = (start + i * step); // PRIM_TYPE // ADD_CAST
		}
		return result;
	}

	/**
	 * @param shape
	 * @return a dataset filled with ones
	 */
	static CompoundDoubleDataset ones(final int itemSize, final int... shape) {
		return new CompoundDoubleDataset(itemSize, shape).fill(1);
	}

	/**
	 * Create a compound dataset using last dimension of given dataset
	 * @param a dataset
	 * @param shareData if true, then share data when possible otherwise copy it
	 * @return compound dataset
	 */
	public static CompoundDoubleDataset createCompoundDatasetWithLastDimension(final Dataset a, final boolean shareData) {
		if (a.getElementsPerItem() != 1) {
			logger.error("Need a single-element dataset");
			throw new IllegalArgumentException("Need a single-element dataset");
		}
		if (!DoubleDataset.class.isAssignableFrom(a.getClass())) { // CLASS_TYPE
			logger.error("Dataset type must be double"); // PRIM_TYPE 
			throw new IllegalArgumentException("Dataset type must be double"); // PRIM_TYPE 
		}

		final int[] shape = a.getShapeRef();
		if (shape == null) {
			return new CompoundDoubleDataset(0);
		}

		final int rank = shape.length - 1;
		final int is = rank < 0 ? 1 : shape[rank];

		CompoundDoubleDataset result = new CompoundDoubleDataset(is);

		result.shape = rank > 0 ? Arrays.copyOf(shape, rank) : (rank < 0 ? new int[0] : new int[] {1});
		result.size = ShapeUtils.calcSize(result.shape);
		result.odata = shareData ? a.flatten().getBuffer() : a.clone().getBuffer();
		result.setName(a.getName());
		result.setData();
		return result;
	}

	@Override
	public DoubleDataset asNonCompoundDataset(final boolean shareData) { // CLASS_TYPE
		DoubleDataset result = new DoubleDataset(); // CLASS_TYPE
		final int is = getElementsPerItem();
		final int rank = is == 1 ? shape.length : shape.length + 1;
		final int[] nshape = Arrays.copyOf(shape, rank);
		if (is != 1)
			nshape[rank-1] = is;

		result.shape = nshape;
		result.size = ShapeUtils.calcSize(nshape);
		result.odata = shareData && isContiguous() ? data : clone().getBuffer();
		result.setName(name);
		result.setData();
		return result;
	}

	@Override
	public CompoundDoubleDataset fill(Object obj) {
		setDirty();
		if (obj instanceof Complex) {
			obj = new Complex(((Complex) obj).getReal(), 0);
		}
		double[] vr = DTypeUtils.toDoubleArray(obj, isize); // PRIM_TYPE // CLASS_TYPE
		IndexIterator iter = getIterator();

		while (iter.hasNext()) {
			for (int i = 0; i < isize; i++) {
				data[iter.index + i] = vr[i]; // PRIM_TYPE
			}
		}

		return this;
	}

	/**
	 * This is a typed version of {@link #getBuffer()}
	 * @return data buffer as linear array
	 */
	public double[] getData() { // PRIM_TYPE
		return data;
	}

	@Override
	protected int getBufferLength() {
		if (data == null)
			return 0;
		return data.length;
	}

	@Override
	public CompoundDoubleDataset getView(boolean deepCopyMetadata) {
		CompoundDoubleDataset view = new CompoundDoubleDataset(isize);
		copyToView(this, view, true, deepCopyMetadata);
		view.setData();
		return view;
	}

	/**
	 * Get values at absolute index in the internal array. This is an internal method with no checks so can be
	 * dangerous. Use with care or ideally with an iterator.
	 * 
	 * @param index
	 *            absolute index
	 * @return values
	 */
	public double[] getAbs(final int index) { // PRIM_TYPE
		double[] result = new double[isize]; // PRIM_TYPE
		for (int i = 0; i < isize; i++) {
			result[i] = data[index + i];
		}
		return result;
	}

	/**
	 * Get values at absolute index in the internal array. This is an internal method with no checks so can be
	 * dangerous. Use with care or ideally with an iterator.
	 *
	 * @param index
	 *            absolute index
	 * @param values output destination
	 */
	public void getAbs(final int index, final double[] values) { // PRIM_TYPE
		for (int i = 0; i < isize; i++) {
			values[i] = data[index + i];
		}
	}

	@Override
	public boolean getElementBooleanAbs(final int index) {
		return data[index] != 0;
	}

	@Override
	public double getElementDoubleAbs(final int index) {
		return data[index];
	}

	@Override
	public long getElementLongAbs(final int index) {
		return DTypeUtils.toLong(data[index]); // OMIT_TOLONG_INT
	}

	@Override
	protected void setItemDirect(final int dindex, final int sindex, final Object src) {
		setDirty();
		double[] dsrc = (double[]) src; // PRIM_TYPE
		for (int i = 0; i < isize; i++) {
			data[dindex + i] = dsrc[sindex + i];
		}
	}

	/**
	 * Set values at absolute index in the internal array. This is an internal method with no checks so can be
	 * dangerous. Use with care or ideally with an iterator.
	 *
	 * @param index
	 *            absolute index
	 * @param val
	 *            new values
	 */
	public void setAbs(final int index, final double[] val) { // PRIM_TYPE
		setDirty();
		for (int i = 0; i < isize; i++) {
			data[index + i] = val[i];
		}
	}

	/**
	 * Set element value at absolute index in the internal array. This is an internal method with no checks so can be
	 * dangerous. Use with care or ideally with an iterator.
	 *
	 * @param index
	 *            absolute index
	 * @param val
	 *            new value
	 */
	public void setAbs(final int index, final double val) { // PRIM_TYPE
		setDirty();
		data[index] = val;
	}

	@Override
	public Object getObject() {
		return getDoubleArray(); // PRIM_TYPE
	}

	@Override
	public Object getObject(final int i) {
		return getDoubleArray(i); // PRIM_TYPE
	}

	@Override
	public Object getObject(final int i, final int j) {
		return getDoubleArray(i, j); // PRIM_TYPE
	}

	@Override
	public Object getObject(final int... pos) {
		return getDoubleArray(pos); // PRIM_TYPE
	}

	@Override
	public byte[] getByteArray() {
		byte[] result = new byte[isize];
		int index = getFirst1DIndex();
		for (int k = 0; k < isize; k++) {
			result[k] = (byte) data[index + k]; // OMIT_UPCAST
		}
		return result;
	}

	@Override
	public byte[] getByteArray(final int i) {
		byte[] result = new byte[isize];
		int index = get1DIndex(i);
		for (int k = 0; k < isize; k++) {
			result[k] = (byte) data[index + k]; // OMIT_UPCAST
		}
		return result;
	}

	@Override
	public byte[] getByteArray(final int i, final int j) {
		byte[] result = new byte[isize];
		int index = get1DIndex(i, j);
		for (int k = 0; k < isize; k++) {
			result[k] = (byte) data[index + k]; // OMIT_UPCAST
		}
		return result;
	}

	@Override
	public byte[] getByteArray(final int... pos) {
		byte[] result = new byte[isize];
		int index = get1DIndex(pos);
		for (int k = 0; k < isize; k++) {
			result[k] = (byte) data[index + k]; // OMIT_UPCAST
		}
		return result;
	}

	@Override
	public short[] getShortArray() {
		short[] result = new short[isize];
		int index = getFirst1DIndex();
		for (int k = 0; k < isize; k++) {
			result[k] = (short) data[index + k]; // OMIT_UPCAST
		}
		return result;
	}

	@Override
	public short[] getShortArray(final int i) {
		short[] result = new short[isize];
		int index = get1DIndex(i);
		for (int k = 0; k < isize; k++) {
			result[k] = (short) data[index + k]; // OMIT_UPCAST
		}
		return result;
	}

	@Override
	public short[] getShortArray(final int i, final int j) {
		short[] result = new short[isize];
		int index = get1DIndex(i, j);
		for (int k = 0; k < isize; k++) {
			result[k] = (short) data[index + k]; // OMIT_UPCAST
		}
		return result;
	}

	@Override
	public short[] getShortArray(final int... pos) {
		short[] result = new short[isize];
		int index = get1DIndex(pos);
		for (int k = 0; k < isize; k++) {
			result[k] = (short) data[index + k]; // OMIT_UPCAST
		}
		return result;
	}

	@Override
	public int[] getIntArray() {
		int[] result = new int[isize];
		int index = getFirst1DIndex();
		for (int k = 0; k < isize; k++) {
			result[k] = (int) data[index + k]; // OMIT_UPCAST
		}
		return result;
	}

	@Override
	public int[] getIntArray(final int i) {
		int[] result = new int[isize];
		int index = get1DIndex(i);
		for (int k = 0; k < isize; k++) {
			result[k] = (int) data[index + k]; // OMIT_UPCAST
		}
		return result;
	}

	@Override
	public int[] getIntArray(final int i, final int j) {
		int[] result = new int[isize];
		int index = get1DIndex(i, j);
		for (int k = 0; k < isize; k++) {
			result[k] = (int) data[index + k]; // OMIT_UPCAST
		}
		return result;
	}

	@Override
	public int[] getIntArray(final int... pos) {
		int[] result = new int[isize];
		int index = get1DIndex(pos);
		for (int k = 0; k < isize; k++) {
			result[k] = (int) data[index + k]; // OMIT_UPCAST
		}
		return result;
	}

	@Override
	public long[] getLongArray() {
		long[] result = new long[isize];
		int index = getFirst1DIndex();
		for (int k = 0; k < isize; k++) {
			result[k] = (long) data[index + k]; // OMIT_UPCAST
		}
		return result;
	}

	@Override
	public long[] getLongArray(final int i) {
		long[] result = new long[isize];
		int index = get1DIndex(i);
		for (int k = 0; k < isize; k++) {
			result[k] = (long) data[index + k]; // OMIT_UPCAST
		}
		return result;
	}

	@Override
	public long[] getLongArray(final int i, final int j) {
		long[] result = new long[isize];
		int index = get1DIndex(i, j);
		for (int k = 0; k < isize; k++) {
			result[k] = (long) data[index + k]; // OMIT_UPCAST
		}
		return result;
	}

	@Override
	public long[] getLongArray(final int... pos) {
		long[] result = new long[isize];
		int index = get1DIndex(pos);
		for (int k = 0; k < isize; k++) {
			result[k] = (long) data[index + k]; // OMIT_UPCAST
		}
		return result;
	}

	@Override
	public float[] getFloatArray() {
		float[] result = new float[isize];
		int index = getFirst1DIndex();
		for (int k = 0; k < isize; k++) {
			result[k] = (float) data[index + k]; // OMIT_REAL_CAST
		}
		return result;
	}

	@Override
	public float[] getFloatArray(final int i) {
		float[] result = new float[isize];
		int index = get1DIndex(i);
		for (int k = 0; k < isize; k++) {
			result[k] = (float) data[index + k]; // OMIT_REAL_CAST
		}
		return result;
	}

	@Override
	public float[] getFloatArray(final int i, final int j) {
		float[] result = new float[isize];
		int index = get1DIndex(i, j);
		for (int k = 0; k < isize; k++) {
			result[k] = (float) data[index + k]; // OMIT_REAL_CAST
		}
		return result;
	}

	@Override
	public float[] getFloatArray(final int... pos) {
		float[] result = new float[isize];
		int index = get1DIndex(pos);
		for (int k = 0; k < isize; k++) {
			result[k] = (float) data[index + k]; // OMIT_REAL_CAST
		}
		return result;
	}

	@Override
	public double[] getDoubleArray() {
		double[] result = new double[isize];
		int index = getFirst1DIndex();
		for (int k = 0; k < isize; k++) {
			result[k] = data[index + k]; // OMIT_REAL_CAST
		}
		return result;
	}

	@Override
	public double[] getDoubleArray(final int i) {
		double[] result = new double[isize];
		int index = get1DIndex(i);
		for (int k = 0; k < isize; k++) {
			result[k] = data[index + k]; // OMIT_REAL_CAST
		}
		return result;
	}

	@Override
	public double[] getDoubleArray(final int i, final int j) {
		double[] result = new double[isize];
		int index = get1DIndex(i, j);
		for (int k = 0; k < isize; k++) {
			result[k] = data[index + k]; // OMIT_REAL_CAST
		}
		return result;
	}

	@Override
	public double[] getDoubleArray(final int... pos) {
		double[] result = new double[isize];
		int index = get1DIndex(pos);
		for (int k = 0; k < isize; k++) {
			result[k] = data[index + k]; // OMIT_REAL_CAST
		}
		return result;
	}

	@Override
	public void getDoubleArrayAbs(final int index, final double[] darray) {
		for (int i = 0; i < isize; i++) {
			darray[i] = data[index + i];
		}
	}

	@Override
	public String getString() {
		return getStringAbs(getFirst1DIndex());
	}

	@Override
	public String getString(final int i) {
		return getStringAbs(get1DIndex(i));
	}

	@Override
	public String getString(final int i, final int j) {
		return getStringAbs(get1DIndex(i, j));
	}

	@Override
	public String getString(final int... pos) {
		return getStringAbs(get1DIndex(pos));
	}

	@Override
	protected double getFirstValue() {
		return data[getFirst1DIndex()];
	}

	@Override
	protected double getFirstValue(int i) {
		return data[get1DIndex(i)];
	}

	@Override
	protected double getFirstValue(int i, int j) {
		return data[get1DIndex(i, j)];
	}

	@Override
	protected double getFirstValue(final int... pos) {
		return data[get1DIndex(pos)];
	}

	@Override
	public Object getObjectAbs(final int index) {
		double[] result = new double[isize]; // PRIM_TYPE
		for (int i = 0; i < isize; i++) {
			result[i] = data[index + i];
		}
		return result;
	}

	@Override
	public String getStringAbs(final int index) {
		StringBuilder s = new StringBuilder();
		s.append('(');
		s.append(stringFormat == null ? String.format("%.8g", data[index]) : // FORMAT_STRING
			stringFormat.format(data[index]));
		for (int i = 1; i < isize; i++) {
			s.append(' ');
			s.append(stringFormat == null ? String.format("%.8g", data[index + i]) : // FORMAT_STRING
				stringFormat.format(data[index + i]));
		}
		s.append(')');
		return s.toString();
	}

	@Override
	public void setObjectAbs(final int index, Object obj) {
		if (obj instanceof Complex) {
			obj = new Complex(((Complex) obj).getReal(), 0);
		}
		double[] oa = DTypeUtils.toDoubleArray(obj, isize); // PRIM_TYPE // CLASS_TYPE
		setAbs(index, oa);
	}

	@Override
	public void set(final Object obj) {
		setItem(DTypeUtils.toDoubleArray(obj, isize)); // CLASS_TYPE
	}

	@Override
	public void set(final Object obj, final int i) {
		setItem(DTypeUtils.toDoubleArray(obj, isize), i); // CLASS_TYPE
	}

	@Override
	public void set(final Object obj, final int i, final int j) {
		setItem(DTypeUtils.toDoubleArray(obj, isize), i, j); // CLASS_TYPE
	}

	@Override
	public void set(final Object obj, int... pos) {
		if (pos == null || (pos.length == 0 && shape.length > 0)) {
			pos = new int[shape.length];
		}

		setItem(DTypeUtils.toDoubleArray(obj, isize), pos); // CLASS_TYPE
	}

	/**
	 * Set values at first position. The dataset must not be null
	 * 
	 * @param d input source
	 * @since 2.0
	 */
	public void setItem(final double[] d) { // PRIM_TYPE
		if (d.length > isize) {
			throw new IllegalArgumentException("Array is larger than number of elements in an item");
		}
		setAbs(getFirst1DIndex(), d);
	}

	/**
	 * Set values at given position. The dataset must be 1D
	 * 
	 * @param d input source
	 * @param i position in first dimension
	 */
	public void setItem(final double[] d, final int i) { // PRIM_TYPE
		if (d.length > isize) {
			throw new IllegalArgumentException("Array is larger than number of elements in an item");
		}
		setAbs(get1DIndex(i), d);
	}

	/**
	 * Set values at given position. The dataset must be 1D
	 * 
	 * @param d input source
	 * @param i position in first dimension
	 * @param j position in second dimension
	 */
	public void setItem(final double[] d, final int i, final int j) { // PRIM_TYPE
		if (d.length > isize) {
			throw new IllegalArgumentException("Array is larger than number of elements in an item");
		}
		setAbs(get1DIndex(i, j), d);
	}

	/**
	 * Set values at given position
	 * 
	 * @param d input source
	 * @param pos position
	 */
	public void setItem(final double[] d, final int... pos) { // PRIM_TYPE
		if (d.length > isize) {
			throw new IllegalArgumentException("Array is larger than number of elements in an item");
		}
		setAbs(get1DIndex(pos), d);
	}

	private void setDoubleArrayAbs(final int index, final double[] d) {
		for (int i = 0; i < isize; i++)
			data[index + i] = d[i]; // ADD_CAST
	}

	@Override
	public void resize(int... newShape) {
		setDirty();
		IndexIterator iter = getIterator();
		int nsize = ShapeUtils.calcSize(newShape);
		double[] ndata; // PRIM_TYPE
		try {
			ndata = createArray(nsize);
		} catch (Throwable t) {
			logger.error("Could not create a dataset of shape {}", Arrays.toString(shape), t);
			throw new IllegalArgumentException(t);
		}

		int i = 0;
		while (iter.hasNext() && i < nsize) {
			for (int j = 0; j < isize; j++) {
				ndata[i++] = data[iter.index + j];
			}
		}

		odata = data = ndata;
		size = nsize;
		shape = newShape;
		stride = null;
		offset = 0;
		base = null;
	}

	@Override
	public CompoundDoubleDataset getSlice(final SliceIterator siter) {
		CompoundDoubleDataset result = new CompoundDoubleDataset(isize, siter.getShape());
		double[] rdata = result.data; // PRIM_TYPE
		IndexIterator riter = result.getIterator();

		while (siter.hasNext() && riter.hasNext()) {
			for (int i = 0; i < isize; i++)
				rdata[riter.index + i] = data[siter.index + i];
		}

		result.setName(name + BLOCK_OPEN + Slice.createString(siter.shape, siter.start, siter.stop, siter.step) + BLOCK_CLOSE);
		return result;
	}

	@Override
	public DoubleDataset getElementsView(int element) { // CLASS_TYPE
		if (element < 0)
			element += isize;
		if (element < 0 || element > isize) {
			throw new IllegalArgumentException(String.format("Invalid choice of element: %d/%d", element, isize));
		}

		DoubleDataset view = new DoubleDataset(shape); // CLASS_TYPE

		copyToView(this, view, true, true);
		view.setData();
		if (view.stride == null) {
			int[] offset = new int[1];
			view.stride = createStrides(this, offset);
			view.offset = offset[0] + element;
			view.base = base == null ? this : base;
		} else {
			view.offset += element;
		}

		return view;
	}

	@Override
	public DoubleDataset getElements(int element) { // CLASS_TYPE
		final DoubleDataset elements = new DoubleDataset(shape); // CLASS_TYPE

		copyElements(elements, element);
		return elements;
	}

	@Override
	public void copyElements(Dataset destination, int element) {
		if (element < 0)
			element += isize;
		if (element < 0 || element > isize) {
			throw new IllegalArgumentException(String.format("Invalid choice of element: %d/%d", element, isize));
		}
		if (getElementClass() != destination.getElementClass()) {
			throw new IllegalArgumentException("Element class of destination does not match this dataset");
		}

		final IndexIterator it = getIterator(element);
		final double[] elements = ((DoubleDataset) destination).data; // CLASS_TYPE // PRIM_TYPE
		destination.setDirty();

		int n = 0;
		while (it.hasNext()) {
			elements[n] = data[it.index];
			n++;
		}
	}

	@Override
	public void setElements(Dataset source, int element) {
		setDirty();
		if (element < 0)
			element += isize;
		if (element < 0 || element > isize) {
			throw new IllegalArgumentException(String.format("Invalid choice of element: %d/%d", element, isize));
		}
		if (getElementClass() != source.getElementClass()) {
			throw new IllegalArgumentException("Element class of destination does not match this dataset");
		}

		final IndexIterator it = getIterator(element);
		final double[] elements = ((DoubleDataset) source).data; // CLASS_TYPE // PRIM_TYPE

		int n = 0;
		while (it.hasNext()) {
			data[it.index] = elements[n];
			n++;
		}
	}

	@Override
	public void fillDataset(Dataset result, IndexIterator iter) {
		IndexIterator riter = result.getIterator();
		result.setDirty();

		double[] rdata = ((CompoundDoubleDataset) result).data; // PRIM_TYPE

		while (riter.hasNext() && iter.hasNext()) {
			for (int i = 0; i < isize; i++) {
				rdata[riter.index + i] = data[iter.index + i];
			}
		}
	}

	@Override
	public CompoundDoubleDataset setByBoolean(final Object o, Dataset selection) {
		setDirty();
		if (o instanceof Dataset) {
			Dataset ds = (Dataset) o;
			final int length = ((Number) selection.sum()).intValue();
			if (length != ds.getSize()) {
				throw new IllegalArgumentException(
						"Number of true items in selection does not match number of items in dataset");
			}

			IndexIterator iter = ds.getIterator();
			BooleanIterator biter = getBooleanIterator(selection);

			if (ds instanceof AbstractCompoundDataset) {
				if (isize != ds.getElementsPerItem()) {
					throw new IllegalArgumentException("Input dataset is not compatible with slice");
				}

				while (biter.hasNext() && iter.hasNext()) {
					for (int i = 0; i < isize; i++) {
						data[biter.index + i] = ds.getElementDoubleAbs(iter.index + i); // GET_ELEMENT_WITH_CAST
					}
				}
			} else {
				while (biter.hasNext() && iter.hasNext()) {
					data[biter.index] = ds.getElementDoubleAbs(iter.index); // GET_ELEMENT_WITH_CAST
					for (int i = 1; i < isize; i++) {
						data[biter.index + i] = 0;
					}
				}
			}
		} else {
			try {
				final double[] vr = DTypeUtils.toDoubleArray(o, isize); // PRIM_TYPE // CLASS_TYPE

				final BooleanIterator biter = getBooleanIterator(selection);

				while (biter.hasNext()) {
					for (int i = 0; i < isize; i++) {
						data[biter.index + i] = vr[i];
					}
				}
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Object for setting is not a dataset or number");
			}
		}
		return this;
	}

	@Override
	public CompoundDoubleDataset setBy1DIndex(final Object o, Dataset index) {
		setDirty();
		if (o instanceof Dataset) {
			Dataset ds = (Dataset) o;
			if (index.getSize() != ds.getSize()) {
				throw new IllegalArgumentException(
						"Number of items in selection does not match number of items in dataset");
			}

			IndexIterator oiter = ds.getIterator();
			final IntegerIterator iter = new IntegerIterator(index, size, isize);

			if (ds instanceof AbstractCompoundDataset) {
				if (isize != ds.getElementsPerItem()) {
					throw new IllegalArgumentException("Input dataset is not compatible with slice");
				}

				double[] temp = new double[isize];
				while (iter.hasNext() && oiter.hasNext()) {
					((AbstractCompoundDataset) ds).getDoubleArrayAbs(oiter.index, temp);
					setDoubleArrayAbs(iter.index, temp);
				}
				while (iter.hasNext() && oiter.hasNext()) {
					for (int i = 0; i < isize; i++) {
						data[iter.index + i] = ds.getElementDoubleAbs(oiter.index + i); // GET_ELEMENT_WITH_CAST
					}
				}
			} else {
				while (iter.hasNext() && oiter.hasNext()) {
					data[iter.index] = ds.getElementDoubleAbs(oiter.index); // GET_ELEMENT_WITH_CAST
					for (int i = 1; i < isize; i++) {
						data[iter.index + i] = 0;
					}
				}
			}
		} else {
			try {
				final double[] vr = DTypeUtils.toDoubleArray(o, isize); // PRIM_TYPE // CLASS_TYPE

				final IntegerIterator iter = new IntegerIterator(index, size, isize);

				while (iter.hasNext()) {
					setAbs(iter.index, vr);
				}
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Object for setting is not a dataset or number");
			}
		}
		return this;
	}

	@Override
	public CompoundDoubleDataset setByIndexes(final Object o, final Object... indexes) {
		setDirty();
		final IntegersIterator iter = new IntegersIterator(shape, indexes);
		final int[] pos = iter.getPos();

		if (o instanceof Dataset) {
			Dataset ds = (Dataset) o;
			if (ShapeUtils.calcSize(iter.getShape()) != ds.getSize()) {
				throw new IllegalArgumentException(
						"Number of items in selection does not match number of items in dataset");
			}

			IndexIterator oiter = ds.getIterator();

			if (ds instanceof AbstractCompoundDataset) {
				if (isize != ds.getElementsPerItem()) {
					throw new IllegalArgumentException("Input dataset is not compatible with slice");
				}

				double[] temp = new double[isize];
				while (iter.hasNext() && oiter.hasNext()) {
					((AbstractCompoundDataset) ds).getDoubleArray(temp, pos);
					setDoubleArrayAbs(get1DIndex(pos), temp);
				}
			} else {
				while (iter.hasNext() && oiter.hasNext()) {
					int n = get1DIndex(pos);
					data[n] = ds.getElementDoubleAbs(oiter.index); // GET_ELEMENT_WITH_CAST
					for (int i = 1; i < isize; i++) {
						data[n + i] = 0;
					}
				}
			}
		} else {
			try {
				final double[] vr = DTypeUtils.toDoubleArray(o, isize); // PRIM_TYPE // CLASS_TYPE

				while (iter.hasNext()) {
					setAbs(get1DIndex(pos), vr);
				}
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Object for setting is not a dataset or number");
			}
		}
		return this;
	}

	@Override
	CompoundDoubleDataset setSlicedView(Dataset view, Dataset d) {
		setDirty();
		final BroadcastSelfIterator it = BroadcastSelfIterator.createIterator(view, d);

		final int is = view.getElementsPerItem();

		if (is > 1) {
			if (d.getElementsPerItem() == 1) {
				while (it.hasNext()) {
					final double bv = it.bDouble; // PRIM_TYPE // BCAST_WITH_CAST d.getElementDoubleAbs(it.bIndex);
					data[it.aIndex] = bv;
					for (int j = 1; j < is; j++) {
						data[it.aIndex + j] = bv;
					}
				}
			} else {
				while (it.hasNext()) {
					data[it.aIndex] = it.bDouble; // BCAST_WITH_CAST d.getElementDoubleAbs(it.bIndex);
					for (int j = 1; j < is; j++) {
						data[it.aIndex + j] = d.getElementDoubleAbs(it.bIndex + j); // GET_ELEMENT_WITH_CAST
					}
				}
			}
		} else {
			while (it.hasNext()) {
				data[it.aIndex] = it.bDouble; // BCAST_WITH_CAST d.getElementDoubleAbs(it.bIndex);
			}
		}
		return this;
	}

	@Override
	public CompoundDoubleDataset setSlice(final Object o, final IndexIterator siter) {
		setDirty();
		if (o instanceof IDataset) {
			final IDataset ds = (IDataset) o;
			final int[] oshape = ds.getShape();

			if (!ShapeUtils.areShapesCompatible(siter.getShape(), oshape)) {
				throw new IllegalArgumentException(String.format(
						"Input dataset is not compatible with slice: %s cf %s", Arrays.toString(oshape),
						Arrays.toString(siter.getShape())));
			}

			if (ds instanceof Dataset) {
				final Dataset ads = (Dataset) ds;
				IndexIterator oiter = ads.getIterator();

				if (ds instanceof AbstractCompoundDataset) {
					if (isize != ads.getElementsPerItem()) {
						throw new IllegalArgumentException("Input dataset is not compatible with slice");
					}

					while (siter.hasNext() && oiter.hasNext()) {
						for (int i = 0; i < isize; i++) {
							data[siter.index + i] = ads.getElementDoubleAbs(oiter.index + i); // GET_ELEMENT_WITH_CAST
						}
					}
				} else {
					while (siter.hasNext() && oiter.hasNext()) {
						data[siter.index] = ads.getElementDoubleAbs(oiter.index); // GET_ELEMENT_WITH_CAST
						for (int i = 1; i < isize; i++) {
							data[siter.index + i] = 0;
						}
					}
				}
			} else {
				final IndexIterator oiter = new PositionIterator(oshape);
				final int[] pos = oiter.getPos();

				if (ds.getElementsPerItem() == 1) {
					while (siter.hasNext() && oiter.hasNext()) {
						data[siter.index] = ds.getDouble(pos); // PRIM_TYPE
						for (int i = 1; i < isize; i++) {
							data[siter.index + i] = 0;
						}
					}
				} else {
					while (siter.hasNext() && oiter.hasNext()) {
						final double[] val = DTypeUtils.toDoubleArray(ds.getObject(pos), isize); // PRIM_TYPE // CLASS_TYPE
						for (int i = 0; i < isize; i++) {
							data[siter.index + i] = val[i];
						}
					}
				}
			}
		} else {
			try {
				final double[] vr = DTypeUtils.toDoubleArray(o, isize); // PRIM_TYPE // CLASS_TYPE

				while (siter.hasNext()) {
					for (int i = 0; i < isize; i++) {
						data[siter.index + i] = vr[i];
					}
				}
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Object for setting slice is not a dataset or number");
			}
		}
		return this;
	}

	@Override
	public void copyItemsFromAxes(final int[] pos, final boolean[] axes, final Dataset dest) {
		double[] ddata = (double[]) dest.getBuffer(); // PRIM_TYPE

		if (dest.getElementsPerItem() != isize) {
			throw new IllegalArgumentException(String.format(
					"Destination dataset is incompatible as it has %d elements per item not %d",
					dest.getElementsPerItem(), isize));
		}

		SliceIterator siter = getSliceIteratorFromAxes(pos, axes);
		int[] sshape = ShapeUtils.squeezeShape(siter.getShape(), false);

		IndexIterator diter = dest.getSliceIterator(null, sshape, null);

		if (ddata.length < ShapeUtils.calcSize(sshape)) {
			throw new IllegalArgumentException("destination array is not large enough");
		}

		dest.setDirty();
		while (siter.hasNext() && diter.hasNext()) {
			for (int i = 0; i < isize; i++) {
				ddata[diter.index + i] = data[siter.index + i];
			}
		}
	}

	@Override
	public void setItemsOnAxes(final int[] pos, final boolean[] axes, final Object src) {
		setDirty();
		double[] sdata = (double[]) src; // PRIM_TYPE

		SliceIterator siter = getSliceIteratorFromAxes(pos, axes);

		if (sdata.length < ShapeUtils.calcSize(siter.getShape())) {
			throw new IllegalArgumentException("source array is not large enough");
		}

		for (int i = 0; siter.hasNext(); i++) {
			for (int j = 0; j < isize; j++) {
				data[siter.index + j] = sdata[isize * i + j];
			}
		}
	}

	@Override
	public boolean containsNans() {
		final IndexIterator iter = getIterator(); // REAL_ONLY
		while (iter.hasNext()) { // REAL_ONLY
			for (int i = 0; i < isize; i++) { // REAL_ONLY
				if (Double.isNaN(data[iter.index + i])) // CLASS_TYPE // REAL_ONLY
					return true; // REAL_ONLY
			} // REAL_ONLY
		} // REAL_ONLY
		return false;
	}

	@Override
	public boolean containsInfs() {
		final IndexIterator iter = getIterator(); // REAL_ONLY
		while (iter.hasNext()) { // REAL_ONLY
			for (int i = 0; i < isize; i++) { // REAL_ONLY
				if (Double.isInfinite(data[iter.index + i])) // CLASS_TYPE // REAL_ONLY
					return true; // REAL_ONLY
			} // REAL_ONLY
		} // REAL_ONLY
		return false;
	}

	@Override
	public boolean containsInvalidNumbers() {
		IndexIterator iter = getIterator(); // REAL_ONLY
		while (iter.hasNext()) { // REAL_ONLY
			for (int i = 0; i < isize; i++) { // REAL_ONLY
				double x = data[iter.index + i]; // PRIM_TYPE // REAL_ONLY
				if (Double.isNaN(x) || Double.isInfinite(x)) // CLASS_TYPE // REAL_ONLY
					return true; // REAL_ONLY
			} // REAL_ONLY
		} // REAL_ONLY
		return false;
	}

	@Override
	public CompoundDoubleDataset iadd(final Object b) {
		setDirty();
		Dataset bds = b instanceof Dataset ? (Dataset) b : DatasetFactory.createFromObject(b);
		boolean useLong = bds.getElementClass().equals(Long.class);
		int is = bds.getElementsPerItem();
		if (bds.getSize() == 1) {
			final IndexIterator it = getIterator();
			final int bOffset = bds.getOffset();
			if (is == 1) {
				if (useLong) {
					final long lb = bds.getElementLongAbs(bOffset);
					while (it.hasNext()) {
						for (int i = 0; i < isize; i++) {
							data[it.index + i] += lb;
						}
					}
				} else {
					final double db = bds.getElementDoubleAbs(bOffset);
					while (it.hasNext()) {
						for (int i = 0; i < isize; i++) {
							data[it.index + i] += db;
						}
					}
				}
			} else if (is == isize) {
				if (useLong) {
					while (it.hasNext()) {
						for (int i = 0; i < isize; i++) {
							data[it.index + i] += bds.getElementLongAbs(i);
						}
					}
				} else {
					while (it.hasNext()) {
						for (int i = 0; i < isize; i++) {
							data[it.index + i] += bds.getElementDoubleAbs(i);
						}
					}
				}
			} else {
				throw new IllegalArgumentException("Argument does not have same number of elements per item or is not a non-compound dataset");
			}
		} else {
			final BroadcastSelfIterator it = BroadcastSelfIterator.createIterator(this, bds);
			it.setOutputDouble(!useLong);
			if (is == 1) {
				if (useLong) {
					while (it.hasNext()) {
						final long lb = it.bLong;
						data[it.aIndex] += lb;
						for (int i = 1; i < isize; i++) {
							data[it.aIndex + i] += lb;
						}
					}
				} else {
					while (it.hasNext()) {
						final double db = it.bDouble;
						data[it.aIndex] += db;
						for (int i = 1; i < isize; i++) {
							data[it.aIndex + i] += db;
						}
					}
				}
			} else if (is == isize) {
				if (useLong) {
					while (it.hasNext()) {
						data[it.aIndex] += it.bLong;
						for (int i = 1; i < isize; i++) {
							data[it.aIndex + i] += bds.getElementLongAbs(it.bIndex + i);
						}
					}
				} else {
					while (it.hasNext()) {
						data[it.aIndex] += it.bDouble;
						for (int i = 1; i < isize; i++) {
							data[it.aIndex + i] += bds.getElementDoubleAbs(it.bIndex + i);
						}
					}
				}
			} else {
				throw new IllegalArgumentException("Argument does not have same number of elements per item or is not a non-compound dataset");
			}
		}
		return this;
	}

	@Override
	public CompoundDoubleDataset isubtract(final Object b) {
		setDirty();
		Dataset bds = b instanceof Dataset ? (Dataset) b : DatasetFactory.createFromObject(b);
		boolean useLong = bds.getElementClass().equals(Long.class);
		int is = bds.getElementsPerItem();
		if (bds.getSize() == 1) {
			final IndexIterator it = getIterator();
			final int bOffset = bds.getOffset();
			if (is == 1) {
				if (useLong) {
					final long lb = bds.getElementLongAbs(bOffset);
					while (it.hasNext()) {
						for (int i = 0; i < isize; i++) {
							data[it.index + i] -= lb;
						}
					}
				} else {
					final double db = bds.getElementDoubleAbs(bOffset);
					while (it.hasNext()) {
						for (int i = 0; i < isize; i++) {
							data[it.index + i] -= db;
						}
					}
				}
			} else if (is == isize) {
				if (useLong) {
					while (it.hasNext()) {
						for (int i = 0; i < isize; i++) {
							data[it.index + i] -= bds.getElementLongAbs(i);
						}
					}
				} else {
					while (it.hasNext()) {
						for (int i = 0; i < isize; i++) {
							data[it.index + i] -= bds.getElementDoubleAbs(i);
						}
					}
				}
			} else {
				throw new IllegalArgumentException("Argument does not have same number of elements per item or is not a non-compound dataset");
			}
		} else {
			final BroadcastSelfIterator it = BroadcastSelfIterator.createIterator(this, bds);
			it.setOutputDouble(!useLong);
			if (is == 1) {
				if (useLong) {
					while (it.hasNext()) {
						final long lb = it.bLong;
						data[it.aIndex] += lb;
						for (int i = 1; i < isize; i++) {
							data[it.aIndex + i] -= lb;
						}
					}
				} else {
					while (it.hasNext()) {
						final double db = it.bDouble;
						data[it.aIndex] += db;
						for (int i = 1; i < isize; i++) {
							data[it.aIndex + i] -= db;
						}
					}
				}
			} else if (is == isize) {
				if (useLong) {
					while (it.hasNext()) {
						data[it.aIndex] += it.bLong;
						for (int i = 1; i < isize; i++) {
							data[it.aIndex + i] -= bds.getElementLongAbs(it.bIndex + i);
						}
					}
				} else {
					while (it.hasNext()) {
						data[it.aIndex] += it.bDouble;
						for (int i = 1; i < isize; i++) {
							data[it.aIndex + i] -= bds.getElementDoubleAbs(it.bIndex + i);
						}
					}
				}
			} else {
				throw new IllegalArgumentException("Argument does not have same number of elements per item or is not a non-compound dataset");
			}
		}
		return this;
	}

	@Override
	public CompoundDoubleDataset imultiply(final Object b) {
		setDirty();
		Dataset bds = b instanceof Dataset ? (Dataset) b : DatasetFactory.createFromObject(b);
		boolean useLong = bds.getElementClass().equals(Long.class);
		int is = bds.getElementsPerItem();
		if (bds.getSize() == 1) {
			final IndexIterator it = getIterator();
			final int bOffset = bds.getOffset();
			if (useLong) {
				if (is == 1) {
					final long lb = bds.getElementLongAbs(bOffset);
					while (it.hasNext()) {
						for (int i = 0; i < isize; i++) {
							data[it.index + i] *= lb;
						}
					}
				} else if (is == isize) {
					while (it.hasNext()) {
						for (int i = 0; i < isize; i++) {
							data[it.index + i] *= bds.getElementLongAbs(i);
						}
					}
				} else {
					throw new IllegalArgumentException("Argument does not have same number of elements per item or is not a non-compound dataset");
				}
			} else {
				if (is == 1) {
					final double db = bds.getElementDoubleAbs(bOffset);
					while (it.hasNext()) {
						for (int i = 0; i < isize; i++) {
							data[it.index + i] *= db;
						}
					}
				} else if (is == isize) {
					while (it.hasNext()) {
						for (int i = 0; i < isize; i++) {
							data[it.index + i] *= bds.getElementDoubleAbs(i);
						}
					}
				} else {
					throw new IllegalArgumentException("Argument does not have same number of elements per item or is not a non-compound dataset");
				}
			}
		} else {
			final BroadcastSelfIterator it = BroadcastSelfIterator.createIterator(this, bds);
			it.setOutputDouble(!useLong);
			if (useLong) {
				if (is == 1) {
					while (it.hasNext()) {
						final double lb = it.bLong;
						for (int i = 0; i < isize; i++) {
							data[it.aIndex + i] *= lb;
						}
					}
				} else if (is == isize) {
					while (it.hasNext()) {
						data[it.aIndex] *= it.bLong;
						for (int i = 1; i < isize; i++) {
							data[it.aIndex + i] *= bds.getElementLongAbs(it.bIndex + i);
						}
					}
				} else {
					throw new IllegalArgumentException("Argument does not have same number of elements per item or is not a non-compound dataset");
				}
			} else {
				if (is == 1) {
					while (it.hasNext()) {
						final double db = it.bDouble;
						for (int i = 0; i < isize; i++) {
							data[it.aIndex + i] *= db;
						}
					}
				} else if (is == isize) {
					while (it.hasNext()) {
						data[it.aIndex] *= it.bDouble;
						for (int i = 1; i < isize; i++) {
							data[it.aIndex + i] *= bds.getElementDoubleAbs(it.bIndex + i);
						}
					}
				} else {
					throw new IllegalArgumentException("Argument does not have same number of elements per item or is not a non-compound dataset");
				}
			}
		}
		return this;
	}

	@Override
	public CompoundDoubleDataset idivide(final Object b) {
		setDirty();
		Dataset bds = b instanceof Dataset ? (Dataset) b : DatasetFactory.createFromObject(b);
		boolean useLong = bds.getElementClass().equals(Long.class);
		int is = bds.getElementsPerItem();
		if (bds.getSize() == 1) {
			final IndexIterator it = getIterator();
			final int bOffset = bds.getOffset();
			if (useLong) {
				if (is == 1) {
					final long lb = bds.getElementLongAbs(bOffset);
					// if (lb == 0) { // INT_USE
					// 	fill(0); // INT_USE
					// } else { // INT_USE
					while (it.hasNext()) {
						for (int i = 0; i < isize; i++) {
							data[it.index + i] /= lb;
						}
					}
					// } // INT_USE
				} else if (is == isize) {
					while (it.hasNext()) {
						for (int i = 0; i < isize; i++) {
							final long lb = bds.getElementLongAbs(i);
							data[it.index + i] /= lb; // INT_EXCEPTION
						}
					}
				} else {
					throw new IllegalArgumentException("Argument does not have same number of elements per item or is not a non-compound dataset");
				}
			} else {
				if (is == 1) {
					final double db = bds.getElementDoubleAbs(bOffset);
					// if (db == 0) { // INT_USE
					// 	fill(0); // INT_USE
					// } else { // INT_USE
					while (it.hasNext()) {
						for (int i = 0; i < isize; i++) {
							data[it.index + i] /= db;
						}
					}
					// } // INT_USE
				} else if (is == isize) {
					while (it.hasNext()) {
						for (int i = 0; i < isize; i++) {
							final double db = bds.getElementDoubleAbs(i);
							data[it.index + i] /= db; // INT_EXCEPTION
						}
					}
				} else {
					throw new IllegalArgumentException("Argument does not have same number of elements per item or is not a non-compound dataset");
				}
			}
		} else {
			final BroadcastSelfIterator it = BroadcastSelfIterator.createIterator(this, bds);
			it.setOutputDouble(!useLong);
			if (useLong) {
				if (is == 1) {
					while (it.hasNext()) {
						final long lb = it.bLong;
						// if (lb == 0) { // INT_USE
						// 	for (int i = 0; i < isize; i++) { // INT_USE
						// 		data[it.aIndex + i] = 0; // INT_USE
						// 	}// INT_USE
						// } else { // INT_USE
						for (int i = 0; i < isize; i++) {
							data[it.aIndex + i] /= lb;
						}
						// } // INT_USE
					}
				} else if (is == isize) {
					while (it.hasNext()) {
						for (int i = 0; i < isize; i++) {
							final long lb = bds.getElementLongAbs(it.bIndex + i);
							data[it.aIndex + i] /= lb; // INT_EXCEPTION
						}
					}
				} else {
					throw new IllegalArgumentException("Argument does not have same number of elements per item or is not a non-compound dataset");
				}
			} else {
				if (is == 1) {
					while (it.hasNext()) {
						final double db = it.bDouble;
						// if (db == 0) { // INT_USE
						// 	for (int i = 0; i < isize; i++) { // INT_USE
						// 		data[it.aIndex + i] = 0; // INT_USE
						// 	}// INT_USE
						// } else { // INT_USE
						for (int i = 0; i < isize; i++) {
							data[it.aIndex + i] /= db;
						}
						// } // INT_USE
					}
				} else if (is == isize) {
					while (it.hasNext()) {
						for (int i = 0; i < isize; i++) {
							final double db = bds.getElementDoubleAbs(it.bIndex + i);
							data[it.aIndex + i] /= db; // INT_EXCEPTION
						}
					}
				} else {
					throw new IllegalArgumentException("Argument does not have same number of elements per item or is not a non-compound dataset");
				}
			}
		}
		return this;
	}

	@Override
	public CompoundDoubleDataset ifloor() {
		setDirty(); // REAL_ONLY
		final IndexIterator it = getIterator(); // REAL_ONLY
		while (it.hasNext()) { // REAL_ONLY
			for (int i = 0; i < isize; i++) { // REAL_ONLY
				data[it.index + i] = Math.floor(data[it.index] + i); // REAL_ONLY // ADD_CAST
			} // REAL_ONLY
		} // REAL_ONLY
		return this;
	}

	@Override
	public CompoundDoubleDataset iremainder(final Object b) {
		setDirty();
		Dataset bds = b instanceof Dataset ? (Dataset) b : DatasetFactory.createFromObject(b);
		boolean useLong = bds.getElementClass().equals(Long.class);
		int is = bds.getElementsPerItem();
		if (bds.getSize() == 1) {
			final IndexIterator it = getIterator();
			final int bOffset = bds.getOffset();
			if (useLong) {
				if (is == 1) {
					final long lb = bds.getElementLongAbs(bOffset);
					// if (lb == 0) { // INT_USE
					// 	fill(0); // INT_USE
					// } else { // INT_USE
					while (it.hasNext()) {
						for (int i = 0; i < isize; i++) {
							data[it.index + i] %= lb;
						}
					}
					// } // INT_USE
				} else if (is == isize) {
					while (it.hasNext()) {
						for (int i = 0; i < isize; i++) {
							data[it.index + i] %= bds.getElementLongAbs(i); // INT_EXCEPTION
						}
					}
				} else {
					throw new IllegalArgumentException("Argument does not have same number of elements per item or is not a non-compound dataset");
				}
			} else {
				if (is == 1) {
					final double db = bds.getElementDoubleAbs(bOffset);
					// if (db == 0) { // INT_USE
					// 	fill(0); // INT_USE
					// } else { // INT_USE
					while (it.hasNext()) {
						for (int i = 0; i < isize; i++) {
							data[it.index + i] %= db;
						}
					}
					// } // INT_USE
				} else if (is == isize) {
					while (it.hasNext()) {
						for (int i = 0; i < isize; i++) {
							data[it.index + i] %= bds.getElementDoubleAbs(i); // INT_EXCEPTION
						}
					}
				} else {
					throw new IllegalArgumentException("Argument does not have same number of elements per item or is not a non-compound dataset");
				}
			}
		} else {
			final BroadcastSelfIterator it = BroadcastSelfIterator.createIterator(this, bds);
			it.setOutputDouble(!useLong);
			if (useLong) {
				if (is == 1) {
					while (it.hasNext()) {
						final long lb = it.bLong;
						// if (lb == 0) { // INT_USE
						// 	for (int i = 0; i < isize; i++) // INT_USE
						// 		data[it.aIndex + i] = 0; // INT_USE
						// } else { // INT_USE
						for (int i = 0; i < isize; i++)
							data[it.aIndex + i] %= lb;
						// } // INT_USE
					}
				} else if (is == isize) {
					while (it.hasNext()) {
						for (int i = 0; i < isize; i++) {
							final long lb = bds.getElementLongAbs(it.bIndex + i);
							data[it.aIndex + i] %= lb; // INT_EXCEPTION
						}
					}
				} else {
					throw new IllegalArgumentException("Argument does not have same number of elements per item or is not a non-compound dataset");
				}
			} else {
				if (is == 1) {
					while (it.hasNext()) {
						final double db = it.bDouble;
						// if (db == 0) { // INT_USE
						// 	for (int i = 0; i < isize; i++) // INT_USE
						// 		data[it.aIndex + i] = 0; // INT_USE
						// } else { // INT_USE
						for (int i = 0; i < isize; i++) {
							data[it.aIndex + i] %= db;
						}
						// } // INT_USE
					}
				} else if (is == isize) {
					while (it.hasNext()) {
						for (int i = 0; i < isize; i++) {
							final double db = bds.getElementDoubleAbs(it.bIndex + i);
							data[it.aIndex + i] %= db; // INT_EXCEPTION
						}
					}
				} else {
					throw new IllegalArgumentException("Argument does not have same number of elements per item or is not a non-compound dataset");
				}
			}
		}
		return this;
	}

	@Override
	public CompoundDoubleDataset ipower(final Object b) {
		setDirty();
		Dataset bds = b instanceof Dataset ? (Dataset) b : DatasetFactory.createFromObject(b);
		final int is = bds.getElementsPerItem();
		if (bds.getSize() == 1) {
			final int bOffset = bds.getOffset();
			final double vr = bds.getElementDoubleAbs(bOffset);
			final IndexIterator it = getIterator();
			if (bds.isComplex()) {
				final double vi = bds.getElementDoubleAbs(bOffset + 1);
				if (vi == 0) {
					while (it.hasNext()) {
						for (int i = 0; i < isize; i++) {
							final double v = Math.pow(data[it.index + i], vr);
							// if (Double.isInfinite(v) || Double.isNaN(v)) { // INT_USE
							// 	data[it.index + i] = 0; // INT_USE
							// } else { // INT_USE
							data[it.index + i] = v; // PRIM_TYPE_LONG // ADD_CAST
							// } // INT_USE
						}
					}
				} else {
					final Complex zv = new Complex(vr, vi);
					while (it.hasNext()) {
						for (int i = 0; i < isize; i++) {
							Complex zd = new Complex(data[it.index + i], 0);
							final double v = zd.pow(zv).getReal();
							// if (Double.isInfinite(v) || Double.isNaN(v)) { // INT_USE
							// 	data[it.index + i] = 0; // INT_USE
							// } else { // INT_USE
							data[it.index + i] = v; // PRIM_TYPE_LONG // ADD_CAST
							// } // INT_USE
						}
					}
				}
			} else if (is == 1) {
				while (it.hasNext()) {
					for (int i = 0; i < isize; i++) {
						final double v = Math.pow(data[it.index + i], vr);
						// if (Double.isInfinite(v) || Double.isNaN(v)) { // INT_USE
						// 	data[it.index + i] = 0; // INT_USE
						// } else { // INT_USE
						data[it.index + i] = v; // PRIM_TYPE_LONG // ADD_CAST
						// } // INT_USE
					}
				}
			} else if (is == isize) {
				while (it.hasNext()) {
					for (int i = 0; i < isize; i++) {
						final double v = Math.pow(data[it.index + i], bds.getElementDoubleAbs(i));
						// if (Double.isInfinite(v) || Double.isNaN(v)) { // INT_USE
						// 	data[it.index + i] = 0; // INT_USE
						// } else { // INT_USE
						data[it.index + i] = v; // PRIM_TYPE_LONG // ADD_CAST
						// } // INT_USE
					}
				}
			}
		} else {
			final BroadcastIterator it = BroadcastIterator.createIterator(this, bds);
			it.setOutputDouble(true);
			if (bds.isComplex()) {
				while (it.hasNext()) {
					final Complex zv = new Complex(it.bDouble, bds.getElementDoubleAbs(it.bIndex + 1));
					double v = new Complex(it.aDouble, 0).pow(zv).getReal();
					// if (Double.isInfinite(v) || Double.isNaN(v)) { // INT_USE
					// 	data[it.aIndex] = 0; // INT_USE
					// } else { // INT_USE
					data[it.aIndex] = v; // PRIM_TYPE_LONG // ADD_CAST
					// } // INT_USE
					for (int i = 1; i < isize; i++) {
						v = new Complex(data[it.aIndex + i], 0).pow(zv).getReal();
						// if (Double.isInfinite(v) || Double.isNaN(v)) { // INT_USE
						// 	data[it.aIndex + i] = 0; // INT_USE
						// } else { // INT_USE
						data[it.aIndex + i] = v; // PRIM_TYPE_LONG // ADD_CAST
						// } // INT_USE
					}
				}
			} else {
				while (it.hasNext()) {
					double v = Math.pow(it.aDouble, it.bDouble);
					// if (Double.isInfinite(v) || Double.isNaN(v)) { // INT_USE
					// 	data[it.aIndex] = 0; // INT_USE
					// } else { // INT_USE
					data[it.aIndex] = v; // PRIM_TYPE_LONG // ADD_CAST
					// } // INT_USE
					for (int i = 1; i < isize; i++) {
						v = Math.pow(data[it.aIndex + i], bds.getElementDoubleAbs(it.bIndex + i));
						// if (Double.isInfinite(v) || Double.isNaN(v)) { // INT_USE
						// 	data[it.aIndex + i] = 0; // INT_USE
						// } else { // INT_USE
						data[it.aIndex + i] = v; // PRIM_TYPE_LONG // ADD_CAST
						// } // INT_USE
					}
				}
			}
		}
		return this;
	}

	@Override
	public double residual(final Object b, final Dataset w, boolean ignoreNaNs) {
		Dataset bds = b instanceof Dataset ? (Dataset) b : DatasetFactory.createFromObject(b);
		final BroadcastIterator it = BroadcastIterator.createIterator(this, bds);
		it.setOutputDouble(true);
		double sum = 0;
		double comp = 0;
		final int bis = bds.getElementsPerItem();

		if (bis == 1) {
			if (w == null) {
				while (it.hasNext()) {
					final double db = it.bDouble;
					double diff = it.aDouble - db;
					if (ignoreNaNs) { // REAL_ONLY
						if (Double.isNaN(diff)) // REAL_ONLY
							continue; // REAL_ONLY
						boolean skip = false; // REAL_ONLY
						for (int i = 1; i < isize; i++) { // REAL_ONLY
							if (Double.isNaN(data[it.aIndex + i])) { // REAL_ONLY
								skip = true; // REAL_ONLY
								break; // REAL_ONLY
							} // REAL_ONLY
						} // REAL_ONLY
						if (skip) { // REAL_ONLY
							continue; // REAL_ONLY
						} // REAL_ONLY
					} // REAL_ONLY
					double err = diff * diff - comp;
					double temp = sum + err;
					comp = (temp - sum) - err;
					sum = temp;
					for (int i = 1; i < isize; i++) {
						diff = data[it.aIndex + i] - db;
						err = diff * diff - comp;
						temp = sum + err;
						comp = (temp - sum) - err;
						sum = temp;
					}
				}
			} else {
				IndexIterator itw = w.getIterator();
				while (it.hasNext() && itw.hasNext()) {
					final double db = it.bDouble;
					double diff = it.aDouble - db;
					if (ignoreNaNs) { // REAL_ONLY
						if (Double.isNaN(diff)) // REAL_ONLY
							continue; // REAL_ONLY
						boolean skip = false; // REAL_ONLY
						for (int i = 1; i < isize; i++) { // REAL_ONLY
							if (Double.isNaN(data[it.aIndex + i])) { // REAL_ONLY
								skip = true; // REAL_ONLY
								break; // REAL_ONLY
							} // REAL_ONLY
						} // REAL_ONLY
						if (skip) { // REAL_ONLY
							continue; // REAL_ONLY
						} // REAL_ONLY
					} // REAL_ONLY
					final double dw = w.getElementDoubleAbs(itw.index);
					double err = diff * diff * dw - comp;
					double temp = sum + err;
					comp = (temp - sum) - err;
					sum = temp;
					for (int i = 1; i < isize; i++) {
						diff = data[it.aIndex + i] - db;
						err = diff * diff * dw - comp;
						temp = sum + err;
						comp = (temp - sum) - err;
						sum = temp;
					}
				}
			}
		} else {
			if (w == null) {
				while (it.hasNext()) {
					double diff = it.aDouble - it.bDouble;
					if (ignoreNaNs) { // REAL_ONLY
						if (Double.isNaN(diff)) // REAL_ONLY
							continue; // REAL_ONLY
						boolean skip = false; // REAL_ONLY
						for (int i = 1; i < isize; i++) { // REAL_ONLY
							if (Double.isNaN(data[it.aIndex + i]) || Double.isNaN(bds.getElementDoubleAbs(it.bIndex + i))) { // REAL_ONLY
								skip = true; // REAL_ONLY
								break; // REAL_ONLY
							} // REAL_ONLY
						} // REAL_ONLY
						if (skip) { // REAL_ONLY
							continue; // REAL_ONLY
						} // REAL_ONLY
					} // REAL_ONLY
					double err = diff * diff - comp;
					double temp = sum + err;
					comp = (temp - sum) - err;
					sum = temp;
					for (int i = 1; i < isize; i++) {
						diff = data[it.aIndex + i] - bds.getElementDoubleAbs(it.bIndex + i);
						err = diff * diff - comp;
						temp = sum + err;
						comp = (temp - sum) - err;
						sum = temp;
					}
				}
			} else {
				IndexIterator itw = w.getIterator();
				while (it.hasNext() && itw.hasNext()) {
					double diff = it.aDouble - it.bDouble;
					if (ignoreNaNs) { // REAL_ONLY
						if (Double.isNaN(diff)) // REAL_ONLY
							continue; // REAL_ONLY
						boolean skip = false; // REAL_ONLY
						for (int i = 1; i < isize; i++) { // REAL_ONLY
							if (Double.isNaN(data[it.aIndex + i]) || Double.isNaN(bds.getElementDoubleAbs(it.bIndex + i))) { // REAL_ONLY
								skip = true; // REAL_ONLY
								break; // REAL_ONLY
							} // REAL_ONLY
						} // REAL_ONLY
						if (skip) { // REAL_ONLY
							continue; // REAL_ONLY
						} // REAL_ONLY
					} // REAL_ONLY
					final double dw = w.getElementDoubleAbs(itw.index);
					double err = diff * diff * dw - comp;
					double temp = sum + err;
					comp = (temp - sum) - err;
					sum = temp;
					for (int i = 1; i < isize; i++) {
						diff = data[it.aIndex + i] - bds.getElementDoubleAbs(it.bIndex + i);
						err = diff * diff * dw - comp;
						temp = sum + err;
						comp = (temp - sum) - err;
						sum = temp;
					}
				}
			}
		}
		return sum;
	}
}
