/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.ogm.model.spi.AssociationOrderBy;
import org.hibernate.sql.ordering.antlr.GeneratedOrderByFragmentRenderer;

/**
 * Navigate the {@link antlr.collections.AST} tree, collecting the {@link AssociationOrderBy}s.
 *
 * @author Fabio Massimo Ercoli
 */
public class OGMOrderByRendered extends GeneratedOrderByFragmentRenderer {

	private List<AssociationOrderBy> orderByItems = new ArrayList<>();

	@Override
	protected String renderOrderByElement(String expression, String collation, String order, String nulls) {
		orderByItems.add( new AssociationOrderBy( expression, order ) );
		return expression;
	}

	public List<AssociationOrderBy> getOrderByItems() {
		return orderByItems;
	}
}
