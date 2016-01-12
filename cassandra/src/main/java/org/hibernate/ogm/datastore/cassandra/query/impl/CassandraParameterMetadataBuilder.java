/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.query.impl;

import java.util.Map;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.query.spi.OrdinalParameterDescriptor;
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.ogm.dialect.query.spi.ParameterMetadataBuilder;
import org.hibernate.type.Type;

import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;

/**
 * {@link ParameterMetadataBuilder} for native Cassandra CQL queries.
 * This implementation delegates parsing to the server cluster, so no string positional information.
 *
 * @author Jonathan Halliday
 */
public class CassandraParameterMetadataBuilder implements ParameterMetadataBuilder {

	private final Session session;
	private final Map<String, Table> metaDataCache;

	public CassandraParameterMetadataBuilder(Session session, Map<String, Table> metaDataCache) {
		this.session = session;
		this.metaDataCache = metaDataCache;
	}

	@Override
	public ParameterMetadata buildParameterMetadata(String nativeQuery) {
		PreparedStatement preparedStatement = session.prepare( nativeQuery );
		ColumnDefinitions columnDefinitions = preparedStatement.getVariables();
		OrdinalParameterDescriptor[] ordinalDescriptors = new OrdinalParameterDescriptor[columnDefinitions.size()];

		if ( columnDefinitions.size() > 0 ) {

			// the cassandra metadata will give us the CQL type, but the type conversion system only goes
			// in hibernate->cassandra direction, so we can't turn it back into the required hibernate type.
			// instead we rely on the cached hibernate metdata from schema creation time

			String tableName = columnDefinitions.getTable( 0 );
			Table table = metaDataCache.get( tableName );

			for ( ColumnDefinitions.Definition definition : columnDefinitions ) {
				String name = definition.getName();
				Column column = table.getColumn( Identifier.toIdentifier( name ) );
				Type hibernateType = column.getValue().getType();
				// cassandra side index is 0-based, hibernate side index is 1-based
				int index = columnDefinitions.getIndexOf( name );
				ordinalDescriptors[index] = new OrdinalParameterDescriptor( index + 1, hibernateType, 0 );
			}
		}

		return new ParameterMetadata( ordinalDescriptors, null );
	}
}
