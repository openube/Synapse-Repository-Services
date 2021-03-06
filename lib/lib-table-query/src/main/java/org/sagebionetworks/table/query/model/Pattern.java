package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * This matches &ltpattern&gt  in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class Pattern extends SQLElement {
	
	CharacterValueExpression characterValueExpression;

	public Pattern(CharacterValueExpression characterValueExpression) {
		super();
		this.characterValueExpression = characterValueExpression;
	}

	public CharacterValueExpression getCharacterValueExpression() {
		return characterValueExpression;
	}
	
	@Override
	public void toSql(StringBuilder builder) {
		characterValueExpression.toSql(builder);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, characterValueExpression);
	}
}
