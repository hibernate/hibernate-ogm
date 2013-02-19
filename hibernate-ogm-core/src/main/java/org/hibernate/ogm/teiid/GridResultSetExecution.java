/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.teiid;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.hibernate.Query;
import org.hibernate.Transaction;
import org.hibernate.ogm.hibernatecore.impl.OgmSession;
import org.teiid.language.QueryExpression;
import org.teiid.language.Select;
import org.teiid.metadata.RuntimeMetadata;
import org.teiid.query.eval.TeiidScriptEngine;
import org.teiid.translator.DataNotAvailableException;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.ResultSetExecution;
import org.teiid.translator.TranslatorException;

public class GridResultSetExecution implements ResultSetExecution {
	private static final String OBJECT_NAME = "o"; //$NON-NLS-1$
	private ScriptContext sc = new SimpleScriptContext();
	private static TeiidScriptEngine scriptEngine = new TeiidScriptEngine();
	
	private ExecutionContext executionContext;
	private RuntimeMetadata metadata; 
	private OgmSession session;
	private Select command;
	private Iterator rowIter = null;
	
	private ArrayList<CompiledScript> projects = new ArrayList<CompiledScript>();

	public GridResultSetExecution(QueryExpression command, ExecutionContext executionContext, RuntimeMetadata metadata, OgmSession connection) throws TranslatorException {
		this.command = (Select)command;
		this.executionContext = executionContext;
		this.metadata = metadata;
		this.session = connection;
		
		SelectVisitor visitor = new SelectVisitor();
		visitor.visit((Select)command);
		for (String col:visitor.projectColumnNames) {
			try {
				projects.add(scriptEngine.compile(OBJECT_NAME + "." + col.toLowerCase())); //$NON-NLS-1$
			} catch (ScriptException e) {
				throw new TranslatorException(e);
			}			
		}
	}

	@Override
	public void execute() throws TranslatorException {
		System.out.println("Teiid Query = "+command);
		HibernateQueryVisitor visitor = new HibernateQueryVisitor();
		visitor.visitNode(command);
		String hql = visitor.toString();
		System.out.println("hql = "+hql);
		Query query = this.session.createQuery(hql);
		Transaction transaction = session.beginTransaction();
		try {
			List results = query.list();
			//System.out.println("no of rows="+results.size());
			this.rowIter = results.iterator();
		} finally {
			transaction.commit();
		}
	}

	@Override
	public List<?> next() throws TranslatorException, DataNotAvailableException {
		if (this.rowIter != null && this.rowIter.hasNext()) {
			Object entity = this.rowIter.next();
			
			List<Object> row = new ArrayList<Object>(projects.size());
			sc.setAttribute(OBJECT_NAME, entity, ScriptContext.ENGINE_SCOPE);
			for (CompiledScript cs : this.projects) {
				try {
					row.add(cs.eval(sc));
				} catch (ScriptException e) {
					throw new TranslatorException(e);
				}
			}
			//System.out.println("row="+row);
			return row;
		}
		return null;
	}
	
	@Override
	public void close() {
		session.clear();
	}

	@Override
	public void cancel() throws TranslatorException {
		session.cancelQuery();
	}	
}
