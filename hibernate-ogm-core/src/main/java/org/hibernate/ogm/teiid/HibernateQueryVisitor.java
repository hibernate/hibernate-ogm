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

import static org.teiid.language.SQLConstants.Reserved.FROM;
import static org.teiid.language.SQLConstants.Reserved.WHERE;

import org.teiid.language.ColumnReference;
import org.teiid.language.Comparison;
import org.teiid.language.Expression;
import org.teiid.language.In;
import org.teiid.language.SQLConstants.Tokens;
import org.teiid.language.Select;
import org.teiid.language.visitor.SQLStringVisitor;
import org.teiid.metadata.AbstractMetadataRecord;
/**
 * This visitor converts the Teiid command into Hibernate Query 
 */
public class HibernateQueryVisitor extends SQLStringVisitor {
    	
	public void visit(Select obj) {
		
//		buffer.append(SELECT).append(Tokens.SPACE);
//		
//        if (obj.isDistinct()) {
//            buffer.append(DISTINCT).append(Tokens.SPACE);
//        }
//               
//        append(obj.getDerivedColumns());
        
        if (obj.getFrom() != null && !obj.getFrom().isEmpty()) {
        	buffer.append(Tokens.SPACE).append(FROM).append(Tokens.SPACE);
        	append(obj.getFrom());
        }
        
        if (obj.getWhere() != null) {
        	buffer.append(Tokens.SPACE).append("h");
            buffer.append(Tokens.SPACE)
                  .append(WHERE)
                  .append(Tokens.SPACE);
            append(obj.getWhere());
        }
//        if (obj.getGroupBy() != null) {
//            buffer.append(Tokens.SPACE);
//            append(obj.getGroupBy());
//        }
//        if (obj.getHaving() != null) {
//            buffer.append(Tokens.SPACE)
//                  .append(HAVING)
//                  .append(Tokens.SPACE);
//            append(obj.getHaving());
//        }
//        if (obj.getOrderBy() != null) {
//            buffer.append(Tokens.SPACE);
//            append(obj.getOrderBy());
//        }
    } 
	
    public void visit(In obj) {
    	String cond = "OR";
    	String eq = "=";
        if (obj.isNegated()) {
            cond = "AND";
            eq = "!=";
        }
        
		String elemShortName = getElementName((ColumnReference)obj.getLeftExpression());         
        
        boolean first = true;
        for (Expression ex: obj.getRightExpressions()) {
        	if (!first) {
        		buffer.append(Tokens.SPACE).append(cond).append(Tokens.SPACE);
        	}
        	else {
        		first = false;
        	}
        	buffer.append("h.").append(elemShortName).append(eq).append(ex);
        }
    }

	private String getElementName(ColumnReference leftExpr) {
		String elemShortName = null;        
		AbstractMetadataRecord elementID = leftExpr.getMetadataObject();
        if(elementID != null) {
            elemShortName = getName(elementID);            
        } else {
            elemShortName = leftExpr.getName();
        }
		return elemShortName;
	}
    
	public void visit(Comparison obj) {
		Comparison.Operator op = obj.getOperator();
		Expression lhs = obj.getLeftExpression();
		Expression rhs = obj.getRightExpression();
		buffer.append("h.").append(getElementName((ColumnReference)lhs)).append("=").append(rhs);
	}     
}
