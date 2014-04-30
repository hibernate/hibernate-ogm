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

import static org.neo4j.cypherdsl.CypherQuery.identifier;

import org.neo4j.cypherdsl.query.AbstractExpression;

/**
 * Represents matching a label to a value
 *
 * @deprecated Update {@code org.neo4j:neo4j-cypher-dsl} to version 2.0.2 and use the methods in {@link CypherQuery}
 * instead
 */
@Deprecated
public class LabelValue extends AbstractExpression {

	private final String[] labels;
	private final String identifier;

	public LabelValue(String identifier, String... labels) {
		this.identifier = identifier;
		this.labels = labels;
	}

	@Override
	public void asString(StringBuilder builder) {
		identifier( identifier ).asString( builder );
		for ( String label : labels ) {
			builder.append( ":" );
			builder.append( label );
		}
	}

}
