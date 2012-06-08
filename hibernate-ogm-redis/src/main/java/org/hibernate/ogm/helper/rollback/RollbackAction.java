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

package org.hibernate.ogm.helper.rollback;

import java.util.Map;

import org.hibernate.ogm.datastore.redis.impl.RedisDatastoreProvider;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.helper.memento.CareTaker;
import org.hibernate.ogm.helper.memento.Memento;
import org.hibernate.ogm.helper.memento.Originator;

/**
 * @author Seiya Kawashima <skawashima@uchicago.edu>
 */
public class RollbackAction {
	
	private final Originator originator;
	private final CareTaker careTaker;
	private final RedisDatastoreProvider redisDatastoreProvider;
	private final Object key;

	public RollbackAction(RedisDatastoreProvider redisDatastoreProvider, Object key) {
		this.key = key;
		this.redisDatastoreProvider = redisDatastoreProvider;
		Object[] objs = setSavePoint( key );
		originator = (Originator) objs[0];
		careTaker = (CareTaker) objs[1];
	}

	/**
	 * Roll back to the previous state.
	 * 
	 * @param key
	 */
	public void rollback() {

		originator.undo( careTaker.getMemento() );

		if ( key instanceof EntityKey ) {

			if ( originator.getObj() == null ) {
				redisDatastoreProvider.putEntity( (EntityKey) key, null );
			}
			else {
				redisDatastoreProvider.putEntity( (EntityKey) key, (Map<String, Object>) originator.getObj() );
			}
		}
		else if ( key instanceof AssociationKey ) {

			if ( originator.getObj() == null ) {
				redisDatastoreProvider.putAssociation( (AssociationKey) key, null );
			}
			else {
				redisDatastoreProvider.putAssociation( (AssociationKey) key,
						(Map<RowKey, Map<String, Object>>) originator.getObj() );
			}
		}
		else if ( key instanceof RowKey ) {

			if ( originator.getObj() == null ) {
				redisDatastoreProvider.putSequence( (RowKey) key, null );
			}
			else {
				redisDatastoreProvider.putSequence( (RowKey) key, (Map<String, Integer>) originator.getObj() );
			}
		}
	}

	/**
	 * Saves the current state for the possible rollback.
	 * @param key
	 * @return
	 */
	private Object[] setSavePoint(Object key) {

		Object[] objs = new Object[2];
		Originator o = new Originator();
		Memento memento = null;

		if ( key instanceof EntityKey ) {
			memento = o.createMemento( redisDatastoreProvider.getEntityTuple( (EntityKey) key ) );
		}
		else if ( key instanceof AssociationKey ) {
			memento = o.createMemento( redisDatastoreProvider.getAssociation( (AssociationKey) key ) );
		}
		else if ( key instanceof RowKey ) {
			memento = o.createMemento( redisDatastoreProvider.getSequence( (RowKey) key ) );
		}

		objs[0] = o;
		objs[1] = new CareTaker( memento );
		
		return objs;
	}
}
