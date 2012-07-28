/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.service.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.ogm.persister.OgmEntityPersister;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.event.impl.FullTextIndexEventListener;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.sql.ast.origin.hql.parse.HQLLexer;
import org.hibernate.sql.ast.origin.hql.parse.HQLParser;
import org.hibernate.sql.ast.origin.hql.resolve.LuceneJPQLWalker;


/**
 * QueryParserService using the ANTLR3-powered LuceneJPQLWalker.
 * Expects the targeted entities and used attributes to be indexed via Hibernate Search,
 * transforming HQL and JPQL in Lucene Queries.
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class LuceneBasedQueryParserService implements QueryParserService {

	private final ServiceRegistryImplementor registry;
	private transient SearchFactoryImplementor searchFactory;

	public LuceneBasedQueryParserService(ServiceRegistryImplementor registry, Map configurationValues) {
		this.registry = registry;
		//TODO: make it possible to lookup the SearchFactoryImplementor at initialization time
		//searchFactoryImplementor = lookupSearchFactory( registry );
	}

	@Override
	public Query getParsedQueryExecutor(Session session, String queryString, Map<String, Object> namedParameters) {
		HQLLexer lexed = new HQLLexer( new ANTLRStringStream( queryString ) );
		TokenStream tokens = new CommonTokenStream( lexed );
		HQLParser parser = new HQLParser( tokens );
		try {
			//TODO move the following logic into the hibernate-jpql-parser project?
			//needs to consider usage of a parsed query plans cache

			// parser#statement() is the entry point for evaluation of any kind of statement
			HQLParser.statement_return r = parser.statement();
			CommonTree tree = (CommonTree) r.getTree();
			// To walk the resulting tree we need a treenode stream:
			CommonTreeNodeStream treeStream = new CommonTreeNodeStream( tree );
			// AST nodes have payloads referring to the tokens from the Lexer:
			treeStream.setTokenStream( tokens );
			HashMap<String, Class> entityNames = getDefinedEntityNames( session.getSessionFactory() );
			SearchFactoryImplementor searchFactory = getSearchFactory( session );
			// Finally create the treewalker:
			LuceneJPQLWalker walker = new LuceneJPQLWalker( treeStream, searchFactory, entityNames, namedParameters );
			walker.statement();
			org.apache.lucene.search.Query luceneQuery = walker.getLuceneQuery();
			Class targetEntity = walker.getTargetEntity();
			//TODO avoid wrapping in a Search FullTextSession? (repeated lookup of SearchFactory)
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( luceneQuery, targetEntity );
			//Following options are mandatory to load matching entities without using a query
			//(chicken and egg problem)
			fullTextQuery.initializeObjectsWith( ObjectLookupMethod.SKIP, DatabaseRetrievalMethod.FIND_BY_ID );
			return fullTextQuery;
		}
		catch (RecognitionException e) {
			throw new HibernateException( "Invalid query syntax", e );
		}
	}

	private SearchFactoryImplementor getSearchFactory(Session session) {
		//FIXME should use lookupSearchFactory(ServiceRegistryImplementor) instead
		if ( searchFactory == null ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			searchFactory = (SearchFactoryImplementor) fullTextSession.getSearchFactory();
		}
		return searchFactory;
	}

	/**
	 * @return a map function from entity names to Class types representing the entity
	 */
	private HashMap<String, Class> getDefinedEntityNames(SessionFactory factory) {
		Map<String, ClassMetadata> allClassMetadata = factory.getAllClassMetadata();
		HashMap<String,Class> hashMap = new HashMap<String, Class>();
		for ( Entry<String, ClassMetadata> entry : allClassMetadata.entrySet() ) {
			OgmEntityPersister classMetadata = (OgmEntityPersister) entry.getValue();
			Class mappedClass = classMetadata.getMappedClass();
			String entityName = classMetadata.getJpaEntityName();
			if ( mappedClass != null ) {
				//add the short-hand entityName
				hashMap.put( entityName, mappedClass );
				//and the full class name as it might be used too
				hashMap.put( mappedClass.getName(), mappedClass );
			}
		}
		//finally make sure to define java.lang.Object as a special case query
		hashMap.put( Object.class.getName(), Object.class );
		hashMap.put( Object.class.getSimpleName(), Object.class );
		return hashMap;
	}

	private static SearchFactoryImplementor lookupSearchFactory(final ServiceRegistryImplementor registry) {
		//FIXME following code taken from Hibernate Search's 
		// org.hibernate.search.util.impl.ContextHelper
		//TODO Have Hibernate Search register the SearchFactoryImplementor in the registry?
		final EventListenerRegistry service = registry
				.getService( EventListenerRegistry.class );
		final Iterable<PostInsertEventListener> listeners = service
				.getEventListenerGroup( EventType.POST_INSERT )
				.listeners();
		FullTextIndexEventListener listener = null;
		for ( PostInsertEventListener candidate : listeners ) {
			if ( candidate instanceof FullTextIndexEventListener ) {
				listener = (FullTextIndexEventListener) candidate;
				break;
			}
		}
		if ( listener == null ) {
			throw new HibernateException( "Hibernate Search Event listeners not found" );
		}
		return listener.getSearchFactoryImplementor();
	}

}
