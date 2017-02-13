/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.dialect.impl;

import org.hibernate.ogm.datastore.orientdb.constant.OrientDBConstant;
import org.hibernate.ogm.datastore.orientdb.logging.impl.Log;
import org.hibernate.ogm.datastore.orientdb.logging.impl.LoggerFactory;
import org.hibernate.ogm.model.spi.TupleSnapshot;

import com.orientechnologies.orient.core.record.impl.ODocument;
import java.math.BigDecimal;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Represents a tuple snapshot as loaded by the datastore.
 * <p>
 * This interface is to be implemented by dialects to avoid data duplication in memory (if possible), typically wrapping
 * a store-specific representation of the tuple data. This snapshot will never be modified by the Hibernate OGM engine.
 * <p>
 * Note that in the case of embeddables (e.g. composite ids), column names are given using dot notation, e.g.
 * "id.countryCode" or "address.city.zipCode". The column names of the physical JPA model will be used, as e.g. given
 * via {@code @Column} .
 * <p>
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class OrientDBTupleSnapshot implements TupleSnapshot {

	private static final Log log = LoggerFactory.getLogger();
	private final ODocument document;

	public OrientDBTupleSnapshot(String docClass) {
		this( new ODocument( docClass ) );
	}

	public OrientDBTupleSnapshot(ODocument document) {
		this.document = document;
	}

	@Override
	public Object get(String targetColumnName) {
		log.debugf( "targetColumnName: %s", targetColumnName );
		Object value = null;
		if ( targetColumnName.startsWith( "_identifierMapper." ) ) {
			String fieldName = targetColumnName.substring( "_identifierMapper.".length() );
			value = get( fieldName );
		}
		else if ( targetColumnName.equals( OrientDBConstant.SYSTEM_RID ) ) {
			value = document.getIdentity();
			log.debugf( "targetColumnName: %s, value: %d", targetColumnName, value );
		}
		else {
			value = document.field( targetColumnName );
		}
		return value instanceof BigDecimal ? ( (BigDecimal) value ).toString() : value;
	}

	@Override
	public boolean isEmpty() {
		return document.isEmpty();
	}

	@Override
	public Set<String> getColumnNames() {
		return new LinkedHashSet<>( Arrays.asList( document.fieldNames() ) );
	}

	/**
	 * Whether this snapshot has been newly created (meaning it doesn't have an actual {@link ODocument} yet) or not. A
	 * node will be in the "new" state between the {@code createTuple()} call and the next {@code insertOrUpdateTuple()}
	 * call.
	 *
	 * @return true if the snapshot is new (not saved in database yet), otherwise false
	 */
	public boolean isNew() {
		return ( document.isEmpty() );
	}
}
