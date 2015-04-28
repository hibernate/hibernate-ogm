/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.DriverException;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.ogm.datastore.cassandra.impl.CassandraDatastoreProvider;
import org.hibernate.ogm.datastore.cassandra.impl.CassandraTypeMapper;
import org.hibernate.ogm.datastore.cassandra.logging.impl.Log;
import org.hibernate.ogm.datastore.cassandra.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.cassandra.model.impl.ResultSetTupleSnapshot;

import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;

import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.DuplicateInsertPreventionStrategy;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.TupleAlreadyExistsException;
import org.hibernate.ogm.dialect.spi.TupleContext;

import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.TupleOperation;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.Type;

import static com.datastax.driver.core.querybuilder.QueryBuilder.delete;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.datastax.driver.core.querybuilder.QueryBuilder.quote;

/**
 * Dialect implementation using CQL3 over Cassandra's native transport via java-driver.
 *
 * @author Jonathan Halliday
 */
public class CassandraDialect implements GridDialect {

	private static final Log log = LoggerFactory.getLogger();

	private final CassandraDatastoreProvider provider;
	private final Session session;
	private final ProtocolVersion protocolVersion;

	public CassandraDialect(CassandraDatastoreProvider provider) {
		this.provider = provider;
		session = provider.getSession();
		protocolVersion = session
				.getCluster()
				.getConfiguration()
				.getProtocolOptions()
				.getProtocolVersionEnum();
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		//Cassandra essentially has no workable lock strategy unless you use external tools like
		// ZooKeeper or fancy cql overlays like waitchain.
		return null;
	}

	private ResultSet bindAndExecute(Object[] columnValues, RegularStatement statement) {

		try {
			PreparedStatement preparedStatement = session.prepare( statement );
			BoundStatement boundStatement = new BoundStatement( preparedStatement );
			for ( int i = 0; i < columnValues.length; i++ ) {
				DataType dataType = preparedStatement.getVariables().getType( i );
				boundStatement.setBytesUnsafe( i, dataType.serialize( columnValues[i], protocolVersion ) );
			}
			return session.execute( boundStatement );
		}
		catch (DriverException e) {
			log.failToExecuteCQL( statement.getQueryString(), e );
			throw e;
		}
	}

	@Override
	public Tuple getTuple(EntityKey key, TupleContext tupleContext) {

		Select select = select().all().from( quote( key.getTable() ) );
		Select.Where selectWhere = select.where( eq( quote( key.getColumnNames()[0] ), QueryBuilder.bindMarker() ) );
		for ( int i = 1; i < key.getColumnNames().length; i++ ) {
			selectWhere = selectWhere.and( eq( quote( key.getColumnNames()[i] ), QueryBuilder.bindMarker() ) );
		}

		Object[] columnValues = key.getColumnValues();
		ResultSet resultSet = bindAndExecute( columnValues, select );

		if ( resultSet.isExhausted() ) {
			return null;
		}

		Row row = resultSet.one();
		Tuple tuple = new Tuple( new ResultSetTupleSnapshot( row, protocolVersion ) );
		return tuple;
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		Map<String, Object> toSave = new HashMap<String, Object>();
		toSave.put( key.getColumnNames()[0], key.getColumnValues()[0] );
		return new Tuple( new MapTupleSnapshot( toSave ) );
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, Tuple tuple, TupleContext tupleContext)
			throws TupleAlreadyExistsException {

		List<TupleOperation> updateOps = new ArrayList<TupleOperation>( tuple.getOperations().size() );
		List<TupleOperation> deleteOps = new ArrayList<TupleOperation>( tuple.getOperations().size() );

		for ( TupleOperation op : tuple.getOperations() ) {
			switch ( op.getType() ) {
				case PUT:
					updateOps.add( op );
					break;
				case REMOVE:
				case PUT_NULL:
					deleteOps.add( op );
					break;
				default:
					throw new HibernateException( "TupleOperation not supported: " + op.getType() );
			}
		}

		if ( deleteOps.size() > 0 ) {

			Delete.Selection deleteSelection = delete();
			for ( TupleOperation tupleOperation : deleteOps ) {
				deleteSelection.column( quote( tupleOperation.getColumn() ) );
			}
			Delete delete = deleteSelection.from( quote( key.getTable() ) );
			Delete.Where deleteWhere = delete.where(
					eq(
							quote( key.getColumnNames()[0] ),
							QueryBuilder.bindMarker()
					)
			);
			for ( int i = 1; i < key.getColumnNames().length; i++ ) {
				deleteWhere = deleteWhere.and( eq( quote( key.getColumnNames()[i] ), QueryBuilder.bindMarker() ) );
			}

			bindAndExecute( key.getColumnValues(), delete );
		}

		if ( updateOps.size() > 0 ) {

			// insert and update are both 'upsert' in cassandra.
			Insert insert = insertInto( quote( key.getTable() ) );
			Object[] columnValues = new Object[updateOps.size()];
			for ( int i = 0; i < updateOps.size(); i++ ) {
				TupleOperation op = updateOps.get( i );
				insert.value( quote( op.getColumn() ), QueryBuilder.bindMarker() );
				columnValues[i] = op.getValue();
			}

			bindAndExecute( columnValues, insert );
		}
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {

		Delete delete = delete().from( quote( key.getTable() ) );
		Delete.Where deleteWhere = delete.where(
				eq( quote( key.getColumnNames()[0] ), QueryBuilder.bindMarker() )
		);
		for ( int i = 1; i < key.getColumnNames().length; i++ ) {
			deleteWhere = deleteWhere.and( eq( quote( key.getColumnNames()[i] ), QueryBuilder.bindMarker() ) );
		}

		bindAndExecute( key.getColumnValues(), delete );
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		return null; // TODO
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		return null; // TODO
	}

	@Override
	public void insertOrUpdateAssociation(
			AssociationKey key,
			Association association,
			AssociationContext associationContext) {

		// TODO
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		// TODO
	}

	@Override
	public GridType overrideType(Type type) {
		return CassandraTypeMapper.INSTANCE.overrideType( type );
	}

	@Override
	public void forEachTuple(ModelConsumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		for ( EntityKeyMetadata entityKeyMetadata : entityKeyMetadatas ) {

			Select select = select().all().from( quote( entityKeyMetadata.getTable() ) );

			ResultSet resultSet;
			try {
				resultSet = session.execute( select );
			}
			catch (DriverException e) {
				throw e;
			}
			Iterator<Row> iter = resultSet.iterator();
			while ( iter.hasNext() ) {
				Row row = iter.next();
				Tuple tuple = new Tuple( new ResultSetTupleSnapshot( row, protocolVersion ) );
				consumer.consume( tuple );
			}
		}
	}

	@Override
	public boolean isStoredInEntityStructure(
			AssociationKeyMetadata associationKeyMetadata,
			AssociationTypeContext associationTypeContext) {
		return false;
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		return provider.getSequenceHandler().nextValue( request );
	}

	@Override
	public boolean supportsSequences() {
		return false;
	}

	@Override
	public DuplicateInsertPreventionStrategy getDuplicateInsertPreventionStrategy(EntityKeyMetadata entityKeyMetadata) {
		return null;
	}
}
