/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.persister.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Subclass;
import org.hibernate.type.Type;

/**
 * The entity class is used as a discriminator.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class TablePerClassDiscriminator implements EntityDiscriminator {

	private final Integer subclassId;
	private final Map<Object, String> subclassesByValue;

	public TablePerClassDiscriminator(PersistentClass persistentClass) {
		subclassId = persistentClass.getSubclassId();
		subclassesByValue = subclassesByValue( persistentClass, subclassId );
	}

	private static Map<Object, String> subclassesByValue(final PersistentClass persistentClass, Object value) {
		Map<Object, String> subclassesByDiscriminator = new HashMap<Object, String>();
		subclassesByDiscriminator.put( persistentClass.getSubclassId(), persistentClass.getEntityName() );

		if ( persistentClass.isPolymorphic() ) {
			@SuppressWarnings("unchecked")
			Iterator<Subclass> iter = persistentClass.getSubclassIterator();
			while ( iter.hasNext() ) {
				Subclass sc = iter.next();
				subclassesByDiscriminator.put( sc.getSubclassId(), sc.getEntityName() );
			}
		}
		return subclassesByDiscriminator;
	}

	@Override
	public Object getValue() {
		return subclassId;
	}

	@Override
	public boolean isNeeded() {
		return false;
	}

	@Override
	public String provideClassByValue(Object value) {
		return subclassesByValue.get( value );
	}

	@Override
	public String getSqlValue() {
		return null;
	}

	@Override
	public String getColumnName() {
		return null;
	}

	@Override
	public String getAlias() {
		return null;
	}

	@Override
	public Type getType() {
		return null;
	}

	@Override
	public boolean isForced() {
		return false;
	}
}
