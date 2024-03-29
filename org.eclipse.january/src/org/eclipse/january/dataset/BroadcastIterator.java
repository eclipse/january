/*-
 * Copyright 2015, 2016 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.january.dataset;

import java.util.Arrays;

/**
 * Base class for broadcast iterators of pairs with output.<p>
 * For speed, there are public members. Note, index is not updated
 */
public abstract class BroadcastIterator extends BroadcastIteratorBase {

	/**
	 * @param a dataset to iterate over
	 * @param b dataset to iterate over
	 * @return broadcast iterator
	 */
	public static BroadcastIterator createIterator(Dataset a, Dataset b) {
		return createIterator(a, b, null, false);
	}

	/**
	 * @param a dataset to iterate over
	 * @param b dataset to iterate over
	 * @param o output (can be null for new dataset, or a)
	 * @return broadcast iterator
	 */
	public static BroadcastIterator createIterator(Dataset a, Dataset b, Dataset o) {
		return createIterator(a, b, o, false);
	}

	/**
	 * @param a dataset to iterate over
	 * @param b dataset to iterate over
	 * @param o output (can be null for new dataset, or a)
	 * @param createIfNull if true, create new dataset if o is null
	 * @return broadcast iterator
	 */
	public static BroadcastIterator createIterator(Dataset a, Dataset b, Dataset o, boolean createIfNull) {
		if (Arrays.equals(a.getShapeRef(), b.getShapeRef()) && a.getStrides() == null && b.getStrides() == null) {
			if (o == null || (o.getStrides() == null && Arrays.equals(a.getShapeRef(), o.getShapeRef()))) {
				return new ContiguousPairIterator(a, b, o, createIfNull);
			}
		}
		return new BroadcastPairIterator(a, b, o, createIfNull);
	}

	/**
	 * Index in output dataset
	 */
	public int oIndex;
	/**
	 * Current value in first dataset
	 */
	public double aDouble;
	/**
	 * Current value in first dataset
	 */
	public long aLong;
	/**
	 * Output dataset
	 */
	protected Dataset oDataset;

	final protected boolean outputA;
	final protected boolean outputB;

	/**
	 * @param a dataset to iterate over
	 * @param b dataset to iterate over
	 * @param o output (can be null for new dataset, or a)
	 */
	protected BroadcastIterator(Dataset a, Dataset b, Dataset o) {
		super(a, b);
		oDataset = o;
		outputA = a == o;
		outputB = b == o;
		read = InterfaceUtils.isNumerical(a.getClass()) && InterfaceUtils.isNumerical(b.getClass());
		asDouble = aDataset.hasFloatingPointElements() || bDataset.hasFloatingPointElements();
		BroadcastUtils.checkItemSize(a, b, o);
		if (o != null) {
			o.setDirty();
		}
	}

	/**
	 * @return output dataset (can be null)
	 */
	public Dataset getOutput() {
		return oDataset;
	}

	@Override
	protected void storeCurrentValues() {
		if (aIndex >= 0) {
			if (asDouble) {
				aDouble = aDataset.getElementDoubleAbs(aIndex);
			} else {
				aLong = aDataset.getElementLongAbs(aIndex);
			}
		}
		if (bIndex >= 0) {
			if (asDouble) {
				bDouble = bDataset.getElementDoubleAbs(bIndex);
			} else {
				bLong = bDataset.getElementLongAbs(bIndex);
			}
		}
	}
}
