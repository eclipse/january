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

// This is generated from DoubleDataset.java by fromdouble.py

package org.eclipse.january.dataset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.january.metadata.StatisticsMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Extend dataset for boolean values // PRIM_TYPE
 */
public class BooleanDatasetBase extends AbstractDataset {
	// pin UID to base class
	private static final long serialVersionUID = Dataset.serialVersionUID;

	private static final Logger logger = LoggerFactory.getLogger(BooleanDatasetBase.class);

	protected boolean[] data; // subclass alias // PRIM_TYPE

	@Override
	protected void setData() {
		data = (boolean[]) odata; // PRIM_TYPE
	}

	protected static boolean[] createArray(final int size) { // PRIM_TYPE
		boolean[] array = null; // PRIM_TYPE

		try {
			array = new boolean[size]; // PRIM_TYPE
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
	BooleanDatasetBase() {
	}

	/**
	 * Create a zero-filled dataset of given shape
	 * @param shape
	 */
	BooleanDatasetBase(final int... shape) {
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
	 * Create a dataset using given data
	 * @param data
	 * @param shape
	 *            (can be null to create 1D dataset)
	 */
	BooleanDatasetBase(final boolean[] data, int... shape) { // PRIM_TYPE
		if (data == null) {
			throw new IllegalArgumentException("Data must not be null");
		}
		if (shape == null || shape.length == 0) {
			shape = new int[] { data.length };
		}
		size = ShapeUtils.calcSize(shape);
		if (size != data.length) {
			throw new IllegalArgumentException(String.format("Shape %s is not compatible with size of data array, %d",
					Arrays.toString(shape), data.length));
		}
		this.shape = size == 0 ? null : shape.clone();

		odata = this.data = data;
	}

	/**
	 * Copy a dataset
	 * @param dataset
	 */
	BooleanDatasetBase(final BooleanDatasetBase dataset) {
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
				for (int i = 0; iter.hasNext(); i++) {
					data[i] = dataset.data[iter.index];
				}
			}
		} catch (Throwable t) {
			logger.error("Could not create a dataset of shape {}", Arrays.toString(shape), t);
			throw new IllegalArgumentException(t);
		}
	}

	/**
	 * Copy and cast a dataset to this class type
	 * @param dataset
	 */
	BooleanDatasetBase(final Dataset dataset) {
		copyToView(dataset, this, true, false);
		offset = 0;
		stride = null;
		base = null;
		try {
			odata = data = createArray(size);
		} catch (Throwable t) {
			logger.error("Could not create a dataset of shape {}", Arrays.toString(shape), t);
			throw new IllegalArgumentException(t);
		}
		IndexIterator iter = dataset.getIterator();
		for (int i = 0; iter.hasNext(); i++) {
			data[i] = dataset.getElementBooleanAbs(iter.index); // GET_ELEMENT_WITH_CAST
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

		BooleanDatasetBase other = (BooleanDatasetBase) obj;
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
			if (data[iter.index] != other.data[oiter.index]) { // OBJECT_UNEQUAL
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public BooleanDatasetBase clone() {
		return new BooleanDatasetBase(this);
	}

	/**
	 * Create a dataset from an object which could be a Java list, array (of arrays...) or Number. Ragged
	 * sequences or arrays are padded with zeros.
	 *
	 * @param obj
	 * @return dataset with contents given by input
	 */
	static BooleanDatasetBase createFromObject(final Object obj) {
		BooleanDatasetBase result = new BooleanDatasetBase();

		if (obj != null) {
			result.shape = ShapeUtils.getShapeFromObject(obj);
			result.size = ShapeUtils.calcSize(result.shape);

			try {
				result.odata = result.data = createArray(result.size);
			} catch (Throwable t) {
				logger.error("Could not create a dataset of shape {}", Arrays.toString(result.shape), t);
				throw new IllegalArgumentException(t);
			}

			int[] pos = new int[result.shape.length];
			result.fillData(obj, 0, pos);
		}

		return result;
	}

	/**
	 * @param shape
	 * @return a dataset filled with ones
	 */
	static BooleanDatasetBase ones(final int... shape) {
		return new BooleanDatasetBase(shape).fill(1);
	}

	@Override
	public BooleanDatasetBase fill(final Object obj) {
		setDirty();
		boolean dv = DTypeUtils.toBoolean(obj); // PRIM_TYPE // FROM_OBJECT
		IndexIterator iter = getIterator();
		while (iter.hasNext()) {
			data[iter.index] = dv;
		}

		return this;
	}

	/**
	 * This is a typed version of {@link #getBuffer()}
	 * @return data buffer as linear array
	 */
	public boolean[] getData() { // PRIM_TYPE
		return data;
	}

	@Override
	protected int getBufferLength() {
		if (data == null)
			return 0;
		return data.length;
	}

	@Override
	public BooleanDatasetBase getView(boolean deepCopyMetadata) {
		BooleanDatasetBase view = new BooleanDatasetBase();
		copyToView(this, view, true, deepCopyMetadata);
		view.setData();
		return view;
	}

	/**
	 * Get a value from an absolute index of the internal array. This is an internal method with no checks so can be
	 * dangerous. Use with care or ideally with an iterator.
	 *
	 * @param index
	 *            absolute index
	 * @return value
	 */
	public boolean getAbs(final int index) { // PRIM_TYPE
		return data[index];
	}

	@Override
	public boolean getElementBooleanAbs(final int index) {
		return false;
	}

	@Override
	public double getElementDoubleAbs(final int index) {
		return 0;
	}

	@Override
	public long getElementLongAbs(final int index) {
		return 0;
	}

	@Override
	public Object getObjectAbs(final int index) {
		return data[index];
	}

	@Override
	public String getStringAbs(final int index) {
		return stringFormat == null ? String.format("%b", data[index]) : // FORMAT_STRING
			stringFormat.format(data[index]);
	}

	/**
	 * Set a value at absolute index in the internal array. This is an internal method with no checks so can be
	 * dangerous. Use with care or ideally with an iterator.
	 *
	 * @param index
	 *            absolute index
	 * @param val
	 *            new value
	 */
	public void setAbs(final int index, final boolean val) { // PRIM_TYPE
		setDirty();
		data[index] = val;
	}

	@Override
	protected void setItemDirect(final int dindex, final int sindex, final Object src) {
		setDirty();
		boolean[] dsrc = (boolean[]) src; // PRIM_TYPE
		data[dindex] = dsrc[sindex];
	}

	@Override
	public void setObjectAbs(final int index, final Object obj) {
		if (index < 0 || index > data.length) {
			throw new IndexOutOfBoundsException("Index given is outside dataset");
		}

		setAbs(index, DTypeUtils.toBoolean(obj)); // FROM_OBJECT
	}

	/**
	 * @return item in first position
	 * @since 2.0
	 */
	public boolean get() { // PRIM_TYPE
		return data[getFirst1DIndex()];
	}

	/**
	 * @param i position in first dimension
	 * @return item in given position
	 */
	public boolean get(final int i) { // PRIM_TYPE
		return data[get1DIndex(i)];
	}

	/**
	 * @param i position in first dimension
	 * @param j position in second dimension
	 * @return item in given position
	 */
	public boolean get(final int i, final int j) { // PRIM_TYPE
		return data[get1DIndex(i, j)];
	}

	/**
	 * @param pos position
	 * @return item in given position
	 */
	public boolean get(final int... pos) { // PRIM_TYPE
		return data[get1DIndex(pos)];
	}

	@Override
	public Object getObject() {
		return Boolean.valueOf(get()); // CLASS_TYPE
	}

	@Override
	public Object getObject(final int i) {
		return Boolean.valueOf(get(i)); // CLASS_TYPE
	}

	@Override
	public Object getObject(final int i, final int j) {
		return Boolean.valueOf(get(i, j)); // CLASS_TYPE
	}

	@Override
	public Object getObject(final int... pos) {
		return Boolean.valueOf(get(pos)); // CLASS_TYPE
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
	public double getDouble() {
		return 0;
	}

	@Override
	public double getDouble(final int i) {
		return 0;
	}

	@Override
	public double getDouble(final int i, final int j) {
		return 0;
	}

	@Override
	public double getDouble(final int... pos) {
		return 0;
	}

	@Override
	public float getFloat() {
		return 0;
	}

	@Override
	public float getFloat(final int i) {
		return 0;
	}

	@Override
	public float getFloat(final int i, final int j) {
		return 0;
	}

	@Override
	public float getFloat(final int... pos) {
		return 0;
	}

	@Override
	public long getLong() {
		return 0;
	}

	@Override
	public long getLong(final int i) {
		return 0;
	}

	@Override
	public long getLong(final int i, final int j) {
		return 0;
	}

	@Override
	public long getLong(final int... pos) {
		return 0;
	}

	@Override
	public int getInt() {
		return 0;
	}

	@Override
	public int getInt(final int i) {
		return 0;
	}

	@Override
	public int getInt(final int i, final int j) {
		return 0;
	}

	@Override
	public int getInt(final int... pos) {
		return 0;
	}

	@Override
	public short getShort() {
		return 0;
	}

	@Override
	public short getShort(final int i) {
		return 0;
	}

	@Override
	public short getShort(final int i, final int j) {
		return 0;
	}

	@Override
	public short getShort(final int... pos) {
		return 0;
	}

	@Override
	public byte getByte() {
		return 0;
	}

	@Override
	public byte getByte(final int i) {
		return 0;
	}

	@Override
	public byte getByte(final int i, final int j) {
		return 0;
	}

	@Override
	public byte getByte(final int... pos) {
		return 0;
	}

	@Override
	public boolean getBoolean() {
		return false;
	}

	@Override
	public boolean getBoolean(final int i) {
		return false;
	}

	@Override
	public boolean getBoolean(final int i, final int j) {
		return false;
	}

	@Override
	public boolean getBoolean(final int... pos) {
		return false;
	}

	/**
	 * Sets the value at first point to the passed value. The dataset must not be null
	 *
	 * @param value to set
	 * @since 2.0
	 */
	public void setItem(final boolean value) { // PRIM_TYPE
		setAbs(getFirst1DIndex(), value);
	}

	/**
	 * Sets the value at a particular point to the passed value. The dataset must be 1D
	 *
	 * @param value to set
	 * @param i position in first dimension
	 */
	public void setItem(final boolean value, final int i) { // PRIM_TYPE
		setAbs(get1DIndex(i), value);
	}

	/**
	 * Sets the value at a particular point to the passed value. The dataset must be 2D
	 *
	 * @param value to set
	 * @param i position in first dimension
	 * @param j position in second dimension
	 */
	public void setItem(final boolean value, final int i, final int j) { // PRIM_TYPE
		setAbs(get1DIndex(i, j), value);
	}

	/**
	 * Sets the value at a particular point to the passed value
	 *
	 * @param value to set
	 * @param pos position
	 */
	public void setItem(final boolean value, final int... pos) { // PRIM_TYPE
		setAbs(get1DIndex(pos), value);
	}

	@Override
	public void set(final Object obj) {
		setItem(DTypeUtils.toBoolean(obj)); // FROM_OBJECT
	}

	@Override
	public void set(final Object obj, final int i) {
		setItem(DTypeUtils.toBoolean(obj), i); // FROM_OBJECT
	}

	@Override
	public void set(final Object obj, final int i, final int j) {
		setItem(DTypeUtils.toBoolean(obj), i, j); // FROM_OBJECT
	}

	@Override
	public void set(final Object obj, int... pos) {
		if (pos == null || (pos.length == 0 && shape.length > 0)) {
			pos = new int[shape.length];
		}

		setItem(DTypeUtils.toBoolean(obj), pos); // FROM_OBJECT
	}

	@Override
	public void resize(int... newShape) {
		setDirty();
		final IndexIterator iter = getIterator();
		final int nsize = ShapeUtils.calcSize(newShape);
		final boolean[] ndata; // PRIM_TYPE
		try {
			ndata = createArray(nsize);
		} catch (Throwable t) {
			logger.error("Could not create a dataset of shape {}", Arrays.toString(shape), t);
			throw new IllegalArgumentException(t);
		}
		for (int i = 0; iter.hasNext() && i < nsize; i++) {
			ndata[i] = data[iter.index];
		}

		odata = data = ndata;
		size = nsize;
		shape = newShape;
		stride = null;
		offset = 0;
		base = null;
	}

	@Override
	public BooleanDatasetBase sort(Integer axis) {
		throw new UnsupportedOperationException("Cannot sort dataset"); // BOOLEAN_USE
	}

	@Override
	public BooleanDatasetBase getUniqueItems() {
		Set<Boolean> set = new TreeSet<Boolean>(); // CLASS_TYPE
		IndexIterator it = getIterator();
		while (it.hasNext()) {
			set.add(data[it.index]);
		}

		BooleanDataset u = new BooleanDataset(set.size()); // CLASS_TYPE
		int i = 0;
		boolean[] udata = u.getData(); // PRIM_TYPE
		for (Boolean v : set) { // CLASS_TYPE
			udata[i++] = v;
		}
		return u;
	}

	@Override
	public BooleanDatasetBase getSlice(final SliceIterator siter) {
		BooleanDatasetBase result = new BooleanDatasetBase(siter.getShape());
		boolean[] rdata = result.data; // PRIM_TYPE

		for (int i = 0; siter.hasNext(); i++)
			rdata[i] = data[siter.index];

		result.setName(name + BLOCK_OPEN + Slice.createString(siter.shape, siter.start, siter.stop, siter.step) + BLOCK_CLOSE);
		return result;
	}

	@Override
	public void fillDataset(Dataset result, IndexIterator iter) {
		IndexIterator riter = result.getIterator();
		result.setDirty();

		boolean[] rdata = ((BooleanDatasetBase) result).data; // PRIM_TYPE

		while (riter.hasNext() && iter.hasNext()) {
			rdata[riter.index] = data[iter.index];
		}
	}

	@Override
	public BooleanDatasetBase setByBoolean(final Object obj, Dataset selection) {
		setDirty();
		if (obj instanceof Dataset) {
			final Dataset ds = (Dataset) obj;
			final int length = ((Number) selection.sum()).intValue();
			if (length != ds.getSize()) {
				throw new IllegalArgumentException(
						"Number of true items in selection does not match number of items in dataset");
			}

			final IndexIterator oiter = ds.getIterator();
			final BooleanIterator biter = getBooleanIterator(selection);

			while (biter.hasNext() && oiter.hasNext()) {
				data[biter.index] = ds.getElementBooleanAbs(oiter.index); // GET_ELEMENT_WITH_CAST
			}
		} else {
			final boolean dv = DTypeUtils.toBoolean(obj); // PRIM_TYPE // FROM_OBJECT
			final BooleanIterator biter = getBooleanIterator(selection);

			while (biter.hasNext()) {
				data[biter.index] = dv;
			}
		}
		return this;
	}

	@Override
	public BooleanDatasetBase setBy1DIndex(final Object obj, final Dataset index) {
		setDirty();
		if (obj instanceof Dataset) {
			final Dataset ds = (Dataset) obj;
			if (index.getSize() != ds.getSize()) {
				throw new IllegalArgumentException(
						"Number of items in index dataset does not match number of items in dataset");
			}

			final IndexIterator oiter = ds.getIterator();
			final IntegerIterator iter = new IntegerIterator(index, size);

			while (iter.hasNext() && oiter.hasNext()) {
				data[iter.index] = ds.getElementBooleanAbs(oiter.index); // GET_ELEMENT_WITH_CAST
			}
		} else {
			final boolean dv = DTypeUtils.toBoolean(obj); // PRIM_TYPE // FROM_OBJECT
			IntegerIterator iter = new IntegerIterator(index, size);

			while (iter.hasNext()) {
				data[iter.index] = dv;
			}
		}
		return this;
	}

	@Override
	public BooleanDatasetBase setByIndexes(final Object obj, final Object... indexes) {
		setDirty();
		final IntegersIterator iter = new IntegersIterator(shape, indexes);
		final int[] pos = iter.getPos();

		if (obj instanceof Dataset) {
			final Dataset ds = (Dataset) obj;
			if (ShapeUtils.calcSize(iter.getShape()) != ds.getSize()) {
				throw new IllegalArgumentException(
						"Number of items in index datasets does not match number of items in dataset");
			}

			final IndexIterator oiter = ds.getIterator();

			while (iter.hasNext() && oiter.hasNext()) {
				setItem(ds.getElementBooleanAbs(oiter.index), pos); // GET_ELEMENT_WITH_CAST
			}
		} else {
			final boolean dv = DTypeUtils.toBoolean(obj); // PRIM_TYPE // FROM_OBJECT

			while (iter.hasNext()) {
				setItem(dv, pos);
			}
		}
		return this;
	}

	@Override
	BooleanDatasetBase setSlicedView(Dataset view, Dataset d) {
		setDirty();
		final BroadcastSelfIterator it = BroadcastSelfIterator.createIterator(view, d);

		while (it.hasNext()) {
			data[it.aIndex] = d.getElementBooleanAbs(it.bIndex);
		}
		return this;
	}

	@Override
	public BooleanDatasetBase setSlice(final Object obj, final IndexIterator siter) {
		setDirty();

		if (obj instanceof IDataset) {
			final IDataset ds = (IDataset) obj;
			final int[] oshape = ds.getShape();

			if (!ShapeUtils.areShapesCompatible(siter.getShape(), oshape)) {
				throw new IllegalArgumentException(String.format(
						"Input dataset is not compatible with slice: %s cf %s", Arrays.toString(oshape),
						Arrays.toString(siter.getShape())));
			}

			if (ds instanceof Dataset) {
				final Dataset ads = (Dataset) ds;
				final IndexIterator oiter = ads.getIterator();

				while (siter.hasNext() && oiter.hasNext())
					data[siter.index] = ads.getElementBooleanAbs(oiter.index); // GET_ELEMENT_WITH_CAST
			} else {
				final IndexIterator oiter = new PositionIterator(oshape);
				final int[] pos = oiter.getPos();

				while (siter.hasNext() && oiter.hasNext())
					data[siter.index] = ds.getBoolean(pos); // PRIM_TYPE
			}
		} else {
			try {
				boolean v = DTypeUtils.toBoolean(obj); // PRIM_TYPE // FROM_OBJECT

				while (siter.hasNext())
					data[siter.index] = v;
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Object for setting slice is not a dataset or number");
			}
		}
		return this;
	}

	@Override
	public void copyItemsFromAxes(final int[] pos, final boolean[] axes, final Dataset dest) {
		boolean[] ddata = (boolean[]) dest.getBuffer(); // PRIM_TYPE

		SliceIterator siter = getSliceIteratorFromAxes(pos, axes);
		int[] sshape = ShapeUtils.squeezeShape(siter.getShape(), false);

		IndexIterator diter = dest.getSliceIterator(null, sshape, null);

		if (ddata.length < ShapeUtils.calcSize(sshape)) {
			throw new IllegalArgumentException("destination array is not large enough");
		}

		dest.setDirty();
		while (siter.hasNext() && diter.hasNext()) {
			ddata[diter.index] = data[siter.index];
		}
	}

	@Override
	public void setItemsOnAxes(final int[] pos, final boolean[] axes, final Object src) {
		setDirty();
		boolean[] sdata = (boolean[]) src; // PRIM_TYPE

		SliceIterator siter = getSliceIteratorFromAxes(pos, axes);

		if (sdata.length < ShapeUtils.calcSize(siter.getShape())) {
			throw new IllegalArgumentException("destination array is not large enough");
		}

		for (int i = 0; siter.hasNext(); i++) {
			data[siter.index] = sdata[i];
		}
	}

	private List<int[]> findPositions(final boolean value) { // PRIM_TYPE
		IndexIterator iter = getIterator(true);
		List<int[]> posns = new ArrayList<int[]>();
		int[] pos = iter.getPos();

		{
			while (iter.hasNext()) {
				if (data[iter.index] == value) {
					posns.add(pos.clone());
				}
			}
		}
		return posns;
	}

	@Override
	public int[] maxPos(boolean... ignoreInvalids) {
		StatisticsMetadata<Number> md = getStats(); // BOOLEAN_USE
		// StatisticsMetadata<String> md = getStringStats(); // OBJECT_USE
		List<int[]> max = md.getMaximumPositions(ignoreInvalids);

		if (max == null) {
			max = findPositions(md.getMaximum(ignoreInvalids).intValue() != 0); // BOOLEAN_USE
			// max = findPositions(md.getMaximum(ignoreInvalids).toString()); // OBJECT_USE

			md.setMaximumPositions(max);
		}

		return max.get(0); // first maximum
	}

	@Override
	public int[] minPos(boolean... ignoreInvalids) {
		StatisticsMetadata<Number> md = getStats(); // BOOLEAN_USE
		// StatisticsMetadata<String> md = getStringStats(); // OBJECT_USE
		List<int[]> min = md.getMinimumPositions(ignoreInvalids);

		if (min == null) {
			min = findPositions(md.getMinimum(ignoreInvalids).intValue() != 0); // BOOLEAN_USE
			// min = findPositions(md.getMinimum(ignoreInvalids).toString()); // OBJECT_USE

			md.setMinimumPositions(min);
		}

		return min.get(0); // first minimum
	}

	@Override
	public boolean containsNans() {
		return false;
	}

	@Override
	public boolean containsInfs() {
		return false;
	}

	@Override
	public boolean containsInvalidNumbers() {
		return false;
	}

	@Override
	public BooleanDatasetBase iadd(final Object b) {
		return this;
	}

	@Override
	public BooleanDatasetBase isubtract(final Object b) {
		return this;
	}

	@Override
	public BooleanDatasetBase imultiply(final Object b) {
		return this;
	}

	@Override
	public BooleanDatasetBase idivide(final Object b) {
		return this;
	}

	@Override
	public BooleanDatasetBase ifloor() {
		return this;
	}

	@Override
	public BooleanDatasetBase iremainder(final Object b) {
		return this;
	}

	@Override
	public BooleanDatasetBase ipower(final Object b) {
		return this;
	}

	@Override
	public double residual(final Object b, final Dataset w, boolean ignoreNaNs) {
		double sum = 0;
		return sum;
	}
}
