/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.embedded.dialect.impl;

import static java.util.Collections.singletonMap;

import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.internal.util.collections.BoundedConcurrentHashMap;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.BaseNeo4jSequenceGenerator;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.id.impl.OgmSequenceGenerator;
import org.hibernate.ogm.id.impl.OgmTableGenerator;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata.IdSourceType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Lock;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.ConstraintType;

/**
 * Generates the next value of an id sequence as represented by {@link IdSourceKey}.
 * <p>
 * Both, {@link IdSourceType#TABLE} and {@link IdSourceType#SEQUENCE} are supported. For the table strategy, nodes in
 * the following form are used (the exact property names and the label value can be configured using the options exposed
 * by {@link OgmTableGenerator}):
 *
 * <pre>
 * (:hibernate_sequences:TABLE_BASED_SEQUENCE { sequence_name = 'ExampleSequence', current_value : 3 })
 * </pre>
 *
 * For the sequence strategy, nodes in the following form are used (the sequence name can be configured using the option
 * exposed by {@link OgmSequenceGenerator}):
 *
 * <pre>
 * (:SEQUENCE { sequence_name = 'ExampleSequence', next_val : 3 })
 * </pre>
 *
 * Sequences are created at startup.
 * <p>
 * A write lock is acquired on the node every time the sequence needs to be updated.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class EmbeddedNeo4jSequenceGenerator extends BaseNeo4jSequenceGenerator {

	private static final Log logger = LoggerFactory.make( MethodHandles.lookup() );
	/**
	 * Query for creating SEQUENCE nodes.
	 */
	private static final String SEQUENCE_CREATION_QUERY = "MERGE (n:" + NodeLabel.SEQUENCE.name() + " {" + SEQUENCE_NAME_PROPERTY + ": {sequenceName}} ) ON CREATE SET n." + SEQUENCE_VALUE_PROPERTY + " = {initialValue} RETURN n";

	/**
	 * Query for retrieving the next value from SEQUENCE nodes.
	 */
	private static final String SEQUENCE_VALUE_QUERY = "MATCH (n:" + NodeLabel.SEQUENCE.name() + ") WHERE n." + SEQUENCE_NAME_PROPERTY + " = {sequenceName} RETURN n";

	private final BoundedConcurrentHashMap<String, String> queryCache;

	private final GraphDatabaseService neo4jDb;

	public EmbeddedNeo4jSequenceGenerator(GraphDatabaseService neo4jDb, int sequenceCacheMaxSize) {
		this.neo4jDb = neo4jDb;
		this.queryCache = new BoundedConcurrentHashMap<String, String>( sequenceCacheMaxSize, 20, BoundedConcurrentHashMap.Eviction.LIRS );
	}

	/**
	 * Create the sequence nodes setting the initial value if the node does not exists already.
	 * <p>
	 * All nodes are created inside the same transaction
	 *
	 * @param sequences the generators representing the sequences
	 */
	public void createSequences(Iterable<Sequence> sequences) {
		addUniqueConstraintForSequences();
		addSequences( sequences );
	}

	public void createUniqueConstraintsForTableSequences(Iterable<IdSourceKeyMetadata> tableIdGenerators) {
		Transaction tx = null;
		try {
			tx = neo4jDb.beginTx();

			for ( IdSourceKeyMetadata idSourceKeyMetadata : tableIdGenerators ) {
				if ( idSourceKeyMetadata.getType() == IdSourceType.TABLE ) {
					addUniqueConstraintForTableBasedSequence( idSourceKeyMetadata );
				}
			}

			tx.success();
		}
		finally {
			tx.close();
		}
	}

	private void addUniqueConstraintForSequences() {
		Transaction tx = null;
		try {
			tx = neo4jDb.beginTx();

			if ( isMissingUniqueConstraint( NodeLabel.SEQUENCE ) ) {
				neo4jDb.schema()
					.constraintFor( NodeLabel.SEQUENCE )
					.assertPropertyIsUnique( SEQUENCE_NAME_PROPERTY )
					.create();
			}

			tx.success();
		}
		finally {
			tx.close();
		}
	}

	/**
	 * Adds a unique constraint to make sure that each node of the same "sequence table" is unique.
	 */
	private void addUniqueConstraintForTableBasedSequence(IdSourceKeyMetadata generatorKeyMetadata) {
		Label generatorKeyLabel = Label.label( generatorKeyMetadata.getName() );
		if ( isMissingUniqueConstraint( generatorKeyLabel ) ) {
			neo4jDb.schema().constraintFor( generatorKeyLabel ).assertPropertyIsUnique( generatorKeyMetadata.getKeyColumnName() ).create();
		}
	}

	private boolean isMissingUniqueConstraint(Label generatorKeyLabel) {
		Iterable<ConstraintDefinition> constraints = neo4jDb.schema().getConstraints( generatorKeyLabel );
		for ( ConstraintDefinition constraint : constraints ) {
			if ( constraint.isConstraintType( ConstraintType.UNIQUENESS ) ) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Adds a node for each generator of type {@link IdSourceType#SEQUENCE}. Table-based generators are created lazily
	 * at runtime.
	 *
	 * @param identifierGenerators the generators to process
	 */
	private void addSequences(Iterable<Sequence> sequences) {
		Transaction tx = null;
		try {
			tx = neo4jDb.beginTx();
			for ( Sequence sequence : sequences ) {
				addSequence( sequence );
			}
			tx.success();
		}
		finally {
			tx.close();
		}
	}

	/**
	 * Ex.:
	 * <pre>
	 * MERGE (n:hibernate_sequences:TABLE_BASED_SEQUENCE {sequence_name: {sequenceName}}) ON CREATE SET n.current_value = {initialValue} RETURN n
	 * </pre>
	 */
	private void addTableSequence(NextValueRequest request) {
		IdSourceKeyMetadata idSourceKeyMetadata = request.getKey().getMetadata();
		String query = "MERGE (n" + labels( idSourceKeyMetadata.getName(), NodeLabel.TABLE_BASED_SEQUENCE.name() ) + " { " + idSourceKeyMetadata.getKeyColumnName() + ": {"
				+ SEQUENCE_NAME_QUERY_PARAM + "}} ) ON CREATE SET n." + idSourceKeyMetadata.getValueColumnName() + " = {" + INITIAL_VALUE_QUERY_PARAM + "} RETURN n";
		neo4jDb.execute( query, params( request ) );
	}

	/**
	 * Ex.:
	 * <pre>
	 * MERGE (n:SEQUENCE {sequence_name: {sequenceName}}) ON CREATE SET n.current_value = {initialValue} RETURN n
	 * </pre>
	 */
	private void addSequence(Sequence sequence) {
		neo4jDb.execute( SEQUENCE_CREATION_QUERY, params( sequence ) );
	}

	/**
	 * Generate the next value in a sequence for a given {@link IdSourceKey}.
	 *
	 * @param request the details about how to obtain the next value
	 * @return the next value in a sequence
	 */
	@Override
	public Long nextValue(NextValueRequest request) {
		Transaction tx = neo4jDb.beginTx();
		Lock lock = null;
		try {
			Node sequence = getSequence( request.getKey() );

			if ( sequence == null ) {
				// sequence nodes are expected to have been created up-front
				if ( request.getKey().getMetadata().getType() == IdSourceType.SEQUENCE ) {
					throw logger.sequenceNotFound( sequenceName( request.getKey() ) );
				}
				// table sequence nodes (think of them as rows in a generator table) are created upon first usage
				else {
					addTableSequence( request );
					sequence = getSequence( request.getKey() );
				}
			}

			lock = tx.acquireWriteLock( sequence );
			long nextValue = updateSequenceValue( request.getKey(), sequence, request.getIncrement() );
			tx.success();
			lock.release();
			return nextValue;
		}
		finally {
			tx.close();
		}
	}

	/**
	 * Given a {@link IdSourceKey}, get the corresponding sequence node.
	 *
	 * @param idSourceKey the {@link IdSourceKey} identifying the sequence
	 * @return the node representing the sequence
	 */
	private Node getSequence(IdSourceKey idSourceKey) {
		String updateSequenceQuery = getQuery( idSourceKey );
		Result result = neo4jDb.execute( updateSequenceQuery, singletonMap( SEQUENCE_NAME_QUERY_PARAM, (Object) sequenceName( idSourceKey ) ) );
		ResourceIterator<Node> column = result.columnAs( "n" );
		Node node = null;
		if ( column.hasNext() ) {
			node = column.next();
		}
		column.close();
		return node;
	}

	private String getQuery(IdSourceKey idSourceKey) {
		return idSourceKey.getMetadata().getType() == IdSourceType.TABLE ? getTableQuery( idSourceKey ) : SEQUENCE_VALUE_QUERY;
	}

	/**
	 * Ex.:
	 * <pre>
	 * MATCH (n:hibernate_sequences:TABLE_BASED_SEQUENCE) WHERE n.sequence_name = {sequenceName} RETURN n
	 * </pre>
	 */
	private String getTableQuery(IdSourceKey idSourceKey) {
		String query = queryCache.get( idSourceKey.getTable() );
		if ( query == null ) {
			query = "MATCH (n" + labels( idSourceKey.getTable(), NodeLabel.TABLE_BASED_SEQUENCE.name() ) + ") WHERE n." + idSourceKey.getMetadata().getKeyColumnName() + " = {"
						+ SEQUENCE_NAME_QUERY_PARAM + "} RETURN n";
			String cached = queryCache.putIfAbsent( idSourceKey.getTable(), query );
			if ( cached != null ) {
				query = cached;
			}
		}
		return query;
	}

	private Long updateSequenceValue(IdSourceKey idSourceKey, Node sequence, int increment) {
		String valueProperty = idSourceKey.getMetadata().getType() == IdSourceType.TABLE ? idSourceKey.getMetadata().getValueColumnName() : SEQUENCE_VALUE_PROPERTY;
		Number currentValue = (Number) sequence.getProperty( valueProperty );
		long updatedValue = currentValue.longValue() + increment;
		sequence.setProperty( valueProperty, updatedValue );
		return currentValue.longValue();
	}
}
