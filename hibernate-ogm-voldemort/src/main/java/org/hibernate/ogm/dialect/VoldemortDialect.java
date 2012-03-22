/* 
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.hibernate.ogm.dialect;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.impl.EmptyTupleSnapshot;
import org.hibernate.ogm.datastore.impl.MapBasedTupleSnapshot;
import org.hibernate.ogm.datastore.impl.MapHelpers;
import org.hibernate.ogm.datastore.mapbased.impl.MapAssociationSnapshot;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.voldemort.impl.VoldemortDatastoreProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.persister.entity.Lockable;

/**
 * @author Seiya Kawashima <skawashima@uchicago.edu>
 */
public class VoldemortDialect implements GridDialect {

	private final VoldemortDatastoreProvider provider;
	private Log log = LoggerFactory.make();

	public VoldemortDialect(VoldemortDatastoreProvider provider) {
		this.provider = provider;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.hibernate.ogm.dialect.GridDialect#getLockingStrategy(org.hibernate
	 * .persister.entity.Lockable, org.hibernate.LockMode)
	 */
	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable,
			LockMode lockMode) {

		/**
		 * TODO Voldemort itself doesn't expose Lock objects, so for right now
		 * simple throws an exception here. However, need to confirm if this is
		 * the right implementation.
		 */
		throw new HibernateException("Lock " + lockMode
				+ " is not supported on Voldemort.");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.hibernate.ogm.dialect.GridDialect#getTuple(org.hibernate.ogm.grid
	 * .EntityKey)
	 */
	@Override
	public Tuple getTuple(EntityKey key) {
		Map<String, Object> entityMap = this.provider.getEntityTuple(key);
		if (entityMap == null) {
			return null;
		}

		return new Tuple(new MapBasedTupleSnapshot(entityMap));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.hibernate.ogm.dialect.GridDialect#createTuple(org.hibernate.ogm.grid
	 * .EntityKey)
	 */
	@Override
	public Tuple createTuple(EntityKey key) {
		HashMap<String, Object> tuple = new HashMap<String, Object>();
		return new Tuple(new MapBasedTupleSnapshot(tuple));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.hibernate.ogm.dialect.GridDialect#updateTuple(org.hibernate.ogm.datastore
	 * .spi.Tuple, org.hibernate.ogm.grid.EntityKey)
	 */
	@Override
	public void updateTuple(Tuple tuple, EntityKey key) {
		Map<String, Object> entityRecord = ((MapBasedTupleSnapshot) tuple
				.getSnapshot()).getMap();
		MapHelpers.applyTupleOpsOnMap(tuple, entityRecord);
		this.provider.putEntity(key, tuple.getSnapShotAsMap());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.hibernate.ogm.dialect.GridDialect#removeTuple(org.hibernate.ogm.grid
	 * .EntityKey)
	 */
	@Override
	public void removeTuple(EntityKey key) {
		this.provider.removeEntityTuple(key);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.hibernate.ogm.dialect.GridDialect#getAssociation(org.hibernate.ogm
	 * .grid.AssociationKey)
	 */
	@Override
	public Association getAssociation(AssociationKey key) {
		Map<RowKey, Map<String, Object>> associationMap = this.provider
				.getAssociation(key);
		return associationMap == null ? null : new Association(
				new MapAssociationSnapshot(associationMap));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.hibernate.ogm.dialect.GridDialect#createAssociation(org.hibernate
	 * .ogm.grid.AssociationKey)
	 */
	@Override
	public Association createAssociation(AssociationKey key) {
		Map<RowKey, Map<String, Object>> associationMap = new HashMap<RowKey, Map<String, Object>>();
		return new Association(new MapAssociationSnapshot(associationMap));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.hibernate.ogm.dialect.GridDialect#updateAssociation(org.hibernate
	 * .ogm.datastore.spi.Association, org.hibernate.ogm.grid.AssociationKey)
	 */
	@Override
	public void updateAssociation(Association association, AssociationKey key) {
		MapHelpers.updateAssociation(association, key);
		this.provider.putAssociation(key, association.getAssociationAsMap());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.hibernate.ogm.dialect.GridDialect#removeAssociation(org.hibernate
	 * .ogm.grid.AssociationKey)
	 */
	@Override
	public void removeAssociation(AssociationKey key) {
		this.provider.removeAssociation(key);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.hibernate.ogm.dialect.GridDialect#createTupleAssociation(org.hibernate
	 * .ogm.grid.AssociationKey, org.hibernate.ogm.grid.RowKey)
	 */
	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey,
			RowKey rowKey) {
		return new Tuple(EmptyTupleSnapshot.SINGLETON);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.hibernate.ogm.dialect.GridDialect#nextValue(org.hibernate.ogm.grid
	 * .RowKey, org.hibernate.id.IntegralDataTypeHolder, int, int)
	 */
	@Override
	public void nextValue(RowKey key, IntegralDataTypeHolder value,
			int increment, int initialValue) {
		this.provider.setNextValue(key, value, increment, initialValue);
	}

}
