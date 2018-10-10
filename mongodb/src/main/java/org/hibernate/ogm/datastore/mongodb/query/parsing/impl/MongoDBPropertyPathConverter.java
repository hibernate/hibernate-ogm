/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.impl;

import org.hibernate.hql.ast.origin.hql.resolve.path.AggregationPropertyPath;
import org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Aleksandr Mylnikov
 */
public class MongoDBPropertyPathConverter {

	private static final Map<AggregationPropertyPath.Type, MongoDBQueryDescriptor.Operation> conversionMap = createConversionMap();

	protected static Map<AggregationPropertyPath.Type, MongoDBQueryDescriptor.Operation> createConversionMap() {
		Map<AggregationPropertyPath.Type, MongoDBQueryDescriptor.Operation> conversion = new HashMap<>();
		conversion.put( AggregationPropertyPath.Type.COUNT, MongoDBQueryDescriptor.Operation.COUNT );
		return conversion;
	}

	public MongoDBQueryDescriptor.Operation convert(AggregationPropertyPath.Type type) {
		return conversionMap.computeIfAbsent( type, key -> { throw new UnsupportedOperationException( "Operation " + type + " is not supported" ); } );
	}

}
