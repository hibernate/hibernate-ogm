/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */

package org.hibernate.ogm.teiid;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.teiid.core.util.Assertion;
import org.teiid.language.AggregateFunction;
import org.teiid.language.ColumnReference;
import org.teiid.language.Comparison;
import org.teiid.language.Comparison.Operator;
import org.teiid.language.Delete;
import org.teiid.language.DerivedColumn;
import org.teiid.language.Expression;
import org.teiid.language.Function;
import org.teiid.language.In;
import org.teiid.language.Insert;
import org.teiid.language.Like;
import org.teiid.language.Literal;
import org.teiid.language.NamedTable;
import org.teiid.language.ScalarSubquery;
import org.teiid.language.SearchedCase;
import org.teiid.language.Select;
import org.teiid.language.TableReference;
import org.teiid.language.Update;
import org.teiid.language.visitor.HierarchyVisitor;
import org.teiid.logging.LogConstants;
import org.teiid.logging.LogManager;
import org.teiid.metadata.Column;
import org.teiid.metadata.ForeignKey;
import org.teiid.metadata.RuntimeMetadata;
import org.teiid.metadata.Table;
import org.teiid.translator.TranslatorException;


public class SelectVisitor extends HierarchyVisitor {

	private Table table = null;
	protected String[] projectColumnNames = null;
	protected Class[] projectedColumnTypes = null;
	private SearchCriterion criterion;

    private TranslatorException exception;

    
    public Table getTable() {
    	return table;
    }
    
    public String[] getProjectedColumns() {
    	return projectColumnNames;
    }
    
    public Class[] getProjectedTypes() {
    	return projectedColumnTypes;
    }    
    
    public TranslatorException getException() {
        return this.exception;
    }
    
    public SearchCriterion getCriteria() {
    	return this.criterion;
    }
    
	public void visit(Select query) {
		super.visit(query);
		
		Iterator<DerivedColumn> selectSymbolItr = query.getDerivedColumns().iterator();
		projectColumnNames = new String[query.getDerivedColumns().size()];
		projectedColumnTypes = new Class[query.getDerivedColumns().size()];
		
		int i=0;
		while(selectSymbolItr.hasNext()) {
			Column e = ((ColumnReference)selectSymbolItr.next().getExpression()).getMetadataObject();
			projectColumnNames[i] = e.getCanonicalName();
			projectedColumnTypes[i] = e.getJavaType();
			i++;
		}
		
		List<TableReference> tables = query.getFrom();
		TableReference t = tables.get(0);
		if(t instanceof NamedTable) {
			this.table = ((NamedTable)t).getMetadataObject();
		}
	}
	
	public void visit(Update update) {
		super.visit(update);
		TableReference t = update.getTable();
		if(t instanceof NamedTable) {
			this.table = ((NamedTable)t).getMetadataObject();
		}
	}	
	
	public void visit(Delete delete) {
		super.visit(delete);
		TableReference t = delete.getTable();
		if(t instanceof NamedTable) {
			this.table = ((NamedTable)t).getMetadataObject();
		}
	}	
	
    public void visit(In obj) {
		Expression lhs = obj.getLeftExpression();
		Column column = ((ColumnReference)lhs).getMetadataObject();
		List<Expression> rhsList = obj.getRightExpressions();

		Class<?> type = lhs.getType();
		List<Object> parms = new ArrayList<Object>(rhsList.size());
		for (Expression expr : rhsList) {
			if (expr instanceof Literal) {
				Literal literal = (Literal) expr;
				parms.add(literal.getValue());
				type = literal.getType();

			} else {
				Assertion.assertTrue(false, "expresions are not allowed in IN caluse " + expr);
			}

		}
        addInCriteria(column, parms, type);
    }
    
	public void visit(Comparison obj) {

		Comparison.Operator op = obj.getOperator();
		Expression lhs = obj.getLeftExpression();
		Expression rhs = obj.getRightExpression();

		// comparison between the ojbects is not usable, because the
		// nameInSource and its parent(s)
		// will be how the child objects are obtained
		if ((lhs instanceof ColumnReference && rhs instanceof ColumnReference)
				|| (lhs instanceof Literal && rhs instanceof Literal)) {
			return;
		}

		Object value = null;
		Column column = null;
		Literal literal = null;
		if (lhs instanceof ColumnReference) {
			column = ((ColumnReference) lhs).getMetadataObject();
			literal = (Literal) rhs;
			value = literal.getValue();
		} 
		else {
			column = ((ColumnReference) rhs).getMetadataObject();
			literal = (Literal) lhs;
			value = literal.getValue();
		}

		if (column == null || value == null) {
			Assertion.assertTrue(false,"Missing Comparison Expression in the Equals caluse");
		}

		if (value instanceof String) {
			value = escapeReservedChars((String)value);
		}

		addCompareCriteria(column, value, op, literal.getType());
	}   
    
    private void addCompareCriteria(Column column, Object value, Operator op, Class<?> type) {
		SearchCriterion sc = new SearchCriterion(column, value, op.toString(), SearchCriterion.Operator.EQUALS, type);
		addSearchCriterion(sc);	
    }	
	
	private void addInCriteria(Column column, List<Object> parms, Class<?> type) {
		SearchCriterion sc = new SearchCriterion(column, parms, "in", SearchCriterion.Operator.IN, type);
		addSearchCriterion( sc);
	}    
	
	private void addSearchCriterion(SearchCriterion searchCriteria) {
		if (this.criterion != null) {
			searchCriteria.addOrCriterion(this.criterion);
		} else {
			this.criterion = searchCriteria;
		}
	}	
	
    private Column getElementFromSymbol(DerivedColumn symbol) {
        ColumnReference expr = (ColumnReference) symbol.getExpression();
        return expr.getMetadataObject();
    }
    
	protected static String escapeReservedChars(final String expr) {
		StringBuffer sb = new StringBuffer();
        for (int i = 0; i < expr.length(); i++) {
            char curChar = expr.charAt(i);
            switch (curChar) {
                case '\\':
                    sb.append("\\5c"); //$NON-NLS-1$
                    break;
                case '*':
                    sb.append("\\2a"); //$NON-NLS-1$
                    break;
                case '(':
                    sb.append("\\28"); //$NON-NLS-1$
                    break;
                case ')':
                    sb.append("\\29"); //$NON-NLS-1$
                    break;
                case '\u0000': 
                    sb.append("\\00"); //$NON-NLS-1$
                    break;
                default:
                    sb.append(curChar);
            }
        }
        return sb.toString();
	}    
    
    
}
