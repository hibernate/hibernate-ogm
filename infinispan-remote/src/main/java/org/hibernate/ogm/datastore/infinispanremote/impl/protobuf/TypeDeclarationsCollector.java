/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protobuf;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;

/**
 */
public class TypeDeclarationsCollector {

	private final Map<String,TypeDefinition> namedTypeDefs = new HashMap<>();

	public void exportProtobufEntries(StringBuilder sb) {
		namedTypeDefs.forEach( ( k, v ) -> v.exportProtobufTypeDefinition( sb ) );
	}

	public void createTypeDefinition(TypeDefinition newDef) {
		TypeDefinition previous = namedTypeDefs.put( newDef.getTypeName(), newDef );
		if ( previous != null && ! previous.equals( newDef ) ) {
			//TODO clarify this message or deal with it
			throw new HibernateException( "Conflicting type definition" );
		}
	}

}
