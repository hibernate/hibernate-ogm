/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.neo4j.parser.impl;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.ast.spi.AstProcessingChain;
import org.hibernate.hql.ast.spi.AstProcessor;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.hql.ast.spi.QueryRendererProcessor;
import org.hibernate.hql.ast.spi.QueryResolverProcessor;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class Neo4jProcessingChain implements AstProcessingChain<Neo4jQueryParsingResult> {

	private final QueryResolverProcessor resolverProcessor;
	private final QueryRendererProcessor rendererProcessor;
	private final Neo4jQueryRendererDelegate rendererDelegate;

	public Neo4jProcessingChain(SessionFactoryImplementor sessionFactory, EntityNamesResolver entityNamesResolver, Map<String, Object> namedParameters) {
		Neo4jQueryResolverDelegate resolverDelegate = new Neo4jQueryResolverDelegate();
		Neo4jPropertyHelper propertyHelper = new Neo4jPropertyHelper( sessionFactory, entityNamesResolver );
		this.rendererDelegate = new Neo4jQueryRendererDelegate( sessionFactory, resolverDelegate, entityNamesResolver, propertyHelper, namedParameters );
		this.rendererProcessor = new QueryRendererProcessor( rendererDelegate );
		this.resolverProcessor = new QueryResolverProcessor( resolverDelegate );
	}

	@Override
	public Iterator<AstProcessor> iterator() {
		return Arrays.asList( resolverProcessor, rendererProcessor ).iterator();
	}

	@Override
	public org.hibernate.ogm.datastore.neo4j.parser.impl.Neo4jQueryParsingResult getResult() {
		return rendererDelegate.getResult();
	}

}
