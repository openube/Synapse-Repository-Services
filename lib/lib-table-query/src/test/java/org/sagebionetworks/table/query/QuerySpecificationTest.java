package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.Factor;
import org.sagebionetworks.table.query.model.MysqlFunction;
import org.sagebionetworks.table.query.model.NumericPrimary;
import org.sagebionetworks.table.query.model.NumericValueExpression;
import org.sagebionetworks.table.query.model.NumericValueFunction;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.SetQuantifier;
import org.sagebionetworks.table.query.model.SqlDirective;
import org.sagebionetworks.table.query.model.TableExpression;
import org.sagebionetworks.table.query.model.Term;
import org.sagebionetworks.table.query.model.ValueExpression;
import org.sagebionetworks.table.query.util.SqlElementUntils;

import com.google.common.collect.Lists;

public class QuerySpecificationTest {
	
	@Test
	public void testQuerySpecificationToSQL() throws ParseException{
		SetQuantifier setQuantifier = null;
		SelectList selectList = SqlElementUntils.createSelectList("one, two");
		TableExpression tableExpression = SqlElementUntils.createTableExpression("from syn123");
		QuerySpecification element = new QuerySpecification(null, setQuantifier, selectList, tableExpression);
		assertEquals("SELECT one, two FROM syn123", element.toString());
	}
	
	@Test
	public void testQuerySpecificationToSQLWithDirective() throws ParseException {
		SqlDirective sqlDirective = SqlDirective.SQL_CALC_FOUND_ROWS;
		SetQuantifier setQuantifier = null;
		SelectList selectList = SqlElementUntils.createSelectList("one, two");
		TableExpression tableExpression = SqlElementUntils.createTableExpression("from syn123");
		QuerySpecification element = new QuerySpecification(sqlDirective, setQuantifier, selectList, tableExpression);
		assertEquals("SELECT SQL_CALC_FOUND_ROWS one, two FROM syn123", element.toString());
	}

	@Test
	public void testGetRowCount() throws ParseException {
		MysqlFunction foundRows = MysqlFunction.FOUND_ROWS;
		NumericValueFunction numericValueFunction = new NumericValueFunction(foundRows);
		NumericPrimary numericPrimary = new NumericPrimary(numericValueFunction);
		Factor factor = new Factor(numericPrimary);
		Term term = new Term(factor);
		NumericValueExpression numericValueExpression = new NumericValueExpression(term);
		ValueExpression valueExpression = new ValueExpression(numericValueExpression);
		DerivedColumn column = new DerivedColumn(valueExpression, null);
		List<DerivedColumn> columns = Lists.newArrayList(column);
		SelectList selectList = new SelectList(columns);
		QuerySpecification element = new QuerySpecification(null, null, selectList, null);
		assertEquals("SELECT FOUND_ROWS()", element.toString());
		assertNull(element.getTableName());
	}
	
	@Test
	public void testSQL_CALC_FOUND_ROWS() throws ParseException{
		QuerySpecification element = new TableQueryParser("SELECT SQL_CALC_FOUND_ROWS count(*) from syn123").querySpecification();
		assertEquals(SqlDirective.SQL_CALC_FOUND_ROWS, element.getSqlDirective());
		assertEquals("SELECT SQL_CALC_FOUND_ROWS COUNT(*) FROM syn123", element.toSql());
	}
	
	@Test
	public void testFoundRows() throws ParseException{
		QuerySpecification element = new TableQueryParser("SELECT FOUND_ROWS()").querySpecification();
		assertEquals("SELECT FOUND_ROWS()", element.toSql());
	}

	@Test
	public void testQuerySpecificationToSQLWithSetQuantifier() throws ParseException{
		SetQuantifier setQuantifier = SetQuantifier.DISTINCT;
		SelectList selectList = SqlElementUntils.createSelectList("one, two");
		TableExpression tableExpression = SqlElementUntils.createTableExpression("from syn123");
		QuerySpecification element = new QuerySpecification(null, setQuantifier, selectList, tableExpression);
		assertEquals("SELECT DISTINCT one, two FROM syn123", element.toString());
	}

	@Test
	public void testQuerySpecificationToSQLWithNumericFunction() throws ParseException {
		SelectList selectList = SqlElementUntils.createSelectList("FOUND_ROWS()");
		TableExpression tableExpression = SqlElementUntils.createTableExpression("from syn123");
		QuerySpecification element = new QuerySpecification(null, null, selectList, tableExpression);
		assertEquals("SELECT FOUND_ROWS() FROM syn123", element.toString());
		assertEquals("syn123", element.getTableName());
	}
	
	@Test
	public void testIsAggregateNoDistinct() throws ParseException{
		QuerySpecification element = new TableQueryParser("select * from syn123").querySpecification();
		assertFalse(element.isElementAggregate());
	}
	
	@Test
	public void testIsAggregateDistinct() throws ParseException{
		QuerySpecification element = new TableQueryParser("select distinct three, four from syn123").querySpecification();
		assertTrue(element.isElementAggregate());
	}
	
	@Test
	public void testGetTableName() throws ParseException{
		QuerySpecification element = new TableQueryParser("select distinct three, four from syn123").querySpecification();
		assertEquals("syn123", element.getTableName());
	}
	
}
