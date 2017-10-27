/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.impl;

import java.util.HashMap;
import java.util.Map;

import org.antlr.runtime.tree.Tree;
import org.hibernate.hql.ast.common.JoinType;
import org.hibernate.hql.ast.origin.hql.resolve.path.PathedPropertyReference;
import org.hibernate.hql.ast.origin.hql.resolve.path.PathedPropertyReferenceSource;
import org.hibernate.hql.ast.origin.hql.resolve.path.PropertyPath;
import org.hibernate.hql.ast.spi.QueryResolverDelegate;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

/**
 * Query resolver delegate targeting MongoDB queries. Very basic implementation atm., need to decide on
 * type checks, validation etc.
 *
 * @author Gunnar Morling
 *
 */
public class MongoDBQueryResolverDelegate implements QueryResolverDelegate {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	/**
	 * Persister space: keep track of aliases and entity names.
	 */
	private final Map<String, String> aliasToEntityType = new HashMap<String, String>();
	private final Map<String, PropertyPath> aliasToPropertyPath = new HashMap<String, PropertyPath>();

	private String alias;

	@Override
	public void registerPersisterSpace(Tree entityName, Tree alias) {
		String put = aliasToEntityType.put( alias.getText(), entityName.getText() );
		if ( put != null && !put.equalsIgnoreCase( entityName.getText() ) ) {
			throw new UnsupportedOperationException(
					"Alias reuse currently not supported: alias " + alias.getText()
					+ " already assigned to type " + put );
		}
	}

	@Override
	public void registerJoinAlias(Tree alias, PropertyPath path) {
		PropertyPath put = aliasToPropertyPath.put( alias.getText(), path );
		if ( put != null && !put.equals( path ) ) {
			throw new UnsupportedOperationException( "Alias reuse currently not supported: alias " + alias + " already assigned to type " + put );
		}
	}

	@Override
	public boolean isUnqualifiedPropertyReference() {
		return true;
	}

	@Override
	public PathedPropertyReferenceSource normalizeUnqualifiedPropertyReference(Tree property) {
		return new PathedPropertyReference( property.getText(), null, isAlias( property ) );
	}

	@Override
	public boolean isPersisterReferenceAlias() {
		return isEntityAlias( alias );
	}

	@Override
	public PathedPropertyReferenceSource normalizeUnqualifiedRoot(Tree root) {
		return new PathedPropertyReference( root.getText(), null, isAlias( root ) );
	}

	private boolean isAlias(Tree root) {
		return aliasToEntityType.containsKey( root.getText() ) || aliasToPropertyPath.containsKey( root.getText() );
	}

	private boolean isEntityAlias(String alias) {
		return aliasToEntityType.containsKey( alias );
	}

	@Override
	public PathedPropertyReferenceSource normalizeQualifiedRoot(Tree root) {
		String entityNameForAlias = aliasToEntityType.get( root.getText() );

		if ( entityNameForAlias == null ) {
			throw log.getUnknownAliasException( root.getText() );
		}

		return new PathedPropertyReference( root.getText(), null, true );
	}

	@Override
	public PathedPropertyReferenceSource normalizePropertyPathIntermediary(PropertyPath path, Tree propertyName) {
		return new PathedPropertyReference( propertyName.getText(), null, false );
	}

	@Override
	public PathedPropertyReferenceSource normalizeIntermediateIndexOperation(PathedPropertyReferenceSource propertyReferenceSource, Tree collectionProperty, Tree selector) {
		return propertyReferenceSource;
	}

	@Override
	public void normalizeTerminalIndexOperation(PathedPropertyReferenceSource propertyReferenceSource, Tree collectionProperty, Tree selector) {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public PathedPropertyReferenceSource normalizeUnqualifiedPropertyReferenceSource(Tree identifier394) {
		return null;
	}

	@Override
	public PathedPropertyReferenceSource normalizePropertyPathTerminus(PropertyPath path, Tree propertyNameNode) {
		return new PathedPropertyReference( propertyNameNode.getText(), null, false );
	}

	@Override
	public void pushFromStrategy(JoinType joinType, Tree assosiationFetchTree, Tree propertyFetchTree, Tree alias) {
		this.alias = alias.getText();
	}

	@Override
	public void pushSelectStrategy() {
		//nothing to do
	}

	@Override
	public void popStrategy() {
		//nothing to do
	}

	@Override
	public void propertyPathCompleted(PropertyPath path) {
		//nothing to do
	}
}
