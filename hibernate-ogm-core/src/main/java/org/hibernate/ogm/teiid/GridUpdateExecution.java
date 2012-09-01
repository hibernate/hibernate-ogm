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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.teiid.core.TeiidException;
import org.teiid.language.BatchedUpdates;
import org.teiid.language.ColumnReference;
import org.teiid.language.Command;
import org.teiid.language.Condition;
import org.teiid.language.Delete;
import org.teiid.language.Expression;
import org.teiid.language.ExpressionValueSource;
import org.teiid.language.Insert;
import org.teiid.language.Literal;
import org.teiid.language.SetClause;
import org.teiid.language.Update;
import org.teiid.metadata.AbstractMetadataRecord;
import org.teiid.metadata.Column;
import org.teiid.metadata.RuntimeMetadata;
import org.teiid.metadata.Table;
import org.teiid.translator.DataNotAvailableException;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.TranslatorException;
import org.teiid.translator.UpdateExecution;

public class GridUpdateExecution implements UpdateExecution {
	private Command command;
	private int[] results;
	private ExecutionContext executionContext;
	private RuntimeMetadata metadata; 
	private GridDialect dialect;
	
	public GridUpdateExecution(Command command, ExecutionContext executionContext, RuntimeMetadata metadata, GridDialect dialect) {
		this.command = command;
		this.executionContext = executionContext;
		this.metadata = metadata;
		this.dialect = dialect;
	}
	
	@Override
	public void execute() throws TranslatorException {
		if (command instanceof BatchedUpdates) {
			BatchedUpdates updates = (BatchedUpdates)this.command;
			this.results = new int[updates.getUpdateCommands().size()];
			int index = 0;
			for (Command cmd:updates.getUpdateCommands()) {
				this.results[index++] = executeUpdate(cmd);
			}
		}
		else if (this.command instanceof Insert) {
			this.results = new int[1];
			handleInsert((Insert)this.command);
			this.results[0] = 1;
		}
		else if (this.command instanceof Update) {
			// update or delete
			this.results = new int[1];
			this.results[0] = executeUpdate(this.command);			
		}
		else if (this.command instanceof Delete) {
			// update or delete
			this.results = new int[1];
			this.results[0] = executeDelete(this.command);			
		}
		else {
			throw new TranslatorException("unknown command");
		}
	}

	private int executeUpdate(Command cmd) throws TranslatorException {
		if (this.command instanceof Update) {
			Update update = (Update)this.command;
			SelectVisitor visitor = new SelectVisitor();
			visitor.visit(update);

			Table table = update.getTable().getMetadataObject();
			
			String[] colNames = GridUtil.getNames(table.getColumns()).toArray(new String[table.getColumns().size()]);
			AssociationKey tableKey = new AssociationKey(table.getCanonicalName(), colNames, colNames);
			
			Iterator<RowKey> rowIter = null;
			Association tableData = this.dialect.getAssociation(tableKey, null);
			if (tableData != null) {
				SearchCriterion criteria = visitor.getCriteria();
				
				if (criteria != null) {
					rowIter = criteria.filter(tableData);
				}
				else {
					Set<RowKey> rows = tableData.getKeys();
					rowIter = rows.iterator();
				}				
			}
			
			if (rowIter != null) {
				int counter = 0;
				while (rowIter.hasNext()) {
					RowKey key = rowIter.next();
					Tuple tuple = tableData.get(key);
					if (tuple == null) {
						throw new TranslatorException("row not for update");
					}
					List<SetClause> changes = update.getChanges();
					for (SetClause clause:changes) {
						tuple.put(clause.getSymbol().getMetadataObject().getCanonicalName(), ((Literal)clause.getValue()).getValue());
					}
					tableData.put(key, tuple);
					counter++;
				}
				this.dialect.updateAssociation(tableData, tableKey);
				return counter;
			}
		}
		return 0;
	}
	
	private int executeDelete(Command cmd) throws TranslatorException {
		if (this.command instanceof Delete) {
			Delete delete = (Delete)this.command;
			SelectVisitor visitor = new SelectVisitor();
			visitor.visit(delete);

			Table table = delete.getTable().getMetadataObject();
			
			String[] colNames = GridUtil.getNames(table.getColumns()).toArray(new String[table.getColumns().size()]);
			AssociationKey tableKey = new AssociationKey(table.getCanonicalName(), colNames, colNames);
			
			Iterator<RowKey> rowIter = null;
			Association tableData = this.dialect.getAssociation(tableKey, null);
			if (tableData != null) {
				SearchCriterion criteria = visitor.getCriteria();
				
				if (criteria != null) {
					rowIter = criteria.filter(tableData);
				}
				else {
					Set<RowKey> rows = tableData.getKeys();
					rowIter = rows.iterator();
				}				
			}
			
			if (rowIter != null) {
				int counter = 0;
				while (rowIter.hasNext()) {
					RowKey key = rowIter.next();
					tableData.remove(key);
					counter++;
				}
				this.dialect.updateAssociation(tableData, tableKey);
				return counter;
			}
		}
		return 0;
	}
	

	@Override
	public int[] getUpdateCounts() throws DataNotAvailableException, TranslatorException {
		return this.results;
	}

	@Override
	public void close() {
	}

	@Override
	public void cancel() throws TranslatorException {
	}
	
	private void handleInsert(Insert insert) throws TranslatorException {
		try {
			Table table = insert.getTable().getMetadataObject();
			List<ColumnReference> columns = insert.getColumns();
			List<Expression> values = ((ExpressionValueSource)insert.getValueSource()).getValues();
			
			if(columns.size() != values.size()) {
				throw new TranslatorException("Number of columns do not match number of values in the insert");
			}
			
			HashMap<String, Object> literalValues = new HashMap<String, Object>();
			for(int i = 0; i < columns.size(); i++) {
				Column column = columns.get(i).getMetadataObject();
				Object value = values.get(i);
				if(value instanceof Literal) {
					Literal literalValue = (Literal)value;
					literalValues.put(column.getCanonicalName(), literalValue.getValue());
				} else {
					literalValues.put(column.getCanonicalName(), value);
				}
			}
			
			String[] colNames = GridUtil.getNames(table.getColumns()).toArray(new String[table.getColumns().size()]);
			AssociationKey tableKey = new AssociationKey(table.getCanonicalName(), colNames, colNames);
			
			Association tableData = this.dialect.getAssociation(tableKey, null);
			if (tableData == null) {
				tableData = this.dialect.createAssociation(tableKey);
			}
			
			RowKey key = GridUtil.buildRowKey(insert.getTable().getMetadataObject(), literalValues);
			Tuple tuple = this.dialect.createTupleAssociation(tableKey, key);
			for (String col:literalValues.keySet()) {
				tuple.put(col, literalValues.get(col));
			}
			tableData.put(key, tuple);
			
			this.dialect.updateAssociation(tableData, tableKey);
			
		} catch (TeiidException e) {
			throw new TranslatorException(e);
		}
	}
	

}
