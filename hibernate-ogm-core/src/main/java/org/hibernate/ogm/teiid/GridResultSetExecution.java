package org.hibernate.ogm.teiid;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.RowKey;
import org.teiid.language.QueryExpression;
import org.teiid.language.Select;
import org.teiid.language.TableReference;
import org.teiid.metadata.AbstractMetadataRecord;
import org.teiid.metadata.RuntimeMetadata;
import org.teiid.metadata.Table;
import org.teiid.translator.DataNotAvailableException;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.ResultSetExecution;
import org.teiid.translator.TranslatorException;

public class GridResultSetExecution implements ResultSetExecution {
	private ExecutionContext executionContext;
	private RuntimeMetadata metadata; 
	private GridDialect dialect;
	private Select command;
	private Association tableData;
	private Iterator<RowKey> rowIter;
	private String[] projectedColumns; 

	public GridResultSetExecution(QueryExpression command, ExecutionContext executionContext, RuntimeMetadata metadata, GridDialect connection) {
		this.command = (Select)command;
		this.executionContext = executionContext;
		this.metadata = metadata;
		this.dialect = connection;
	}

	@Override
	public void execute() throws TranslatorException {
		SelectVisitor visitor = new SelectVisitor();
		visitor.visit(this.command);
		
		Table table = visitor.getTable();
		this.projectedColumns = visitor.getProjectedColumns();
		SearchCriterion criteria = visitor.getCriteria();
		
		String[] colNames = GridUtil.getNames(table.getColumns()).toArray(new String[table.getColumns().size()]);
		AssociationKey tableKey = new AssociationKey(table.getCanonicalName(), colNames, colNames);
		
		this.tableData = this.dialect.getAssociation(tableKey, null);
		if (this.tableData != null) {
			if (criteria != null) {
				this.rowIter = criteria.filter(this.tableData);
			}
			else {
				Set<RowKey> rows = this.tableData.getKeys();
				this.rowIter = rows.iterator();
			}
		}
	}

	@Override
	public List<?> next() throws TranslatorException, DataNotAvailableException {
		if (this.tableData != null && this.rowIter.hasNext()) {
			Tuple tuple = tableData.get(this.rowIter.next());
			if (tuple != null) {
				ArrayList<Object> row = new ArrayList<Object>();
				for (String column:this.projectedColumns) {
					row.add(tuple.get(column));
				}
				return row;
			}
		}
		return null;
	}
	
	@Override
	public void close() {
	}

	@Override
	public void cancel() throws TranslatorException {
	}	
}
