/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.persister.impl;

import java.io.Serializable;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.tuple.entity.EntityMetamodel;

/**
 * Dehydrates the properties of a given entity, populating a {@link Tuple} with the converted column values.
 *
 * @author Emmanuel Bernard
 */
class EntityDehydrator {

	private static final Log log = LoggerFactory.make();

	private final OgmEntityPersister persister;
	private Tuple resultset;
	private Object[] fields;
	private boolean[] includeProperties;
	private int tableIndex;
	private Serializable id;
	private SessionImplementor session;

	public EntityDehydrator(OgmEntityPersister persister) {
		this.persister = persister;
	}

	// fluent methods populating data

	public EntityDehydrator resultset(Tuple resultset) {
		this.resultset = resultset;
		return this;
	}

	public EntityDehydrator fields(Object[] fields) {
		this.fields = fields;
		return this;
	}

	public EntityDehydrator includeProperties(boolean[] includeProperties) {
		this.includeProperties = includeProperties;
		return this;
	}

	public EntityDehydrator tableIndex(int tableIndex) {
		this.tableIndex = tableIndex;
		return this;
	}

	public EntityDehydrator id(Serializable id) {
		this.id = id;
		return this;
	}

	public EntityDehydrator session(SessionImplementor session) {
		this.session = session;
		return this;
	}

	//action methods

	public void dehydrate() {
		if ( log.isTraceEnabled() ) {
			log.trace( "Dehydrating entity: " + MessageHelper.infoString( persister, id, persister.getFactory() ) );
		}
		final EntityMetamodel entityMetamodel = persister.getEntityMetamodel();
		for ( int propertyIndex = 0; propertyIndex < entityMetamodel.getPropertySpan(); propertyIndex++ ) {
			if ( persister.isPropertyOfTable( propertyIndex, tableIndex ) ) {
				if ( includeProperties[propertyIndex] ) {
					persister.getGridPropertyTypes()[propertyIndex].nullSafeSet(
							resultset,
							fields[propertyIndex],
							persister.getPropertyColumnNames( propertyIndex ),
							persister.getPropertyColumnInsertable()[propertyIndex],
							session
					);
				}
			}
		}
	}
}
