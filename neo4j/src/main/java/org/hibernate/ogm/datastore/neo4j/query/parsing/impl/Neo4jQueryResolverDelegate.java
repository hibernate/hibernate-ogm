/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl;

import java.util.HashMap;
import java.util.Map;

import org.antlr.runtime.tree.Tree;
import org.hibernate.hql.ast.common.JoinType;
import org.hibernate.hql.ast.origin.hql.resolve.path.PathedPropertyReference;
import org.hibernate.hql.ast.origin.hql.resolve.path.PathedPropertyReferenceSource;
import org.hibernate.hql.ast.origin.hql.resolve.path.PropertyPath;
import org.hibernate.hql.ast.spi.QueryResolverDelegate;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jQueryResolverDelegate implements QueryResolverDelegate {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	/**
	 * Persister space: keep track of aliases and entity names.
	 */
	private final Map<String, String> entityNameByAlias = new HashMap<String, String>();
	private final Map<String, PropertyPath> propertyPathByAlias = new HashMap<String, PropertyPath>();

	private final Neo4jAliasResolver aliasResolver;

	private String currentAlias;

	public Neo4jQueryResolverDelegate(Neo4jAliasResolver aliasResolver) {
		this.aliasResolver = aliasResolver;
	}

	@Override
	public void registerPersisterSpace(Tree entityName, Tree alias) {
		String put = entityNameByAlias.put( alias.getText(), entityName.getText() );
		if ( put != null && !put.equalsIgnoreCase( entityName.getText() ) ) {
			throw new UnsupportedOperationException(
					"Alias reuse currently not supported: alias " + alias.getText()
					+ " already assigned to type " + put );
		}
		aliasResolver.registerEntityAlias( entityName.getText(), alias.getText() );
	}

	@Override
	public void registerJoinAlias(Tree alias, PropertyPath path) {
		PropertyPath put = propertyPathByAlias.put( alias.getText(), path );
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
		return isEntityAlias( currentAlias );
	}

	@Override
	public PathedPropertyReferenceSource normalizeUnqualifiedRoot(Tree root) {
		return new PathedPropertyReference( root.getText(), null, isAlias( root ) );
	}

	private boolean isAlias(Tree root) {
		return isEntityAlias( root.getText() ) || propertyPathByAlias.containsKey( root.getText() );
	}

	private boolean isEntityAlias(String alias) {
		return entityNameByAlias.containsKey( alias );
	}

	@Override
	public PathedPropertyReferenceSource normalizeQualifiedRoot(Tree root) {
		String entityNameForAlias = entityNameByAlias.get( root.getText() );

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
	public PathedPropertyReferenceSource normalizeIntermediateIndexOperation(PathedPropertyReferenceSource propertyReferenceSource, Tree collectionProperty,
			Tree selector) {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public void normalizeTerminalIndexOperation(PathedPropertyReferenceSource propertyReferenceSource, Tree collectionProperty, Tree selector) {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public PathedPropertyReferenceSource normalizeUnqualifiedPropertyReferenceSource(Tree identifier394) {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public PathedPropertyReferenceSource normalizePropertyPathTerminus(PropertyPath path, Tree propertyNameNode) {
		return new PathedPropertyReference( propertyNameNode.getText(), null, false );
	}

	@Override
	public void pushFromStrategy(JoinType joinType, Tree assosiationFetchTree, Tree propertyFetchTree, Tree alias) {
		this.currentAlias = alias.getText();
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
